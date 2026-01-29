package com.hcmus.forumus_client.ui.post.detail

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.hcmus.forumus_client.data.model.Comment
import com.hcmus.forumus_client.data.model.FeedItem
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.model.Report
import com.hcmus.forumus_client.data.model.User
import com.hcmus.forumus_client.data.model.Violation
import com.hcmus.forumus_client.data.repository.CommentRepository
import com.hcmus.forumus_client.data.repository.PostRepository
import com.hcmus.forumus_client.data.repository.UserRepository
import com.hcmus.forumus_client.data.repository.ReportRepository
import com.hcmus.forumus_client.data.repository.SavePostResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.internal.platform.PlatformRegistry.applicationContext

/**
 * ViewModel for managing post detail view with nested comment threading.
 *
 * Responsibilities:
 * - Load post details and all associated comments from repositories
 * - Manage hierarchical comment structure with parent-child relationships
 * - Handle comment expansion/collapse for viewing nested replies
 * - Process voting actions on posts and comments
 * - Manage comment composition and submission (replies to post or comments)
 * - Calculate comment reply counts for hierarchical display
 * - Track current user and reply target for input bar management
 *
 * Architecture:
 * - Uses a single list (allComments) to store all comments and their relationships
 * - Maintains an expandedRootIds set to track which root comments are expanded
 * - Rebuilds items list whenever data or expansion state changes
 * - Uses FeedItem sealed class for polymorphic list rendering
 *
 * @param userRepository Repository for user operations
 * @param postRepository Repository for post operations
 * @param commentRepository Repository for comment operations
 */
class PostDetailViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val postRepository: PostRepository = PostRepository(),
    private val commentRepository: CommentRepository = CommentRepository(),
    private val reportRepository: ReportRepository = ReportRepository()
) : ViewModel() {

    /**
     * Initializes the summary cache with context. Call this from the Fragment/Activity.
     */
    fun initSummaryCache(context: Context) {
        postRepository.initSummaryCache(context.applicationContext)
    }

    // List of FeedItems (posts and comments) to display in RecyclerView
    private val _items = MutableLiveData<List<FeedItem>>(emptyList())
    val items: LiveData<List<FeedItem>> = _items

    // Current post being displayed
    private var currentPost: Post? = null

    // All comments for the post (stored flat, hierarchy managed by parent-child IDs)
    private var allComments: List<Comment> = emptyList()

    // Set of root comment IDs that are currently expanded to show replies
    private val expandedRootIds = mutableSetOf<String>()

    // LiveData to trigger opening reply input bar
    private val _openReplyInput = MutableLiveData<Boolean>(false)
    val openReplyInput: LiveData<Boolean> = _openReplyInput

    // LiveData for the comment/post being replied to (null if replying to post)
    private val _replyTargetComment = MutableLiveData<Comment?>()
    val replyTargetComment: LiveData<Comment?> = _replyTargetComment

    // Loading state indicator
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error message for UI display
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Save post result for UI feedback
    private val _savePostResult = MutableLiveData<SavePostResult?>()
    val savePostResult: LiveData<SavePostResult?> = _savePostResult

    // AI Summary state
    private val _summaryResult = MutableLiveData<Result<String>?>()
    val summaryResult: LiveData<Result<String>?> = _summaryResult

    private val _isSummaryLoading = MutableLiveData<Boolean>(false)
    val isSummaryLoading: LiveData<Boolean> = _isSummaryLoading

    private val _topics = MutableLiveData<List<com.hcmus.forumus_client.data.model.Topic>>(emptyList())
    val topics: LiveData<List<com.hcmus.forumus_client.data.model.Topic>> = _topics

    init {
        loadTopics()
    }

    private fun loadTopics() {
        viewModelScope.launch {
            try {
                val snapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("topics").get().await()
                val topicList = snapshot.toObjects(com.hcmus.forumus_client.data.model.Topic::class.java)
                _topics.value = topicList
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Loads the post detail and all associated comments from repositories.
     *
     * This is the main entry point for data loading. It:
     * 1. Fetches the post by ID
     * 2. Fetches all comments for that post
     * 3. Rebuilds the display items list
     *
     * @param postId The ID of the post to display
     */
    fun loadPostDetail(postId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Load the post
                val post = postRepository.getPostById(postId)
                    ?: throw IllegalStateException("Post not found")

                currentPost = post

                // Load all comments for this post
                val comments = commentRepository.getCommentsByPost(postId)
                allComments = comments

                // Rebuild the display items list
                rebuildItems()
            } catch (e: Exception) {
                _items.value = emptyList()
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Rebuilds the items list by organizing comments hierarchically and respecting expansion state.
     *
     * Algorithm:
     * 1. Find all root comments (parentCommentId == null)
     * 2. For each root, group all nested replies using the root's ID
     * 3. Build the result list: Post + Root comments + (if expanded) nested replies
     * 4. Respects the expandedRootIds set to show/hide nested replies
     *
     * This method is called whenever:
     * - Comments are loaded
     * - Expansion state changes (OPEN action)
     * - Voting state changes
     * - New comments are added
     */
    private fun rebuildItems() {
        val post = currentPost ?: return

        val comments = allComments
        val byId = comments.associateBy { it.id }

        // Find all root comments (top-level comments on the post)
        val roots = comments
            .filter { it.parentCommentId == null }
            .sortedBy { it.createdAt?.toDate()?.time ?: 0L }

        // Helper function to find the root comment ID for any comment in the thread
        fun findRootId(comment: Comment): String {
            var current = comment
            while (true) {
                val parentId = current.parentCommentId ?: return current.id
                val parent = byId[parentId] ?: return current.id
                current = parent
            }
        }

        // Group all nested replies by their root comment ID
        val repliesByRootId: Map<String, List<Comment>> =
            comments
                .filter { it.parentCommentId != null }
                .groupBy { findRootId(it) }

        val result = mutableListOf<FeedItem>()
        // Always show the post first
        result += FeedItem.PostItem(post)

        // For each root comment, show it and conditionally show its replies
        for (root in roots) {
            // Always show root comment
            // Update expansion state for UI binding
            root.isRepliesExpanded = expandedRootIds.contains(root.id)
            result += FeedItem.CommentItem(root)

            // Show nested replies only if this root is expanded
            if (expandedRootIds.contains(root.id)) {
                val replies = repliesByRootId[root.id].orEmpty()
                    .sortedBy { it.createdAt?.toDate()?.time ?: 0L }

                replies.forEach { reply ->
                    result += FeedItem.CommentItem(reply)
                }
            }
        }

        _items.value = result
    }

    /**
     * Handles voting action on a post using optimistic UI updates.
     * Updates UI immediately, then persists to server. Rolls back on error.
     *
     * @param post The post being voted on
     * @param isUpvote True for upvote, false for downvote
     */
    fun handleVote(post: Post, isUpvote: Boolean) {
        viewModelScope.launch {
            // Calculate optimistic update
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                _error.value = "User not logged in"
                return@launch
            }

            val currentVote = post.votedUsers[userId]
            var upvoteChange = 0
            var downvoteChange = 0
            var newVoteState: com.hcmus.forumus_client.data.model.VoteState

            if (isUpvote) {
                when (currentVote) {
                    com.hcmus.forumus_client.data.model.VoteState.UPVOTE -> {
                        upvoteChange = -1
                        newVoteState = com.hcmus.forumus_client.data.model.VoteState.NONE
                    }
                    com.hcmus.forumus_client.data.model.VoteState.DOWNVOTE -> {
                        upvoteChange = 1
                        downvoteChange = -1
                        newVoteState = com.hcmus.forumus_client.data.model.VoteState.UPVOTE
                    }
                    else -> {
                        upvoteChange = 1
                        newVoteState = com.hcmus.forumus_client.data.model.VoteState.UPVOTE
                    }
                }
            } else {
                when (currentVote) {
                    com.hcmus.forumus_client.data.model.VoteState.DOWNVOTE -> {
                        downvoteChange = -1
                        newVoteState = com.hcmus.forumus_client.data.model.VoteState.NONE
                    }
                    com.hcmus.forumus_client.data.model.VoteState.UPVOTE -> {
                        upvoteChange = -1
                        downvoteChange = 1
                        newVoteState = com.hcmus.forumus_client.data.model.VoteState.DOWNVOTE
                    }
                    else -> {
                        downvoteChange = 1
                        newVoteState = com.hcmus.forumus_client.data.model.VoteState.DOWNVOTE
                    }
                }
            }

            // Create optimistic post update
            val optimisticVotedUsers = post.votedUsers.toMutableMap()
            if (newVoteState == com.hcmus.forumus_client.data.model.VoteState.NONE) {
                optimisticVotedUsers.remove(userId)
            } else {
                optimisticVotedUsers[userId] = newVoteState
            }

            val optimisticPost = post.copy(
                upvoteCount = post.upvoteCount + upvoteChange,
                downvoteCount = post.downvoteCount + downvoteChange,
                userVote = newVoteState,
                votedUsers = optimisticVotedUsers
            )

            // Apply optimistic update immediately
            currentPost = optimisticPost
            rebuildItems()

            try {
                // Perform actual vote action on repository
                val actualUpdatedPost = if (isUpvote) {
                    postRepository.toggleUpvote(post)
                } else {
                    postRepository.toggleDownvote(post)
                }

                // Update with actual server response
                currentPost = actualUpdatedPost
                rebuildItems()
            } catch (e: Exception) {
                // Rollback optimistic update on error
                currentPost = post
                rebuildItems()
                _error.value = e.message
            }
        }
    }

    /**
     * Handles voting action on a comment using optimistic UI updates.
     * Updates UI immediately, then persists to server. Rolls back on error.
     *
     * @param comment The comment being voted on
     * @param isUpvote True for upvote, false for downvote
     */
    fun handleVote(comment: Comment, isUpvote: Boolean) {
        viewModelScope.launch {
            // Calculate optimistic update
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                _error.value = "User not logged in"
                return@launch
            }

            val currentVote = comment.votedUsers[userId]
            var upvoteChange = 0
            var downvoteChange = 0
            var newVoteState: com.hcmus.forumus_client.data.model.VoteState

            if (isUpvote) {
                when (currentVote) {
                    com.hcmus.forumus_client.data.model.VoteState.UPVOTE -> {
                        upvoteChange = -1
                        newVoteState = com.hcmus.forumus_client.data.model.VoteState.NONE
                    }
                    com.hcmus.forumus_client.data.model.VoteState.DOWNVOTE -> {
                        upvoteChange = 1
                        downvoteChange = -1
                        newVoteState = com.hcmus.forumus_client.data.model.VoteState.UPVOTE
                    }
                    else -> {
                        upvoteChange = 1
                        newVoteState = com.hcmus.forumus_client.data.model.VoteState.UPVOTE
                    }
                }
            } else {
                when (currentVote) {
                    com.hcmus.forumus_client.data.model.VoteState.DOWNVOTE -> {
                        downvoteChange = -1
                        newVoteState = com.hcmus.forumus_client.data.model.VoteState.NONE
                    }
                    com.hcmus.forumus_client.data.model.VoteState.UPVOTE -> {
                        upvoteChange = -1
                        downvoteChange = 1
                        newVoteState = com.hcmus.forumus_client.data.model.VoteState.DOWNVOTE
                    }
                    else -> {
                        downvoteChange = 1
                        newVoteState = com.hcmus.forumus_client.data.model.VoteState.DOWNVOTE
                    }
                }
            }

            // Create optimistic comment update
            val optimisticVotedUsers = comment.votedUsers.toMutableMap()
            if (newVoteState == com.hcmus.forumus_client.data.model.VoteState.NONE) {
                optimisticVotedUsers.remove(userId)
            } else {
                optimisticVotedUsers[userId] = newVoteState
            }

            val optimisticComment = comment.copy(
                upvoteCount = comment.upvoteCount + upvoteChange,
                downvoteCount = comment.downvoteCount + downvoteChange,
                userVote = newVoteState,
                votedUsers = optimisticVotedUsers
            )

            // Apply optimistic update immediately
            allComments = allComments.map { c ->
                if (c.id == comment.id) optimisticComment else c
            }
            rebuildItems()

            try {
                // Perform actual vote action on repository
                val actualUpdatedComment = if (isUpvote) {
                    commentRepository.toggleUpvote(comment)
                } else {
                    commentRepository.toggleDownvote(comment)
                }

                // Update with actual server response
                allComments = allComments.map { c ->
                    if (c.id == comment.id) actualUpdatedComment else c
                }
                rebuildItems()
            } catch (e: Exception) {
                // Rollback optimistic update on error
                allComments = allComments.map { c ->
                    if (c.id == comment.id) comment else c
                }
                rebuildItems()
                _error.value = e.message
            }
        }
    }

    /**
     * Handles reply action for a comment.
     *
     * Sets the target comment so the input bar knows which comment is being replied to.
     *
     * @param comment The comment being replied to
     */
    fun handleReply(comment: Comment) {
        // Set this comment as the reply target for the input bar
        _replyTargetComment.value = comment
    }

    /**
     * Handles reply action for a post.
     *
     * Clears the target comment (since replying to post, not a specific comment)
     * and opens the input bar with keyboard.
     *
     * @param post The post being replied to
     */
    fun handleReply(post: Post) {
        // Clear target comment when replying to post
        _replyTargetComment.value = null

        // Signal to Activity to open keyboard and focus input
        _openReplyInput.value = true
    }

    /**
     * Handles expand/collapse action for a comment.
     *
     * For root comments (parentCommentId == null), toggles their expansion state
     * to show/hide nested replies. Rebuilds items to reflect the change.
     *
     * @param comment The comment being toggled
     */
    fun handleOpen(comment: Comment) {
        // Only toggle expansion for root comments (those without a parent)
        if (comment.parentCommentId == null) {
            if (expandedRootIds.contains(comment.id)) {
                expandedRootIds.remove(comment.id)
            } else {
                expandedRootIds.add(comment.id)
            }
            rebuildItems()
        }
    }

    /**
     * Toggles the visibility of replies for a comment.
     * Alias for handleOpen to provide semantic clarity when triggered by VIEW_REPLIES button.
     *
     * @param comment The comment whose replies should be shown/hidden
     */
    fun toggleReplies(comment: Comment) {
        handleOpen(comment)
    }

    /**
     * Sends a new comment or reply to the post.
     *
     * Creates a new Comment object with:
     * - If replyTargetComment is null: replies to the post directly
     * - If replyTargetComment is not null: replies to that specific comment
     *
     * Process:
     * 1. Validate text input (not blank)
     * 2. Create Comment with appropriate parent/reply references
     * 3. Submit to repository
     * 4. Update post comment count
     * 5. Ensure root comment is expanded if replying to nested comment
     * 6. Update local comment list and recalculate counts
     * 7. Reset reply state
     *
     * @param rawText The comment text (will be trimmed)
     */
    fun sendComment(rawText: String) {
        val text = rawText.trim()
        if (text.isBlank()) return

        val post = currentPost ?: return

        // If null, replying to post; if not null, replying to that comment
        val targetComment = _replyTargetComment.value

        viewModelScope.launch {
            try {
                val user = userRepository.getCurrentUser() ?: return@launch

                val comment = Comment(
                    postId = post.id,
                    content = text,
                    isOriginalPoster = user.uid == post.authorId,
                    parentCommentId = targetComment?.id,          // null = reply to post, otherwise = reply to comment
                    replyToUserId = targetComment?.authorId,
                    replyToUserName = targetComment?.authorName
                )

                val newComment = commentRepository.saveComment(comment)

                // Increment post's comment count
                post.commentCount++
                postRepository.updatePost(post)

                // If replying to a root comment, ensure it's expanded to see the new reply
                if (targetComment != null && targetComment.parentCommentId == null) {
                    expandedRootIds.add(targetComment.id)
                }

                // Update local comment list and rebuild
                allComments = allComments + newComment
                commentRepository.applyCommentCounts(allComments)
                rebuildItems()

                // Reset reply state - back to replying to post
                _replyTargetComment.value = null
            } catch (e: Exception) {
                // Error handled silently - could show toast if needed
            }
        }
    }

    /**
     * Saves a report for a post when user selects a violation. Creates a Report object, saves it to
     * Firebase, increments reportCount, and adds userId to reportedUsers list in the post.
     *
     * @param post The post being reported
     * @param violation The violation category selected by the user
     */
    fun saveReport(post: Post, violation: Violation) {
        viewModelScope.launch {
            try {
                val currentUser = userRepository.getCurrentUser()
                val userId =
                    currentUser?.uid
                        ?: FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

                // Check if user has already reported this post
                if (post.reportedUsers.contains(userId)) {
                    Toast.makeText(
                        applicationContext,
                        "You have already reported this post",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    return@launch
                }

                // Create report object
                val report =
                    Report(
                        postId = post.id,
                        authorId = userId,
                        nameViolation = violation.name,
                        descriptionViolation = violation
                    )

                // Save report to Firebase
                reportRepository.saveReport(report)

                Toast.makeText(applicationContext, "Post reported", Toast.LENGTH_SHORT).show()

                // Update post: increment reportCount and add userId to reportedUsers
                val updatedPost =
                    post.copy(
                        reportCount = post.reportCount + 1,
                        reportedUsers =
                            post.reportedUsers.toMutableList().apply { add(userId) }
                    )

                // Update post in Firebase via repository
                postRepository.updatePost(updatedPost)

                // Update post with the updated post
                loadPostDetail(currentPost!!.id)

                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to report post: ${e.message}"
            }
        }
    }

    /**
     * Saves a post for the current user.
     *
     * @param post The post to save
     */
    fun savePost(post: Post) {
        viewModelScope.launch {
            val result = userRepository.savePost(post.id)
            _savePostResult.value = result
        }
    }

    /**
     * Clears the save post result after it has been handled by the UI
     */
    fun clearSavePostResult() {
        _savePostResult.value = null
    }

    /**
     * Requests an AI-generated summary for the current post.
     * Prevents duplicate requests while one is in progress.
     */
    fun requestSummary() {
        val postId = currentPost?.id ?: return
        if (_isSummaryLoading.value == true) return

        viewModelScope.launch {
            _isSummaryLoading.value = true
            try {
                val result = postRepository.getPostSummary(postId)
                _summaryResult.value = result
            } catch (e: Exception) {
                _summaryResult.value = Result.failure(e)
            } finally {
                _isSummaryLoading.value = false
            }
        }
    }

    /**
     * Clears the summary result after it has been handled by the UI.
     */
    fun clearSummaryResult() {
        _summaryResult.value = null
    }
}
