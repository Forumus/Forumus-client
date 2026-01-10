package com.hcmus.forumus_client.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.hcmus.forumus_client.data.dto.GetSuggestedTopicsRequest
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.model.PostStatus
import com.hcmus.forumus_client.data.model.Topic
import com.hcmus.forumus_client.data.model.VoteState
import com.hcmus.forumus_client.data.remote.NetworkService
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PostRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userRepository: UserRepository = UserRepository()
) {

    suspend fun getTrendingTopics(): List<Topic> {
        return try {
            firestore.collection("topics")
                .orderBy("postCount", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .await()
                .toObjects(Topic::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun searchPostsCandidates(): List<Post> {
        return try {
            val userId = auth.currentUser?.uid
            firestore.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()
                .toObjects(Post::class.java)
                .map { it.enrichForUser(userId) }
        } catch (e: Exception) { emptyList() }
    }

    // --- SỬA QUAN TRỌNG 1: BỎ ĐOẠN LẤY COMMENT GÂY LAG ---
    private suspend fun Post.enrichForUser(userId: String?): Post {
        this.userVote = userId?.let { votedUsers[it] } ?: VoteState.NONE
        val votes = votedUsers.values
        this.upvoteCount = votes.count { it == VoteState.UPVOTE }
        this.downvoteCount = votes.count { it == VoteState.DOWNVOTE }

        // ĐÃ XÓA ĐOẠN LẤY COMMENT Ở ĐÂY ĐỂ SEARCH NHANH HƠN
        // (Nếu muốn hiện số comment, hãy cập nhật số đó vào Post khi user comment, đừng gọi API ở đây)

        return this
    }

    fun generatePostId(): String {
        val currentDate = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val formattedDate = dateFormat.format(currentDate.time)
        val randomPart = (1000..9999).random()
        return "POST" + "_" + "$formattedDate" + "_" + "$randomPart"
    }

    // --- SỬA QUAN TRỌNG 2: THÊM LOGIC TĂNG TRENDING ---
    suspend fun savePost(context: Context, post: Post): Result<Boolean> {
        return try {
            val storage = FirebaseStorage.getInstance().reference
            val imageUrls = mutableListOf<String>()
            val videoUrls = mutableListOf<String>()
            val videoThumbnailUrls = mutableListOf<String?>()

            post.imageUrls.forEach { imageUri ->
                try {
                    val imageRef = storage.child("post_images/${UUID.randomUUID()}.jpg")
                    imageUrls.add(uploadFile(imageRef, imageUri.toUri()))
                } catch (e: Exception) { Log.e("savePost", "Img error", e) }
            }

            post.videoUrls.forEach { videoUri ->
                try {
                    val videoRef = storage.child("post_videos/${UUID.randomUUID()}.mp4")
                    videoUrls.add(uploadFile(videoRef, videoUri.toUri()))
                    val thumbUri = getVideoThumbnailUri(context, videoUri)
                    if (thumbUri != null) {
                        val thumbRef = storage.child("post_thumbnails/${UUID.randomUUID()}.jpg")
                        videoThumbnailUrls.add(uploadFile(thumbRef, thumbUri))
                    } else { videoThumbnailUrls.add(null) }
                } catch (e: Exception) { Log.e("savePost", "Video error", e) }
            }

            val userId = auth.currentUser!!.uid
            val user = userRepository.getUserById(userId)
            val generatedId = generatePostId()
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

            firestore.collection("posts").document(generatedId).set(updatedPost).await()

            // --- ĐOẠN MỚI: TĂNG POST COUNT CHO CÁC TOPIC ---
            updatedPost.topicIds.forEach { topicId ->
                try {
                    firestore.collection("topics").document(topicId)
                        .update("postCount", FieldValue.increment(1))
                } catch (e: Exception) { }
            }

            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    private suspend fun uploadFile(ref: StorageReference, uri: Uri): String {
        val uploadTask = ref.putFile(uri)
        return uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) task.exception?.let { throw it }
            ref.downloadUrl
        }.await().toString()
    }

    fun getVideoThumbnailUri(context: Context, videoUri: String): Uri? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, videoUri.toUri())
            val bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST) ?: return null
            val tempFile = File.createTempFile("video_thumb_", ".jpg", context.cacheDir)
            FileOutputStream(tempFile).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out) }
            Uri.fromFile(tempFile)
        } catch (e: Exception) { null } finally { retriever.release() }
    }

    suspend fun updatePost(post: Post): Post {
        if (post.id.isBlank()) throw IllegalArgumentException("Post id blank")
        firestore.collection("posts").document(post.id).set(post).await()
        return post
    }

    suspend fun getPosts(limit: Long = 50): List<Post> {
        val userId = auth.currentUser?.uid
        return firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .whereEqualTo("status", PostStatus.APPROVED)
            .limit(limit)
            .get().await()
            .toObjects(Post::class.java)
            .map { it.enrichForUser(userId) }
    }

    suspend fun getPostsPaginated(
        limit: Long = 10,
        lastDocument: com.google.firebase.firestore.DocumentSnapshot? = null
    ): Pair<List<Post>, com.google.firebase.firestore.DocumentSnapshot?> {
        val userId = auth.currentUser?.uid
        var query = firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
        if (lastDocument != null) query = query.startAfter(lastDocument)

        val snapshot = query.get().await()
        val posts = snapshot.toObjects(Post::class.java).map { it.enrichForUser(userId) }
        val lastDoc = if (snapshot.documents.isNotEmpty()) snapshot.documents.last() else null
        return Pair(posts, lastDoc)
    }

    suspend fun getAllPosts(): List<Post> {
        val userId = auth.currentUser?.uid
        return firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await()
            .toObjects(Post::class.java)
            .map { it.enrichForUser(userId) }
    }

    suspend fun getPostsByUser(userId: String, limit: Long = 100): List<Post> {
        val currentUser = auth.currentUser?.uid
        return firestore.collection("posts")
            .whereEqualTo("authorId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get().await()
            .toObjects(Post::class.java)
            .map { it.enrichForUser(currentUser) }
    }

    suspend fun getPostById(postId: String): Post? {
        val userId = auth.currentUser?.uid
        return firestore.collection("posts").document(postId).get().await()
            .toObject(Post::class.java)?.enrichForUser(userId)
    }

    suspend fun toggleUpvote(post: Post): Post {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
        val currentVote = post.votedUsers[userId]
        var upvoteChange = 0
        var downvoteChange = 0

        when (currentVote) {
            VoteState.UPVOTE -> {
                post.votedUsers.remove(userId)
                upvoteChange = -1
            }
            VoteState.DOWNVOTE -> {
                post.votedUsers[userId] = VoteState.UPVOTE
                upvoteChange = 1
                downvoteChange = -1
            }
            else -> {
                post.votedUsers[userId] = VoteState.UPVOTE
                upvoteChange = 1
            }
        }
        post.upvoteCount += upvoteChange
        post.downvoteCount += downvoteChange
        post.userVote = post.votedUsers[userId] ?: VoteState.NONE
        updatePost(post)
        return post.copy()
    }

    suspend fun toggleDownvote(post: Post): Post {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
        val currentVote = post.votedUsers[userId]
        var upvoteChange = 0
        var downvoteChange = 0

        when (currentVote) {
            VoteState.DOWNVOTE -> {
                post.votedUsers.remove(userId)
                downvoteChange = -1
            }
            VoteState.UPVOTE -> {
                post.votedUsers[userId] = VoteState.DOWNVOTE
                downvoteChange = 1
                upvoteChange = -1
            }
            else -> {
                post.votedUsers[userId] = VoteState.DOWNVOTE
                downvoteChange = 1
            }
        }
        post.upvoteCount += upvoteChange
        post.downvoteCount += downvoteChange
        post.userVote = post.votedUsers[userId] ?: VoteState.NONE
        updatePost(post)
        return post.copy()
    }

    suspend fun getAllTopics(): List<Topic> {
        return try {
            firestore.collection("topics").get().await().toObjects(Topic::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getSuggestedTopics(title: String, content: String): List<Topic> {
        return try {
            val request = GetSuggestedTopicsRequest(title = title, content = content)
            val response = NetworkService.apiService.getSuggestedTopics(request)
            if (response.isSuccessful && response.body()?.success == true) response.body()!!.topics else emptyList()
        } catch (e: Exception) { emptyList() }
    }
}