package com.hcmus.forumus_client.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.model.VoteState
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PostRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    // --- HÀM SAVE POST (Tương thích Post.kt gốc) ---
    suspend fun savePost(post: Post): Result<Boolean> {
        return try {
            // 1. Xử lý Upload ẢNH
            val uploadedImageUrls = mutableListOf<String>()
            for (uriString in post.imageUrls) {
                if (uriString.startsWith("http")) {
                    uploadedImageUrls.add(uriString)
                } else {
                    val uri = Uri.parse(uriString)
                    val fileName = "${UUID.randomUUID()}"
                    // Lưu vào folder post_images
                    val ref = storage.reference.child("post_images/$fileName")
                    ref.putFile(uri).await()
                    val downloadUrl = ref.downloadUrl.await().toString()
                    uploadedImageUrls.add(downloadUrl)
                }
            }
            post.imageUrls = uploadedImageUrls

            // 2. Xử lý Upload VIDEO
            val uploadedVideoUrls = mutableListOf<String>()
            for (uriString in post.videoUrls) {
                if (uriString.startsWith("http")) {
                    uploadedVideoUrls.add(uriString)
                } else {
                    val uri = Uri.parse(uriString)
                    val fileName = "${UUID.randomUUID()}"
                    // Lưu vào folder post_videos
                    val ref = storage.reference.child("post_videos/$fileName")
                    ref.putFile(uri).await()
                    val downloadUrl = ref.downloadUrl.await().toString()
                    uploadedVideoUrls.add(downloadUrl)
                }
            }
            post.videoUrls = uploadedVideoUrls

            // 3. Lưu xuống Firestore (Firebase tự map topicIds thành "topics" nhờ @PropertyName)
            firestore.collection("posts").document(post.id).set(post).await()

            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // --- CÁC HÀM GET/UPDATE GIỮ NGUYÊN ---
    private suspend fun Post.enrichForUser(userId: String?): Post {
        this.userVote = userId?.let { votedUsers[it] } ?: VoteState.NONE
        val votes = votedUsers.values
        this.upvoteCount = votes.count { it == VoteState.UPVOTE }
        this.downvoteCount = votes.count { it == VoteState.DOWNVOTE }

        try {
            val commentsSnapshot = firestore.collection("posts")
                .document(this.id)
                .collection("comments").get().await()
            this.commentCount = commentsSnapshot.size()
        } catch (e: Exception) {
            this.commentCount = 0
        }
        return this
    }

    suspend fun updatePost(post: Post): Post {
        if (post.id.isBlank()) throw IllegalArgumentException("Post id is blank")
        firestore.collection("posts").document(post.id).set(post).await()
        return post
    }

    suspend fun getPosts(limit: Long = 50): List<Post> {
        val userId = auth.currentUser?.uid
        return firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit).get().await()
            .toObjects(Post::class.java).map { it.enrichForUser(userId) }
    }

    suspend fun getAllPosts(): List<Post> {
        val userId = auth.currentUser?.uid
        return firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await()
            .toObjects(Post::class.java).map { it.enrichForUser(userId) }
    }

    suspend fun getPostsByUser(userId: String, limit: Long = 100): List<Post> {
        val currentUser = auth.currentUser?.uid
        return firestore.collection("posts")
            .whereEqualTo("authorId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit).get().await()
            .toObjects(Post::class.java).map { it.enrichForUser(currentUser) }
    }

    suspend fun getPostById(postId: String): Post? {
        val userId = auth.currentUser?.uid
        return firestore.collection("posts").document(postId).get().await()
            .toObject(Post::class.java)?.enrichForUser(userId)
    }

    suspend fun toggleUpvote(post: Post): Post {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
        val currentVote = post.votedUsers[userId]
        when (currentVote) {
            VoteState.UPVOTE -> post.votedUsers.remove(userId)
            VoteState.DOWNVOTE -> post.votedUsers[userId] = VoteState.UPVOTE
            else -> post.votedUsers[userId] = VoteState.UPVOTE
        }
        updatePost(post)
        return post.copy().enrichForUser(userId)
    }

    suspend fun toggleDownvote(post: Post): Post {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
        val currentVote = post.votedUsers[userId]
        when (currentVote) {
            VoteState.DOWNVOTE -> post.votedUsers.remove(userId)
            VoteState.UPVOTE -> post.votedUsers[userId] = VoteState.DOWNVOTE
            else -> post.votedUsers[userId] = VoteState.DOWNVOTE
        }
        updatePost(post)
        return post.copy().enrichForUser(userId)
    }
}