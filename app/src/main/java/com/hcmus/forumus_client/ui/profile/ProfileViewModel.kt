package com.hcmus.forumus_client.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.hcmus.forumus_client.data.model.Comment
import com.hcmus.forumus_client.data.model.CommentAction
import com.hcmus.forumus_client.data.model.FeedItem
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.model.PostAction
import com.hcmus.forumus_client.data.model.User
import com.hcmus.forumus_client.data.model.createdAtMillis
import com.hcmus.forumus_client.data.repository.CommentRepository
import com.hcmus.forumus_client.data.repository.PostRepository
import com.hcmus.forumus_client.data.repository.UserRepository
import com.hcmus.forumus_client.data.repository.ReportRepository
import com.hcmus.forumus_client.data.repository.SavePostResult
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.hcmus.forumus_client.data.model.Report
import com.hcmus.forumus_client.data.model.Violation
import kotlinx.coroutines.launch
import okhttp3.internal.platform.PlatformRegistry.applicationContext

/**
 * ViewModel for managing user profile data and interactions.
 *
 * Responsibilities:
 * - Load and manage user profile information (name, email, avatar, etc.)
 * - Fetch user's posts and comments from repositories
 * - Handle content filtering (GENERAL/POSTS/REPLIES modes)
 * - Calculate and expose user statistics (post count, comment count, upvote count)
 * - Handle voting actions on user's posts and comments
 * - Manage loading and error states
 *
 * The view model uses MediatorLiveData to automatically recompute visible items
 * whenever the display mode or content changes, eliminating the need for manual
 * updates when switching between GENERAL, POSTS, and REPLIES views.
 *
 * @param postRepository Repository for post operations
 * @param commentRepository Repository for comment operations
 * @param userRepository Repository for user operations
 */
class ProfileViewModel(
    private val postRepository: PostRepository = PostRepository(),
    private val commentRepository: CommentRepository = CommentRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val reportRepository: ReportRepository = ReportRepository()
) : ViewModel() {

    private lateinit var userId: String

    // User profile information
    private val _user = MutableLiveData<User>()
    val user: LiveData<User> = _user

    // User's posts
    private val _posts = MutableLiveData<List<Post>>(emptyList())
    val posts: LiveData<List<Post>> = _posts

    // User's comments
    private val _comments = MutableLiveData<List<Comment>>(emptyList())
    val comments: LiveData<List<Comment>> = _comments

    // Combined list of posts and comments (mixed chronologically)
    private val _mixedItems = MutableLiveData<List<FeedItem>>(emptyList())
    val mixedItems: LiveData<List<FeedItem>> = _mixedItems

    // Current display mode (GENERAL, POSTS, or REPLIES)
    private val _mode = MutableLiveData<ProfileMode>(ProfileMode.GENERAL)
    val mode: LiveData<ProfileMode> = _mode

    // List filtered based on current display mode, automatically recomputed
    private val _visibleItems = MediatorLiveData<List<FeedItem>>()
    val visibleItems: LiveData<List<FeedItem>> = _visibleItems

    // Loading state indicator
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error message (null if no error)
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Save post result for UI feedback
    private val _savePostResult = MutableLiveData<SavePostResult?>()
    val savePostResult: LiveData<SavePostResult?> = _savePostResult

    // Statistics
    private val _postsCount = MutableLiveData<Int>(0)
    val postsCount: LiveData<Int> = _postsCount

    private val _repliesCount = MutableLiveData<Int>(0)
    val repliesCount: LiveData<Int> = _repliesCount

    private val _upvotesCount = MutableLiveData<Int>(0)
    val upvotesCount: LiveData<Int> = _upvotesCount

    init {
        // Set up MediatorLiveData sources to automatically recompute visible items
        // when any of these LiveData values change
        _visibleItems.addSource(_mode) { recomputeVisibleItems() }
        _visibleItems.addSource(_posts) { recomputeVisibleItems() }
        _visibleItems.addSource(_comments) { recomputeVisibleItems() }
        _visibleItems.addSource(_mixedItems) { recomputeVisibleItems() }
    }

    /**
     * Initializes the view model with a specific user ID and display mode.
     *
     * Should be called from the Activity's onCreate or onNewIntent.
     * Triggers loading of user information and content.
     *
     * @param userId The ID of the user whose profile to display
     * @param initialMode The initial display mode (GENERAL, POSTS, or REPLIES)
     */
    fun loadUserInfo(userId: String) {
        this.userId = userId
        _mode.value = ProfileMode.GENERAL

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userData = userRepository.getUserById(userId)
                _user.value = userData

                // Fetch all posts for the user, sorted newest first
                val allPosts = postRepository.getPostsByUser(userId)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }

                // Fetch all comments for the user, sorted newest first
                val allComments = commentRepository.getCommentsByUser(userId)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }

                _posts.value = allPosts
                _comments.value = allComments

                // Calculate and update statistics
                updateStats(allPosts, allComments)

                // Build mixed list combining posts and comments, sorted by creation time
                val mixed = mutableListOf<FeedItem>()
                mixed += allPosts.map { FeedItem.PostItem(it) }
                mixed += allComments.map { FeedItem.CommentItem(it) }
                mixed.sortByDescending { it.createdAtMillis() }
                _mixedItems.value = mixed

                _error.value = null
            } catch (e: Exception) {
                Log.d("ProfileViewModel", "Error loading user info", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Changes the current display mode for filtering content.
     *
     * The visibleItems list will automatically update through MediatorLiveData
     * without requiring additional repository calls.
     *
     * @param mode The new ProfileMode to display
     */
    fun setMode(mode: ProfileMode) {
        if (_mode.value == mode) return
        _mode.value = mode
    }

    /**
     * Updates user statistics based on posts and comments.
     *
     * Calculates:
     * - Total number of posts
     * - Total number of comments (replies)
     * - Total upvotes across all posts and comments
     *
     * @param posts The user's posts
     * @param comments The user's comments
     */
    private fun updateStats(posts: List<Post>, comments: List<Comment>) {
        _postsCount.value = posts.size
        _repliesCount.value = comments.size

        val totalUpvotes = posts.sumOf { it.upvoteCount } + comments.sumOf { it.upvoteCount }
        _upvotesCount.value = totalUpvotes
    }

    /**
     * Handles user interaction with a comment (upvote/downvote).
     *
     * Delegates to handleVote for vote processing.
     *
     * @param comment The comment being voted on
     * @param commentAction The action type (UPVOTE or DOWNVOTE)
     */
    fun onCommentAction(comment: Comment, commentAction: CommentAction) {
        when (commentAction) {
            CommentAction.UPVOTE -> handleVote(comment, isUpvote = true)
            CommentAction.DOWNVOTE -> handleVote(comment, isUpvote = false)
            else -> Unit
        }
    }

    /**
     * Processes a vote action on a comment.
     *
     * Toggles the vote on the server via repository, then updates the local
     * comment list and recalculates statistics.
     *
     * @param comment The comment to vote on
     * @param isUpvote True for upvote, false for downvote
     */
    private fun handleVote(comment: Comment, isUpvote: Boolean) {
        viewModelScope.launch {
            try {
                // Toggle vote on server and get updated comment
                val updatedComment = if (isUpvote) {
                    commentRepository.toggleUpvote(comment)
                } else {
                    commentRepository.toggleDownvote(comment)
                }

                // Update local comment list with the new state
                val currentList = _comments.value ?: emptyList()
                val newComments = currentList.map { c ->
                    if (c.id == comment.id) updatedComment else c
                }
                _comments.value = newComments

                // Recalculate statistics
                val currentPosts = _posts.value ?: emptyList()
                updateStats(currentPosts, newComments)

                // Rebuild mixed items list
                recomputeMixedItems()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    /**
     * Handles user interaction with a post (upvote/downvote).
     *
     * Delegates to handleVote for vote processing.
     *
     * @param post The post being voted on
     * @param postAction The action type (UPVOTE or DOWNVOTE)
     */
    fun onPostAction(post: Post, postAction: PostAction) {
        when (postAction) {
            PostAction.UPVOTE -> handleVote(post, isUpvote = true)
            PostAction.DOWNVOTE -> handleVote(post, isUpvote = false)
            else -> Unit
        }
    }

    /**
     * Processes a vote action on a post.
     *
     * Toggles the vote on the server via repository, then updates the local
     * post list and recalculates statistics.
     *
     * @param post The post to vote on
     * @param isUpvote True for upvote, false for downvote
     */
    private fun handleVote(post: Post, isUpvote: Boolean) {
        viewModelScope.launch {
            try {
                // Toggle vote on server and get updated post
                val updatedPost = if (isUpvote) {
                    postRepository.toggleUpvote(post)
                } else {
                    postRepository.toggleDownvote(post)
                }

                // Update local post list with the new state
                val currentList = _posts.value ?: emptyList()
                val newPosts = currentList.map { p ->
                    if (p.id == post.id) updatedPost else p
                }
                _posts.value = newPosts

                // Recalculate statistics
                val currentComments = _comments.value ?: emptyList()
                updateStats(newPosts, currentComments)

                // Rebuild mixed items list
                recomputeMixedItems()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    /**
     * Rebuilds the mixed items list from current posts and comments.
     *
     * Sorts the combined list chronologically (newest first) and updates mixedItems.
     * Called automatically whenever posts or comments change.
     */
    private fun recomputeMixedItems() {
        val postsList = _posts.value ?: emptyList()
        val commentsList = _comments.value ?: emptyList()

        val mixed = mutableListOf<FeedItem>()
        mixed += postsList.map { FeedItem.PostItem(it) }
        mixed += commentsList.map { FeedItem.CommentItem(it) }

        mixed.sortByDescending { it.createdAtMillis() }
        _mixedItems.value = mixed
    }

    /**
     * Filters the mixed items list based on the current display mode.
     *
     * Automatically called by MediatorLiveData when mode, posts, comments, or
     * mixedItems change.
     *
     * Modes:
     * - GENERAL: Shows all posts and comments mixed
     * - POSTS: Shows only posts
     * - REPLIES: Shows only comments
     */
    private fun recomputeVisibleItems() {
        val currentMode = _mode.value ?: ProfileMode.GENERAL
        val postsList = _posts.value ?: emptyList()
        val commentsList = _comments.value ?: emptyList()
        val mixedList = _mixedItems.value ?: emptyList()

        val result: List<FeedItem> = when (currentMode) {
            ProfileMode.GENERAL -> mixedList
            ProfileMode.POSTS -> postsList.map { FeedItem.PostItem(it) }
            ProfileMode.REPLIES -> commentsList.map { FeedItem.CommentItem(it) }
        }
        _visibleItems.value = result
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

                // Update posts list with the updated post
                val currentList = _posts.value ?: emptyList()
                _posts.value = currentList.map { p -> if (p.id == post.id) updatedPost else p }

                // Rebuild mixed items list
                recomputeMixedItems()

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
}