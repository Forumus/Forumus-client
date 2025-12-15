package com.hcmus.forumus_client.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.model.VoteState
import kotlinx.coroutines.tasks.await
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import android.media.MediaMetadataRetriever
import java.util.UUID
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import android.graphics.Bitmap
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import androidx.core.net.toUri
import com.google.firebase.Timestamp

/**
 * Repository for managing post data operations with Firestore.
 * Handles CRUD operations, voting, and post enrichment with user-specific data.
 */
class PostRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userRepository: UserRepository = UserRepository()
) {

    /**
     * Enriches a post with user-specific data including vote state, vote counts, and comment count.
     *
     * @param userId The current user's ID (nullable)
     * @return Enriched post with calculated values
     */
    private suspend fun Post.enrichForUser(userId: String?): Post {
        // Calculate user's vote state from votedUsers map
        this.userVote = userId?.let { votedUsers[it] } ?: VoteState.NONE

        // Recalculate upvote and downvote counts from votedUsers map
        val votes = votedUsers.values
        this.upvoteCount = votes.count { it == VoteState.UPVOTE }
        this.downvoteCount = votes.count { it == VoteState.DOWNVOTE }

        // Fetch and count comments for this post
        val commentsSnapshot = firestore.collection("posts")
            .document(this.id)
            .collection("comments")
            .get()
            .await()

        this.commentCount = commentsSnapshot.size()

        return this
    }

    fun generatePostId(): String {
        // Lấy thời gian hiện tại
        val currentDate = Calendar.getInstance()

        // Định dạng thời gian theo yêu cầu (yyyyMMdd_HHmmss)
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val formattedDate = dateFormat.format(currentDate.time)

        // Tạo số ngẫu nhiên từ 1000 đến 9999
        val randomPart = (1000..9999).random()

        // Kết hợp lại thành ID với định dạng "POST_yyyyMMdd_HHmmss_random"
        return "POST" + "_" + "$formattedDate" + "_" + "$randomPart"
    }

    suspend fun savePost(context: Context, post: Post): Result<Boolean> {
        return try {
            val storage = FirebaseStorage.getInstance().reference
            val imageUrls = mutableListOf<String>()
            val videoUrls = mutableListOf<String>()
            val videoThumbnailUrls = mutableListOf<String?>()

            // 1. Upload ảnh
            post.imageUrls.forEach { imageUri ->
                val imageRef = storage.child("post_images/${UUID.randomUUID()}.jpg")
                val imageData = Uri.parse(imageUri)

                try {
                    // Upload ảnh
                    val imageUrl = uploadFile(imageRef, imageData)
                    imageUrls.add(imageUrl)
                } catch (e: Exception) {
                    Log.e("savePost", "Error uploading image: ${e.message}")
                }
            }

            // 2. Upload video
            post.videoUrls.forEach { videoUri ->
                val videoRef = storage.child("post_videos/${UUID.randomUUID()}.mp4")
                val videoData = Uri.parse(videoUri)

                try {
                    // Upload video
                    val videoUrl = uploadFile(videoRef, videoData)
                    videoUrls.add(videoUrl)

                    // 3. Tạo thumbnail cho video
                    val videoThumbnailUri = getVideoThumbnailUri(context, videoUri)
                    if (videoThumbnailUri != null) {
                        val thumbRef = storage.child("post_thumbnails/${UUID.randomUUID()}.jpg")
                        val thumbUrl = uploadFile(thumbRef, videoThumbnailUri)
                        videoThumbnailUrls.add(thumbUrl)
                    } else {
                        videoThumbnailUrls.add(null)
                    }
                } catch (e: Exception) {
                    Log.e("savePost", "Error uploading video: ${e.message}")
                }
            }

            val userId = auth.currentUser!!.uid
            val user = userRepository.getUserById(userId)

            val generatedId = generatePostId()

            val postRef = FirebaseFirestore
                .getInstance()
                .collection("posts")
                .document(generatedId)

            val now = Timestamp.now()

            val updatedPost = post.copy(
                id = generatedId,
                authorId = user.uid,
                authorName = user.fullName,
                authorAvatarUrl = user.profilePictureUrl,
                createdAt = now,
                imageUrls = imageUrls,
                videoUrls = videoUrls,
                videoThumbnailUrls = videoThumbnailUrls
            )

            postRef.set(updatedPost).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun uploadFile(ref: StorageReference, uri: Uri): String {
        return try {
            val uploadTask = ref.putFile(uri)
            val downloadUrlTask = uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                ref.downloadUrl
            }.await()

            downloadUrlTask.toString()
        } catch (e: Exception) {
            throw e
        }
    }

    fun getVideoThumbnailUri(context: Context, videoUri: String): Uri? {
        val retriever = MediaMetadataRetriever()
        return try {
            val uri = videoUri.toUri()
            retriever.setDataSource(context, uri)

            val thumbnailBitmap = retriever.getFrameAtTime(
                0,
                MediaMetadataRetriever.OPTION_CLOSEST
            ) ?: return null

            // Tạo file tạm trong cache của app
            val tempFile = File.createTempFile("video_thumb_", ".jpg", context.cacheDir)
            FileOutputStream(tempFile).use { out ->
                thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }

            Uri.fromFile(tempFile)
        } catch (e: Exception) {
            Log.e("getVideoThumbnailUri", "Error creating thumbnail", e)
            null
        } finally {
            retriever.release()
        }
    }

    /**
     * Updates an existing post in Firestore.
     *
     * @param post The post object to update
     * @return The updated post
     * @throws IllegalArgumentException if post ID is blank
     */
    suspend fun updatePost(post: Post): Post {
        if (post.id.isBlank()) {
            throw IllegalArgumentException("Post id is blank, cannot update")
        }

        val postRef = firestore.collection("posts").document(post.id)
        postRef.set(post).await()

        return post
    }

    /**
     * Retrieves a limited number of recent posts, ordered by creation date descending.
     *
     * @param limit Maximum number of posts to retrieve
     * @return List of enriched posts with user-specific data
     */
    suspend fun getPosts(limit: Long = 50): List<Post> {
        val userId = auth.currentUser?.uid
        return firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
            .toObjects(Post::class.java)
            .map { it.enrichForUser(userId) }
    }

    /**
     * Retrieves all posts from Firestore, ordered by creation date descending.
     *
     * @return List of enriched posts with user-specific data
     */
    suspend fun getAllPosts(): List<Post> {
        val userId = auth.currentUser?.uid
        return firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
            .toObjects(Post::class.java)
            .map { it.enrichForUser(userId) }
    }

    /**
     * Retrieves all posts authored by a specific user, ordered by creation date descending.
     *
     * @param userId The ID of the author
     * @param limit Maximum number of posts to retrieve
     * @return List of enriched posts with user-specific data
     */
    suspend fun getPostsByUser(
        userId: String,
        limit: Long = 100
    ): List<Post> {
        val currentUser = auth.currentUser?.uid

        return firestore.collection("posts")
            .whereEqualTo("authorId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
            .toObjects(Post::class.java)
            .map { it.enrichForUser(currentUser) }
    }

    /**
     * Retrieves a single post by its ID.
     *
     * @param postId The ID of the post to retrieve
     * @return Enriched post with user-specific data, or null if not found
     */
    suspend fun getPostById(postId: String): Post? {
        val userId = auth.currentUser?.uid
        return firestore.collection("posts")
            .document(postId)
            .get()
            .await()
            .toObject(Post::class.java)
            ?.enrichForUser(userId)
    }

    /**
     * Toggles upvote for a post by the current user.
     * If already upvoted, removes the vote.
     * If downvoted, changes to upvote.
     * If no vote, creates new upvote.
     *
     * @param post The post to upvote
     * @return Updated post with new vote state
     * @throws IllegalStateException if user is not logged in
     */
    suspend fun toggleUpvote(post: Post): Post {
        val userId = auth.currentUser?.uid
            ?: throw IllegalStateException("User not logged in")

        val currentVote = post.votedUsers[userId]
        var upvoteChange = 0
        var downvoteChange = 0

        when (currentVote) {
            VoteState.UPVOTE -> {
                // Currently upvoted, remove the vote
                post.votedUsers.remove(userId)
                upvoteChange = -1
            }
            VoteState.DOWNVOTE -> {
                // Currently downvoted, switch to upvote
                post.votedUsers[userId] = VoteState.UPVOTE
                upvoteChange = 1
                downvoteChange = -1
            }
            else -> {
                // No vote or NONE state, create new upvote
                post.votedUsers[userId] = VoteState.UPVOTE
                upvoteChange = 1
            }
        }

        post.upvoteCount += upvoteChange
        post.downvoteCount += downvoteChange
        post.userVote = post.votedUsers[userId] ?: VoteState.NONE

        // Persist changes to Firestore
        updatePost(post)

        return post.copy()
    }

    /**
     * Toggles downvote for a post by the current user.
     * If already downvoted, removes the vote.
     * If upvoted, changes to downvote.
     * If no vote, creates new downvote.
     *
     * @param post The post to downvote
     * @return Updated post with new vote state
     * @throws IllegalStateException if user is not logged in
     */
    suspend fun toggleDownvote(post: Post): Post {
        val userId = auth.currentUser?.uid
            ?: throw IllegalStateException("User not logged in")

        val currentVote = post.votedUsers[userId]
        var upvoteChange = 0
        var downvoteChange = 0

        when (currentVote) {
            VoteState.DOWNVOTE -> {
                // Currently downvoted, remove the vote
                post.votedUsers.remove(userId)
                downvoteChange = -1
            }
            VoteState.UPVOTE -> {
                // Currently upvoted, switch to downvote
                post.votedUsers[userId] = VoteState.DOWNVOTE
                downvoteChange = 1
                upvoteChange = -1
            }
            else -> {
                // No vote or NONE state, create new downvote
                post.votedUsers[userId] = VoteState.DOWNVOTE
                downvoteChange = 1
            }
        }

        post.upvoteCount += upvoteChange
        post.downvoteCount += downvoteChange
        post.userVote = post.votedUsers[userId] ?: VoteState.NONE

        // Persist changes to Firestore
        updatePost(post)

        return post.copy()
    }
}