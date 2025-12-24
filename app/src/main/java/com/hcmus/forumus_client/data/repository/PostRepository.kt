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
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.model.Topic
import com.hcmus.forumus_client.data.model.VoteState
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.tasks.await


class PostRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userRepository: UserRepository = UserRepository()
) {

    // --- LOGIC LẤY TRENDING (Đã chuẩn) ---
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

    // --- LOGIC SEARCH (Giữ nguyên) ---
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

    // --- CÁC HÀM CŨ GIỮ NGUYÊN (enrich, generateID...) ---
    private suspend fun Post.enrichForUser(userId: String?): Post {
        this.userVote = userId?.let { votedUsers[it] } ?: VoteState.NONE
        val votes = votedUsers.values
        this.upvoteCount = votes.count { it == VoteState.UPVOTE }
        this.downvoteCount = votes.count { it == VoteState.DOWNVOTE }
        return this
    }

    fun generatePostId(): String {
        val currentDate = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val formattedDate = dateFormat.format(currentDate.time)
        val randomPart = (1000..9999).random()
        return "POST" + "_" + "$formattedDate" + "_" + "$randomPart"
    }

    // --- SỬA HÀM SAVE POST ĐỂ TĂNG TRENDING ---
    suspend fun savePost(context: Context, post: Post): Result<Boolean> {
        return try {
            val storage = FirebaseStorage.getInstance().reference
            val imageUrls = mutableListOf<String>()
            val videoUrls = mutableListOf<String>()
            val videoThumbnailUrls = mutableListOf<String?>()

            // 1. Upload Media (Giữ nguyên logic cũ của bạn)
            post.imageUrls.forEach { imageUri ->
                try {
                    val imageRef = storage.child("post_images/${UUID.randomUUID()}.jpg")
                    imageUrls.add(uploadFile(imageRef, imageUri.toUri()))
                } catch (e: Exception) { }
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
                } catch (e: Exception) { }
            }

            // 2. Lưu Post
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

            // --- MỚI: TĂNG POST COUNT CHO CÁC TOPIC ---
            // Duyệt qua danh sách topicIds của bài viết (VD: ["physics", "chemistry"])
            // Tìm trong collection "topics", nếu id trùng khớp thì tăng postCount lên 1
            updatedPost.topicIds.forEach { topicId ->
                // Lưu ý: topicId trong Post đang là dạng "analytical_chemistry"
                // Cần đảm bảo Document ID trong collection `topics` cũng là "analytical_chemistry"
                // Nếu Document ID của bạn là tự sinh (Auto ID) thì query theo field "name" hoặc sửa lại logic này.
                // Ở đây giả định Document ID = topic name (dạng snake_case) như cấu trúc bạn đã làm.

                try {
                    // Cách 1: Nếu Document ID chính là topicId (VD: analytical_chemistry)
                    firestore.collection("topics").document(topicId)
                        .update("postCount", FieldValue.increment(1))
                } catch (e: Exception) {
                    // Nếu lỗi (vd chưa có doc), có thể bỏ qua hoặc tạo mới
                }
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ... (Giữ nguyên uploadFile, getVideoThumbnailUri, updatePost...)
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

    // ... (Các hàm getPosts, getPostsPaginated, getPostsByUser, toggleVote GIỮ NGUYÊN từ tin nhắn trước)
    suspend fun getPosts(limit: Long = 50): List<Post> {
        val userId = auth.currentUser?.uid
        return firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
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

        if (lastDocument != null) {
            query = query.startAfter(lastDocument)
        }

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
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Not logged in")
        val currentVote = post.votedUsers[userId]
        if (currentVote == VoteState.UPVOTE) {
            post.votedUsers.remove(userId)
            post.upvoteCount -= 1
        } else {
            if (currentVote == VoteState.DOWNVOTE) post.downvoteCount -= 1
            post.votedUsers[userId] = VoteState.UPVOTE
            post.upvoteCount += 1
        }
        post.userVote = post.votedUsers[userId] ?: VoteState.NONE
        updatePost(post)
        return post
    }

    suspend fun toggleDownvote(post: Post): Post {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Not logged in")
        val currentVote = post.votedUsers[userId]
        if (currentVote == VoteState.DOWNVOTE) {
            post.votedUsers.remove(userId)
            post.downvoteCount -= 1
        } else {
            if (currentVote == VoteState.UPVOTE) post.upvoteCount -= 1
            post.votedUsers[userId] = VoteState.DOWNVOTE
            post.downvoteCount += 1
        }
        post.userVote = post.votedUsers[userId] ?: VoteState.NONE
        updatePost(post)
        return post
    }
}