package com.hcmus.forumus_client.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hcmus.forumus_client.data.model.Comment
import com.hcmus.forumus_client.data.model.VoteState
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Repository for managing comment data operations with Firestore.
 * Handles CRUD operations, voting, and comment hierarchy.
 */
class CommentRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userRepository: UserRepository = UserRepository()
) {
    companion object {
        private const val BATCH_LIMIT = 450
    }

    suspend fun updateAuthorInfoInComments(
        userId: String,
        newName: String,
        newAvatarUrl: String?
    ) {
        var lastDoc: com.google.firebase.firestore.DocumentSnapshot? = null

        while (true) {
            var query = firestore.collectionGroup("comments")
                .whereEqualTo("authorId", userId)
                .orderBy("createdAt")
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

    /**
     * Cập nhật luôn tên ở "replyToUserName"
     * cho những comment reply tới user này.
     */
    suspend fun updateReplyToUserName(
        userId: String,
        newName: String
    ) {
        var lastDoc: com.google.firebase.firestore.DocumentSnapshot? = null

        while (true) {
            var query = firestore.collectionGroup("comments")
                .whereEqualTo("replyToUserId", userId)
                .orderBy("createdAt")
                .limit(BATCH_LIMIT.toLong())

            if (lastDoc != null) query = query.startAfter(lastDoc)

            val snap = query.get().await()
            if (snap.isEmpty) break

            val batch = firestore.batch()
            for (doc in snap.documents) {
                batch.update(doc.reference, "replyToUserName", newName)
            }
            batch.commit().await()

            lastDoc = snap.documents.last()
            if (snap.size() < BATCH_LIMIT) break
        }
    }

    /**
     * Enriches a comment with the current user's vote state and vote counts.
     * Calculates userVote and updates upvote/downvote counts from votedUsers map.
     */
    private fun Comment.withUserVoteAndVoteCounts(userId: String?): Comment {
        // Set user's vote state for this comment
        this.userVote = userId?.let { votedUsers[it] } ?: VoteState.NONE

        // Recalculate upvote and downvote counts from votedUsers map
        val votes = votedUsers.values
        this.upvoteCount = votes.count { it == VoteState.UPVOTE }
        this.downvoteCount = votes.count { it == VoteState.DOWNVOTE }

        return this
    }

    /**
     * Updates an existing comment in Firestore.
     *
     * @param comment The comment object to update
     * @return The updated comment
     * @throws IllegalArgumentException if postId or commentId is blank
     */
    suspend fun updateComment(comment: Comment): Comment {
        if (comment.postId.isBlank() || comment.id.isBlank()) {
            throw IllegalArgumentException("updateComment: postId or commentId is blank")
        }

        val commentRef = firestore.collection("posts")
            .document(comment.postId)
            .collection("comments")
            .document(comment.id)

        commentRef.set(comment).await()
        return comment
    }

    /**
     * Retrieves all comments for a specific post, ordered by creation date.
     * Enriches each comment with reply counts, user vote state, and vote counts.
     *
     * @param postId The ID of the post
     * @param limit Maximum number of comments to retrieve
     * @return List of enriched comments
     */
    suspend fun getCommentsByPost(
        postId: String,
        limit: Long = 100
    ): List<Comment> {
        val currentUser = auth.currentUser?.uid

        val comments = firestore.collection("posts")
            .document(postId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(limit)
            .get()
            .await()
            .toObjects(Comment::class.java)
            .onEach { it.postId = postId }

        // Calculate reply count for each comment based on parent-child relationships
        applyCommentCounts(comments)

        // Enrich with user vote state and vote counts
        return comments.map { it.withUserVoteAndVoteCounts(currentUser) }
    }

    /**
     * Retrieves all comments authored by a specific user across all posts.
     * Uses collectionGroup query which requires a Firestore index to be set up.
     *
     * @param userId The ID of the author
     * @param limit Maximum number of comments to retrieve
     * @return List of enriched comments ordered by creation date descending
     */
    suspend fun getCommentsByUser(
        userId: String,
        limit: Long = 100
    ): List<Comment> {
        val currentUser = auth.currentUser?.uid

        val comments = firestore.collectionGroup("comments")
            .whereEqualTo("authorId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
            .toObjects(Comment::class.java)

        // Calculate reply count for each comment
        applyCommentCounts(comments)

        // Enrich with user vote state and vote counts
        return comments.map { it.withUserVoteAndVoteCounts(currentUser) }
    }

    /**
     * Calculates the reply count for each comment using depth-first search.
     * Takes into account all nested replies (multi-level comment threads).
     *
     * @param comments List of all comments in a post
     * @return The input list with updated commentCount for each comment
     */
    fun applyCommentCounts(comments: List<Comment>): List<Comment> {
        // Group comments by their parent comment ID to build parent-child relationships
        val childrenMap: Map<String, List<Comment>> =
            comments
                .filter { it.parentCommentId != null }
                .groupBy { it.parentCommentId!! }

        // Recursive function to count all nested replies for a given comment
        fun countAllReplies(commentId: String): Int {
            val children = childrenMap[commentId].orEmpty()
            if (children.isEmpty()) return 0

            var total = children.size
            for (child in children) {
                total += countAllReplies(child.id)
            }
            return total
        }

        // Assign reply count to each comment
        comments.forEach { comment ->
            comment.commentCount = countAllReplies(comment.id)
        }

        return comments
    }

    fun generateCommentId(): String {
        // Lấy thời gian hiện tại
        val currentDate = Calendar.getInstance()

        // Định dạng thời gian theo yêu cầu (yyyyMMdd_HHmmss)
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val formattedDate = dateFormat.format(currentDate.time)

        // Tạo số ngẫu nhiên từ 1000 đến 9999
        val randomPart = (1000..9999).random()

        // Kết hợp lại thành ID với định dạng "POST_yyyyMMdd_HHmmss_random"
        return "COMMENT" + "_" + "$formattedDate" + "_" + "$randomPart"
    }

    /**
     * Creates and saves a new comment to Firestore.
     * Generates a unique ID if not provided and sets creation timestamp.
     *
     * @param comment The comment to add
     * @return The created comment with generated ID and timestamp
     * @throws IllegalArgumentException if postId is blank
     */
    suspend fun saveComment(comment: Comment): Comment {
        val userId = auth.currentUser!!.uid
        val user = userRepository.getUserById(userId)

        val generatedId = generateCommentId()

        val commentRef = firestore.collection("posts")
            .document(comment.postId)
            .collection("comments")
            .document(generatedId)

        val now = Timestamp.now()

        val updatedComment = comment.copy(
            id = generatedId,
            authorId = user.uid,
            authorName = user.fullName,
            authorRole = user.role,
            authorAvatarUrl = user.profilePictureUrl ?: "",
            createdAt = now,
        )

        commentRef.set(updatedComment).await()

        return updatedComment
    }

    /**
     * Toggles upvote for a comment by the current user.
     * If already upvoted, removes the vote.
     * If downvoted, changes to upvote.
     * If no vote, creates new upvote.
     *
     * @param comment The comment to upvote
     * @return Updated comment with new vote state
     * @throws IllegalStateException if user is not logged in
     */
    suspend fun toggleUpvote(comment: Comment): Comment {
        val userId = auth.currentUser?.uid
            ?: throw IllegalStateException("User not logged in")

        val currentVote = comment.votedUsers[userId]
        var upvoteChange = 0
        var downvoteChange = 0

        when (currentVote) {
            VoteState.UPVOTE -> {
                // Currently upvoted, remove the vote
                comment.votedUsers.remove(userId)
                upvoteChange = -1
            }
            VoteState.DOWNVOTE -> {
                // Currently downvoted, switch to upvote
                comment.votedUsers[userId] = VoteState.UPVOTE
                upvoteChange = 1
                downvoteChange = -1
            }
            else -> {
                // No vote or NONE state, create new upvote
                comment.votedUsers[userId] = VoteState.UPVOTE
                upvoteChange = 1
            }
        }

        comment.upvoteCount += upvoteChange
        comment.downvoteCount += downvoteChange
        comment.userVote = comment.votedUsers[userId] ?: VoteState.NONE

        // Persist changes to Firestore
        updateComment(comment)

        return comment.copy()
    }

    /**
     * Toggles downvote for a comment by the current user.
     * If already downvoted, removes the vote.
     * If upvoted, changes to downvote.
     * If no vote, creates new downvote.
     *
     * @param comment The comment to downvote
     * @return Updated comment with new vote state
     * @throws IllegalStateException if user is not logged in
     */
    suspend fun toggleDownvote(comment: Comment): Comment {
        val userId = auth.currentUser?.uid
            ?: throw IllegalStateException("User not logged in")

        val currentVote = comment.votedUsers[userId]
        var upvoteChange = 0
        var downvoteChange = 0

        when (currentVote) {
            VoteState.DOWNVOTE -> {
                // Currently downvoted, remove the vote
                comment.votedUsers.remove(userId)
                downvoteChange = -1
            }
            VoteState.UPVOTE -> {
                // Currently upvoted, switch to downvote
                comment.votedUsers[userId] = VoteState.DOWNVOTE
                downvoteChange = 1
                upvoteChange = -1
            }
            else -> {
                // No vote or NONE state, create new downvote
                comment.votedUsers[userId] = VoteState.DOWNVOTE
                downvoteChange = 1
            }
        }

        comment.upvoteCount += upvoteChange
        comment.downvoteCount += downvoteChange
        comment.userVote = comment.votedUsers[userId] ?: VoteState.NONE

        // Persist changes to Firestore
        updateComment(comment)

        return comment.copy()
    }
}
