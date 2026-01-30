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
 * Manages profile data: user info, posts, comments, and statistics.
 * Uses MediatorLiveData to auto-update visible items when mode or content changes.
 */
class ProfileViewModel(
    private val postRepository: PostRepository = PostRepository(),
    private val commentRepository: CommentRepository = CommentRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val reportRepository: ReportRepository = ReportRepository()
) : ViewModel() {

    private lateinit var userId: String

    private val _user = MutableLiveData<User>()
    val user: LiveData<User> = _user

    private val _posts = MutableLiveData<List<Post>>(emptyList())
    val posts: LiveData<List<Post>> = _posts

    private val _comments = MutableLiveData<List<Comment>>(emptyList())
    val comments: LiveData<List<Comment>> = _comments

    // Posts and comments combined, sorted by time (for GENERAL mode)
    private val _mixedItems = MutableLiveData<List<FeedItem>>(emptyList())
    val mixedItems: LiveData<List<FeedItem>> = _mixedItems

    private val _mode = MutableLiveData<ProfileMode>(ProfileMode.GENERAL)
    val mode: LiveData<ProfileMode> = _mode

    // Filtered based on current mode - recomputed automatically via MediatorLiveData
    private val _visibleItems = MediatorLiveData<List<FeedItem>>()
    val visibleItems: LiveData<List<FeedItem>> = _visibleItems

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _savePostResult = MutableLiveData<SavePostResult?>()
    val savePostResult: LiveData<SavePostResult?> = _savePostResult

    // Profile statistics
    private val _postsCount = MutableLiveData<Int>(0)
    val postsCount: LiveData<Int> = _postsCount

    private val _repliesCount = MutableLiveData<Int>(0)
    val repliesCount: LiveData<Int> = _repliesCount

    private val _upvotesCount = MutableLiveData<Int>(0)
    val upvotesCount: LiveData<Int> = _upvotesCount

    init {
        // Recompute visible items whenever mode or content changes
        _visibleItems.addSource(_mode) { recomputeVisibleItems() }
        _visibleItems.addSource(_posts) { recomputeVisibleItems() }
        _visibleItems.addSource(_comments) { recomputeVisibleItems() }
        _visibleItems.addSource(_mixedItems) { recomputeVisibleItems() }
    }

    /**
     * Loads user profile, posts, and comments. Resets mode to GENERAL.
     */
    fun loadUserInfo(userId: String) {
        this.userId = userId
        _mode.value = ProfileMode.GENERAL

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userData = userRepository.getUserById(userId)
                _user.value = userData

                // Fetch posts and comments, sorted newest first
                val allPosts = postRepository.getPostsByUser(userId)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                val allComments = commentRepository.getCommentsByUser(userId)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }

                _posts.value = allPosts
                _comments.value = allComments
                updateStats(allPosts, allComments)

                // Build mixed list for GENERAL mode
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

    fun setMode(mode: ProfileMode) {
        if (_mode.value == mode) return
        _mode.value = mode
    }

    private fun updateStats(posts: List<Post>, comments: List<Comment>) {
        _postsCount.value = posts.size
        _repliesCount.value = comments.size

        val totalUpvotes = posts.sumOf { it.upvoteCount } + comments.sumOf { it.upvoteCount }
        _upvotesCount.value = totalUpvotes
    }

    fun onCommentAction(comment: Comment, commentAction: CommentAction) {
        when (commentAction) {
            CommentAction.UPVOTE -> handleVote(comment, isUpvote = true)
            CommentAction.DOWNVOTE -> handleVote(comment, isUpvote = false)
            else -> Unit
        }
    }

    // Toggles vote on server, updates local list, and recalculates stats
    private fun handleVote(comment: Comment, isUpvote: Boolean) {
        viewModelScope.launch {
            try {
                val updatedComment = if (isUpvote) {
                    commentRepository.toggleUpvote(comment)
                } else {
                    commentRepository.toggleDownvote(comment)
                }

                val currentList = _comments.value ?: emptyList()
                val newComments = currentList.map { c ->
                    if (c.id == comment.id) updatedComment else c
                }
                _comments.value = newComments

                val currentPosts = _posts.value ?: emptyList()
                updateStats(currentPosts, newComments)
                recomputeMixedItems()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun onPostAction(post: Post, postAction: PostAction) {
        when (postAction) {
            PostAction.UPVOTE -> handleVote(post, isUpvote = true)
            PostAction.DOWNVOTE -> handleVote(post, isUpvote = false)
            else -> Unit
        }
    }

    private fun handleVote(post: Post, isUpvote: Boolean) {
        viewModelScope.launch {
            try {
                val updatedPost = if (isUpvote) {
                    postRepository.toggleUpvote(post)
                } else {
                    postRepository.toggleDownvote(post)
                }

                val currentList = _posts.value ?: emptyList()
                val newPosts = currentList.map { p ->
                    if (p.id == post.id) updatedPost else p
                }
                _posts.value = newPosts

                val currentComments = _comments.value ?: emptyList()
                updateStats(newPosts, currentComments)
                recomputeMixedItems()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    // Combines posts and comments into a single list sorted by time
    private fun recomputeMixedItems() {
        val postsList = _posts.value ?: emptyList()
        val commentsList = _comments.value ?: emptyList()

        val mixed = mutableListOf<FeedItem>()
        mixed += postsList.map { FeedItem.PostItem(it) }
        mixed += commentsList.map { FeedItem.CommentItem(it) }

        mixed.sortByDescending { it.createdAtMillis() }
        _mixedItems.value = mixed
    }

    // Filters content based on mode: GENERAL=mixed, POSTS=posts only, REPLIES=comments only
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

    fun saveReport(post: Post, violation: Violation) {
        viewModelScope.launch {
            try {
                val currentUser = userRepository.getCurrentUser()
                val userId =
                    currentUser?.uid
                        ?: FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

                // Prevent duplicate reports
                if (post.reportedUsers.contains(userId)) {
                    Toast.makeText(
                        applicationContext,
                        "You have already reported this post",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    return@launch
                }

                val report =
                    Report(
                        postId = post.id,
                        authorId = userId,
                        nameViolation = violation.name,
                        descriptionViolation = violation
                    )

                reportRepository.saveReport(report)
                Toast.makeText(applicationContext, "Post reported", Toast.LENGTH_SHORT).show()

                // Update post with new report info
                val updatedPost =
                    post.copy(
                        reportCount = post.reportCount + 1,
                        reportedUsers =
                            post.reportedUsers.toMutableList().apply { add(userId) }
                    )

                postRepository.updatePost(updatedPost)

                val currentList = _posts.value ?: emptyList()
                _posts.value = currentList.map { p -> if (p.id == post.id) updatedPost else p }
                recomputeMixedItems()

                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to report post: ${e.message}"
            }
        }
    }

    fun savePost(post: Post) {
        viewModelScope.launch {
            val result = userRepository.savePost(post.id)
            _savePostResult.value = result
        }
    }

    fun clearSavePostResult() {
        _savePostResult.value = null
    }
}