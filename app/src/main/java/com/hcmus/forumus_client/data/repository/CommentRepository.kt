package com.hcmus.forumus_client.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hcmus.forumus_client.data.model.Comment
import com.hcmus.forumus_client.data.model.VoteState
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing comment data operations with Firestore.
 * Handles CRUD operations, voting, and comment hierarchy.
 */
class CommentRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    /**
     * Enriches a comment with the current user's vote state and vote counts.
     * Calculates userVote and updates upvote/downvote counts from votedUsers map.
     */
    private fun Comment.withUserVoteAndVoteCounts(userId: String?): Comment {
        // Set user's vote state for this comment
        this.userVote = userId?.let { votedUsers[it] } ?: VoteState.NONE

        // Recalculate upvote and downvote counts from votedUsers map
        val votes = votedUsers.values
        this.upvoteCount = votes.count { it == VoteState.UP }
        this.downvoteCount = votes.count { it == VoteState.DOWN }

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

    /**
     * Creates and saves a new comment to Firestore.
     * Generates a unique ID if not provided and sets creation timestamp.
     *
     * @param comment The comment to add
     * @return The created comment with generated ID and timestamp
     * @throws IllegalArgumentException if postId is blank
     */
    suspend fun addComment(comment: Comment): Comment {
        if (comment.postId.isBlank()) {
            throw IllegalArgumentException("addComment: postId is blank")
        }

        // Generate unique ID if not provided
        val finalId = comment.id.ifBlank {
            firestore.collection("posts")
                .document(comment.postId)
                .collection("comments")
                .document()
                .id
        }

        val now = Timestamp.now()

        val preparedComment = comment.copy(
            id = finalId,
            createdAt = comment.createdAt ?: now,
        )

        firestore.collection("posts")
            .document(preparedComment.postId)
            .collection("comments")
            .document(preparedComment.id)
            .set(preparedComment)
            .await()

        return preparedComment
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
            VoteState.UP -> {
                // Currently upvoted, remove the vote
                comment.votedUsers.remove(userId)
                upvoteChange = -1
            }
            VoteState.DOWN -> {
                // Currently downvoted, switch to upvote
                comment.votedUsers[userId] = VoteState.UP
                upvoteChange = 1
                downvoteChange = -1
            }
            else -> {
                // No vote or NONE state, create new upvote
                comment.votedUsers[userId] = VoteState.UP
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
            VoteState.DOWN -> {
                // Currently downvoted, remove the vote
                comment.votedUsers.remove(userId)
                downvoteChange = -1
            }
            VoteState.UP -> {
                // Currently upvoted, switch to downvote
                comment.votedUsers[userId] = VoteState.DOWN
                downvoteChange = 1
                upvoteChange = -1
            }
            else -> {
                // No vote or NONE state, create new downvote
                comment.votedUsers[userId] = VoteState.DOWN
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
