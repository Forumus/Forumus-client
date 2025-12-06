package com.hcmus.forumus_client.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.model.VoteState
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing post data operations with Firestore.
 * Handles CRUD operations, voting, and post enrichment with user-specific data.
 */
class PostRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
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
        this.upvoteCount = votes.count { it == VoteState.UP }
        this.downvoteCount = votes.count { it == VoteState.DOWN }

        // Fetch and count comments for this post
        val commentsSnapshot = firestore.collection("posts")
            .document(this.id)
            .collection("comments")
            .get()
            .await()

        this.commentCount = commentsSnapshot.size()

        return this
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
            VoteState.UP -> {
                // Currently upvoted, remove the vote
                post.votedUsers.remove(userId)
                upvoteChange = -1
            }
            VoteState.DOWN -> {
                // Currently downvoted, switch to upvote
                post.votedUsers[userId] = VoteState.UP
                upvoteChange = 1
                downvoteChange = -1
            }
            else -> {
                // No vote or NONE state, create new upvote
                post.votedUsers[userId] = VoteState.UP
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
            VoteState.DOWN -> {
                // Currently downvoted, remove the vote
                post.votedUsers.remove(userId)
                downvoteChange = -1
            }
            VoteState.UP -> {
                // Currently upvoted, switch to downvote
                post.votedUsers[userId] = VoteState.DOWN
                downvoteChange = 1
                upvoteChange = -1
            }
            else -> {
                // No vote or NONE state, create new downvote
                post.votedUsers[userId] = VoteState.DOWN
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