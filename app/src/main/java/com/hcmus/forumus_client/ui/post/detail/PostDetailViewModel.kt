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

/** Manages post detail screen with hierarchical comment threading. */
class PostDetailViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val postRepository: PostRepository = PostRepository(),
    private val commentRepository: CommentRepository = CommentRepository(),
    private val reportRepository: ReportRepository = ReportRepository()
) : ViewModel() {

    fun initSummaryCache(context: Context) {
        postRepository.initSummaryCache(context.applicationContext)
    }

    private val _items = MutableLiveData<List<FeedItem>>(emptyList())
    val items: LiveData<List<FeedItem>> = _items

    private var currentPost: Post? = null
    private var allComments: List<Comment> = emptyList()
    private val expandedRootIds = mutableSetOf<String>()

    private val _openReplyInput = MutableLiveData<Boolean>(false)
    val openReplyInput: LiveData<Boolean> = _openReplyInput

    private val _replyTargetComment = MutableLiveData<Comment?>()
    val replyTargetComment: LiveData<Comment?> = _replyTargetComment

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _savePostResult = MutableLiveData<SavePostResult?>()
    val savePostResult: LiveData<SavePostResult?> = _savePostResult

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

    fun loadPostDetail(postId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val post = postRepository.getPostById(postId)
                    ?: throw IllegalStateException("Post not found")

                currentPost = post

                val comments = commentRepository.getCommentsByPost(postId)
                allComments = comments

                rebuildItems()
            } catch (e: Exception) {
                _items.value = emptyList()
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Rebuilds the display list with post and comments. */
    private fun rebuildItems() {
        val post = currentPost ?: return

        val comments = allComments
        val byId = comments.associateBy { it.id }

        val roots = comments
            .filter { it.parentCommentId == null }
            .sortedBy { it.createdAt?.toDate()?.time ?: 0L }

        // Walk up the parent chain to find the root
        fun findRootId(comment: Comment): String {
            var current = comment
            while (true) {
                val parentId = current.parentCommentId ?: return current.id
                val parent = byId[parentId] ?: return current.id
                current = parent
            }
        }

        val repliesByRootId: Map<String, List<Comment>> =
            comments
                .filter { it.parentCommentId != null }
                .groupBy { findRootId(it) }

        val result = mutableListOf<FeedItem>()
        result += FeedItem.PostItem(post)

        for (root in roots) {
            root.isRepliesExpanded = expandedRootIds.contains(root.id)
            result += FeedItem.CommentItem(root)

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

    // Optimistic UI update: show change immediately, rollback if server fails
    fun handleVote(post: Post, isUpvote: Boolean) {
        viewModelScope.launch {
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

            currentPost = optimisticPost
            rebuildItems()

            try {
                val actualUpdatedPost = if (isUpvote) {
                    postRepository.toggleUpvote(post)
                } else {
                    postRepository.toggleDownvote(post)
                }

                currentPost = actualUpdatedPost
                rebuildItems()
            } catch (e: Exception) {
                currentPost = post
                rebuildItems()
                _error.value = e.message
            }
        }
    }

    fun handleVote(comment: Comment, isUpvote: Boolean) {
        viewModelScope.launch {
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

            allComments = allComments.map { c ->
                if (c.id == comment.id) optimisticComment else c
            }
            rebuildItems()

            try {
                val actualUpdatedComment = if (isUpvote) {
                    commentRepository.toggleUpvote(comment)
                } else {
                    commentRepository.toggleDownvote(comment)
                }

                allComments = allComments.map { c ->
                    if (c.id == comment.id) actualUpdatedComment else c
                }
                rebuildItems()
            } catch (e: Exception) {
                allComments = allComments.map { c ->
                    if (c.id == comment.id) comment else c
                }
                rebuildItems()
                _error.value = e.message
            }
        }
    }

    fun handleReply(comment: Comment) {
        _replyTargetComment.value = comment
    }

    fun handleReply(post: Post) {
        _replyTargetComment.value = null
        _openReplyInput.value = true
    }

    fun cancelReply() {
        _replyTargetComment.value = null
    }

    // Only root comments can be expanded/collapsed
    fun handleOpen(comment: Comment) {
        if (comment.parentCommentId == null) {
            if (expandedRootIds.contains(comment.id)) {
                expandedRootIds.remove(comment.id)
            } else {
                expandedRootIds.add(comment.id)
            }
            rebuildItems()
        }
    }

    fun toggleReplies(comment: Comment) {
        handleOpen(comment)
    }

    /** Creates a comment replying to the post or a specific comment. */
    fun sendComment(rawText: String) {
        val text = rawText.trim()
        if (text.isBlank()) return

        val post = currentPost ?: return
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

                post.commentCount++
                postRepository.updatePost(post)

                // Expand the root to show the new reply
                if (targetComment != null && targetComment.parentCommentId == null) {
                    expandedRootIds.add(targetComment.id)
                }

                allComments = allComments + newComment
                commentRepository.applyCommentCounts(allComments)
                rebuildItems()

                _replyTargetComment.value = null
            } catch (e: Exception) {
            }
        }
    }

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

    fun savePost(post: Post) {
        viewModelScope.launch {
            val result = userRepository.savePost(post.id)
            _savePostResult.value = result
        }
    }

    fun clearSavePostResult() {
        _savePostResult.value = null
    }

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

    fun clearSummaryResult() {
        _summaryResult.value = null
    }
}
