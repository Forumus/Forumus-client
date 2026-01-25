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
import com.hcmus.forumus_client.data.dto.GetSuggestedTopicsRequest
import com.hcmus.forumus_client.data.model.PostStatus
import com.hcmus.forumus_client.data.model.Topic
import com.google.firebase.firestore.FieldValue
import com.hcmus.forumus_client.data.remote.NetworkService
import com.hcmus.forumus_client.data.cache.SummaryCacheManager

/**
 * Repository for managing post data operations with Firestore.
 * Handles CRUD operations, voting, and post enrichment with user-specific data.
 */
class PostRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userRepository: UserRepository = UserRepository(),
    private var summaryCacheManager: SummaryCacheManager? = null
) {
    companion object {
        private const val BATCH_LIMIT = 450 // chừa buffer, Firestore limit 500 ops/batch
        private const val TAG = "PostRepository"
    }

    /**
     * Initializes the summary cache manager. Must be called with application context.
     */
    fun initSummaryCache(context: Context) {
        if (summaryCacheManager == null) {
            summaryCacheManager = SummaryCacheManager.getInstance(context)
        }
    }

    suspend fun updateAuthorInfoInPosts(
        userId: String,
        newName: String,
        newAvatarUrl: String?
    ) {
        var lastDoc: com.google.firebase.firestore.DocumentSnapshot? = null

        while (true) {
            var query = firestore.collection("posts")
                .whereEqualTo("authorId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(BATCH_LIMIT.toLong())

            if (lastDoc != null) query = query.startAfter(lastDoc)

            val snap = query.get().await()
            if (snap.isEmpty) break

            val batch = firestore.batch()
            for (doc in snap.documents) {
                batch.update(
                    doc.reference,
                    mapOf(
                        "authorName" to newName,
                        "authorAvatarUrl" to (newAvatarUrl ?: "")
                    )
                )
            }
            batch.commit().await()

            lastDoc = snap.documents.last()
            if (snap.size() < BATCH_LIMIT) break
        }
    }

    suspend fun getTrendingTopics(): List<Topic> {
        return try {
            firestore.collection("topics")
                .orderBy("postCount", Query.Direction.DESCENDING) // Sắp xếp theo số lượng bài
                .limit(5)
                .get()
                .await()
                .toObjects(Topic::class.java)
        } catch (e: Exception) {
            emptyList()
        }
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
        } catch (e: Exception) {
            emptyList()
        }
    }

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
//        val commentsSnapshot = firestore.collection("posts")
//            .document(this.id)
//            .collection("comments")
//            .get()
//            .await()
//
//        this.commentCount = commentsSnapshot.size()

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

    suspend fun savePost(context: Context, post: Post): Result<String> {
        return try {
            val storage = FirebaseStorage.getInstance().reference
            val imageUrls = mutableListOf<String>()
            val videoUrls = mutableListOf<String>()
            val videoThumbnailUrls = mutableListOf<String?>()

            // 1. Upload ảnh
            post.imageUrls.forEach { imageUri ->
                val imageRef = storage.child("post_images/${UUID.randomUUID()}.jpg")
                val imageData = imageUri.toUri()

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
                val videoData = videoUri.toUri()

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
                authorRole = user.role,
                authorAvatarUrl = user.profilePictureUrl,
                createdAt = now,
                imageUrls = imageUrls,
                videoUrls = videoUrls,
                videoThumbnailUrls = videoThumbnailUrls,
                locationName = post.locationName,
                latitude = post.latitude,
                longitude = post.longitude
            )

            postRef.set(updatedPost).await()

            updatedPost.topicIds.forEach { topicId ->
                try {
                    firestore.collection("topics").document(topicId)
                        .update("postCount", FieldValue.increment(1))
                } catch (e: Exception) {
                    Log.e("savePost", "Error incrementing topic count", e)
                }
            }

            Result.success(generatePostId())
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
            .whereEqualTo("status", PostStatus.APPROVED) // Only fetch approved posts (we used it when the backend is ready)
            .limit(limit)
            .get()
            .await()
            .toObjects(Post::class.java)
            .map { it.enrichForUser(userId) }
    }

    /**
     * Retrieves posts with pagination support.
     *
     * @param limit Maximum number of posts to retrieve per page
     * @param lastDocument The last document from the previous page (null for first page)
     * @return Pair of enriched posts list and the last document snapshot for next page
     */
    suspend fun getPostsPaginated(
        limit: Long = 10,
        lastDocument: com.google.firebase.firestore.DocumentSnapshot? = null
    ): Pair<List<Post>, com.google.firebase.firestore.DocumentSnapshot?> {
        val userId = auth.currentUser?.uid

        var query =
            firestore
                .collection("posts")
                .whereEqualTo("status", PostStatus.APPROVED)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)

        // If we have a last document, start after it for pagination
        if (lastDocument != null) {
            query = query.startAfter(lastDocument)
        }

        val snapshot = query.get().await()
        val posts = snapshot.toObjects(Post::class.java).map { it.enrichForUser(userId) }

        // Get the last document for next pagination
        val lastDoc =
            if (snapshot.documents.isNotEmpty()) {
                snapshot.documents.last()
            } else {
                null
            }

        return Pair(posts, lastDoc)
    }

    /**
     * Retrieves all posts from Firestore, ordered by creation date descending.
     *
     * @return List of enriched posts with user-specific data
     */
    suspend fun getAllPosts(): List<Post> {
        val userId = auth.currentUser?.uid
        return firestore.collection("posts")
            .whereEqualTo("status", PostStatus.APPROVED)
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
            .whereEqualTo("status", PostStatus.APPROVED)
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
     * Retrieves multiple posts by their IDs.
     * Uses Firestore whereIn query which is limited to 10 items per query.
     * Chunks large ID lists into multiple queries if needed.
     *
     * @param postIds List of post IDs to retrieve
     * @return List of enriched posts found (may be less than input if some posts don't exist)
     */
    suspend fun getPostsByIds(postIds: List<String>): List<Post> {
        if (postIds.isEmpty()) return emptyList()

        return try {
            val userId = auth.currentUser?.uid
            // Firestore whereIn supports maximum 10 items per query
            val chunkedIds = postIds.chunked(10)
            val result = mutableListOf<Post>()

            for (chunk in chunkedIds) {
                val posts = firestore.collection("posts")
                    .whereIn("id", chunk)
                    .get()
                    .await()
                    .toObjects(Post::class.java)
                    .map { it.enrichForUser(userId) }

                result.addAll(posts)
            }

            result
        } catch (e: Exception) {
            Log.e("PostRepository", "Error fetching posts by IDs", e)
            emptyList()
        }
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

        // Trigger notification if upvoted and not self-vote
        if (post.userVote == VoteState.UPVOTE && post.authorId != userId) {
            try {
                val user = userRepository.getUserById(userId)
                
                // Backend notification trigger
                try {
                    val request = com.hcmus.forumus_client.data.remote.dto.NotificationTriggerRequest(
                        type = "UPVOTE",
                        actorId = userId,
                        actorName = user.fullName,
                        targetId = post.id,
                        targetUserId = post.authorId,
                        previewText = post.title
                    )
                    com.hcmus.forumus_client.data.remote.NetworkService.apiService.triggerNotification(request)
                } catch (e: Exception) {
                    Log.e("PostRepository", "Backend notification trigger failed", e)
                }
            } catch (e: Exception) {
                // Log error
                Log.e("PostRepository", "Error triggering notification", e)
            }
        }

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

    suspend fun getAllTopics(): List<Topic> {
        return try {
            val topicsSnapshot = firestore.collection("topics")
                .get()
                .await()

            topicsSnapshot.toObjects(Topic::class.java)
        } catch (e: Exception) {
            Log.e("PostRepository", "Error fetching topics: ${e.message}")
            emptyList()
        }
    }

    suspend fun getSuggestedTopics(title: String, content: String): List<Topic> {
        return try {
            val request = GetSuggestedTopicsRequest(
                title = title,
                content = content
            )

            val response = NetworkService.apiService.getSuggestedTopics(request)

            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null && responseBody.success) {
                    responseBody.topics
                } else {
                    emptyList()
                }
            } else {
                Log.e("PostRepository", "Failed to fetch suggested topics: ${response.code()} ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("PostRepository", "Error fetching suggested topics: ${e.message}")
            emptyList()
        }
    }

    suspend fun validatePost(postId: String): Result<com.hcmus.forumus_client.data.dto.PostValidationResponse> {
        return try {
            val request = com.hcmus.forumus_client.data.dto.PostIdRequest(postId)
            val response = NetworkService.apiService.validatePost(request)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Validation request failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generates an AI-powered summary for a post with intelligent caching.
     * 
     * Caching Strategy:
     * 1. Check local cache first (fastest response)
     * 2. If cache miss or expired, call server API
     * 3. Server may return cached response (avoiding AI call)
     * 4. Store new summaries in local cache with content hash
     * 5. Invalidate cache when content hash changes
     *
     * @param postId The ID of the post to summarize
     * @return Result containing the summary string on success, or an exception on failure
     */
    suspend fun getPostSummary(postId: String): Result<String> {
        return try {
            // Check local cache first
            val cache = summaryCacheManager
            if (cache != null) {
                val cachedEntry = cache.get(postId)
                if (cachedEntry != null) {
                    Log.d(TAG, "Local cache HIT for post $postId")
                    return Result.success(cachedEntry.summary)
                }
                Log.d(TAG, "Local cache MISS for post $postId - calling server")
            }
            
            // Call server API
            val request = com.hcmus.forumus_client.data.dto.PostSummaryRequest(postId)
            val response = NetworkService.apiService.summarizePost(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val body = response.body()!!
                val summary = body.summary
                
                if (summary != null) {
                    // Cache the response locally
                    val contentHash = body.contentHash
                    val generatedAt = body.generatedAt
                    val expiresAt = body.expiresAt
                    
                    if (cache != null && contentHash != null && generatedAt != null) {
                        cache.put(
                            postId = postId,
                            summary = summary,
                            contentHash = contentHash,
                            generatedAt = generatedAt,
                            expiresAt = expiresAt
                        )
                        Log.d(TAG, "Cached summary for post $postId (serverCached: ${body.cached})")
                        Log.d(TAG, cache.getCacheStatusSummary())
                    }
                    
                    Result.success(summary)
                } else {
                    Result.failure(Exception("Empty summary returned"))
                }
            } else {
                val errorMsg = response.body()?.errorMessage
                    ?: "Failed to generate summary: ${response.code()}"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting summary: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Invalidates the local cache for a specific post.
     * Call this when a post is updated.
     */
    fun invalidateSummaryCache(postId: String) {
        summaryCacheManager?.remove(postId)
    }

    /**
     * Clears all cached summaries.
     */
    fun clearSummaryCache() {
        summaryCacheManager?.clearAll()
    }

    /**
     * Gets cache statistics for debugging.
     */
    fun getSummaryCacheStats(): String {
        return summaryCacheManager?.getCacheStatusSummary() ?: "Cache not initialized"
    }
}