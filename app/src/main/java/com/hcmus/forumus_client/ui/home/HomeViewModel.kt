package com.hcmus.forumus_client.ui.home

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.model.PostAction
import com.hcmus.forumus_client.data.model.PostStatus
import com.hcmus.forumus_client.data.model.Report
import com.hcmus.forumus_client.data.model.Violation
import com.hcmus.forumus_client.data.repository.PostRepository
import com.hcmus.forumus_client.data.repository.ReportRepository
import com.hcmus.forumus_client.data.repository.UserRepository
import com.hcmus.forumus_client.data.repository.SavePostResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.internal.platform.PlatformRegistry.applicationContext
import com.hcmus.forumus_client.data.model.UserRole
import com.hcmus.forumus_client.data.model.User

/** ViewModel for the Home screen. Manages loading posts and users, handling voting interactions. */
class HomeViewModel(
        private val userRepository: UserRepository = UserRepository(),
        private val postRepository: PostRepository = PostRepository(),
        private val reportRepository: ReportRepository = ReportRepository()
) : ViewModel() {

    /**
     * Initializes the summary cache with context. Call this from the Fragment/Activity.
     */
    fun initSummaryCache(context: Context) {
        postRepository.initSummaryCache(context.applicationContext)
    }

    // List of posts for the home feed
    private val _posts = MutableLiveData<List<Post>>(emptyList())
    val posts: LiveData<List<Post>> = _posts

    // Loading state indicator
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Loading more indicator for pagination
    private val _isLoadingMore = MutableLiveData<Boolean>(false)
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    // Error message for UI display
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Save post result for UI feedback
    private val _savePostResult = MutableLiveData<SavePostResult?>()
    val savePostResult: LiveData<SavePostResult?> = _savePostResult

    // AI Summary state
    private val _summaryResult = MutableLiveData<Pair<String, Result<String>>?>()
    val summaryResult: LiveData<Pair<String, Result<String>>?> = _summaryResult

    private val _summaryLoadingPostId = MutableLiveData<String?>()
    val summaryLoadingPostId: LiveData<String?> = _summaryLoadingPostId

    // Pagination state
    private var lastDocument: com.google.firebase.firestore.DocumentSnapshot? = null
    private var hasMorePosts = true
    private val pageSize = 10L

    // List of topics for the drawer
    private val _topics =
            MutableLiveData<List<com.hcmus.forumus_client.data.model.Topic>>(emptyList())
    val topics: LiveData<List<com.hcmus.forumus_client.data.model.Topic>> = _topics

    enum class SortOption {
        NONE,
        NEW,
        TRENDING
    }

    // Sorting state
    private val _sortOption = MutableLiveData(SortOption.NONE)
    val sortOption: LiveData<SortOption> = _sortOption

    // Selected topics for filtering
    private val _selectedTopics = MutableLiveData<Set<String>>(emptySet())
    val selectedTopics: LiveData<Set<String>> = _selectedTopics



    /** Fetches topics from Firestore. */
    fun loadTopics() {
        viewModelScope.launch {
            try {
                val snapshot = FirebaseFirestore.getInstance().collection("topics").get().await()
                val topicList =
                        snapshot.toObjects(com.hcmus.forumus_client.data.model.Topic::class.java)
                _topics.value = topicList
            } catch (e: Exception) {
                // Handle error or use default topics if needed
                e.printStackTrace()
            }
        }
    }

    /**
     * Fetches initial posts from the repository using pagination. Manages loading state and error
     * handling.
     */
    fun loadPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Reset pagination state
                lastDocument = null
                hasMorePosts = true

                val posts: List<Post>
                val lastDoc: com.google.firebase.firestore.DocumentSnapshot?

                // Check for active filters
                val selected = _selectedTopics.value ?: emptySet()
                if (selected.isNotEmpty()) {
                    // SERVER-SIDE FILTERING: Fetch only topics
                    // Limitation: Only filtering by the first selected topic for now
                    // due to Firestore limitations on 'OR' queries without complex client-merging.
                    posts = postRepository.getPostsByTopic(selected.first(), pageSize)
                    lastDoc = null // Pagination not yet implemented for topic search
                    hasMorePosts = false 
                } else {
                     // STANDARD FEED
                    val result = postRepository.getPostsPaginated(pageSize, null)
                    posts = result.first
                    lastDoc = result.second
                    hasMorePosts = posts.size >= pageSize
                }

                lastDocument = lastDoc
                _posts.value = posts
                // Sorting logic can still be applied client-side to the fetched batch
                applyClientSideSort(posts)
                
                _error.value = null
            } catch (e: Exception) {
                Log.d("HomeViewModel", "loadPosts: Exception ${e.message}")
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Loads more posts for infinite scroll. Only loads if not already loading and there are more
     * posts available.
     */
    fun loadMorePosts() {
        // Don't load if already loading or no more posts
        if (_isLoadingMore.value == true || !hasMorePosts || _isLoading.value == true) {
            return
        }

        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val (newPosts, lastDoc) = postRepository.getPostsPaginated(pageSize, lastDocument)
                lastDocument = lastDoc
                hasMorePosts = newPosts.size >= pageSize

                // Append new posts to original list
                // Append new posts to current list
                val currentList = _posts.value ?: emptyList()
                val combinedList = currentList + newPosts
                
                _posts.value = combinedList
                applyClientSideSort(combinedList)

                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    /** Applie filtering logic. */
    /** Applies client-side sorting to the current list locally. */
    private fun applyClientSideSort(currentList: List<Post>) {
        val sort = _sortOption.value ?: SortOption.NONE
        val sortedList =
                when (sort) {
                    SortOption.NEW -> currentList.sortedByDescending { it.createdAt }
                    SortOption.TRENDING ->
                            currentList.sortedByDescending {
                                it.upvoteCount + it.downvoteCount + it.commentCount
                            }
                    SortOption.NONE -> currentList // Keep server order (usually NEW)
                }

        _posts.value = sortedList
    }

    /**
     * Toggles the sort option. If the same option is clicked, it toggles off to NONE.
     *
     * @param option The sort option to toggle
     */
    fun toggleSort(option: SortOption) {
        val current = _sortOption.value ?: SortOption.NONE
        if (current == option) {
            _sortOption.value = SortOption.NONE
        } else {
            _sortOption.value = option
        }
        applyClientSideSort(_posts.value ?: emptyList())
    }

    /**
     * Toggles the selection of a topic for filtering. Enforces a maximum of 5 selected topics.
     *
     * @param topicId The ID of the topic to toggle
     */
    fun toggleTopicSelection(topicId: String) {
        val currentSelection = _selectedTopics.value?.toMutableSet() ?: mutableSetOf()

        if (currentSelection.contains(topicId)) {
            currentSelection.remove(topicId)
        } else {
            if (currentSelection.size < 5) {
                currentSelection.add(topicId)
            } else {
                // Determine what to do if limit reached?
                // For now, simple logic: Do nothing or replace oldest?
                // User requirement: "up to 5", usually implies adding more is blocked.
                // We will just return to block adding the 6th.
                return
            }
        }

        _selectedTopics.value = currentSelection
        
        // RELOAD FROM SERVER with new filters
        loadPosts()
    }

    fun addFieldForPosts() {
        viewModelScope.launch {
            try {
                // Lấy trực tiếp danh sách post từ Firestore
                val posts = postRepository.getPosts(1000) // tăng limit nếu cần

                posts.forEach { post ->
                    val updated = post.copy(status = PostStatus.APPROVED)
                    postRepository.updatePost(updated)
                }
            } catch (e: Exception) {
                _error.value = "Failed to migrate post status: ${e.message}"
            }
        }
    }

    /**
     * Handles post actions such as voting.
     *
     * @param post The post being acted upon
     * @param postAction The action type (UPVOTE, DOWNVOTE, etc.)
     */
    fun onPostAction(post: Post, postAction: PostAction) {
        when (postAction) {
            PostAction.UPVOTE -> handleVote(post, isUpvote = true)
            PostAction.DOWNVOTE -> handleVote(post, isUpvote = false)
            PostAction.SUMMARY -> requestSummary(post.id)
            else -> Unit
        }
    }

    /**
     * Requests an AI-generated summary for a post.
     * Prevents duplicate requests for the same post.
     *
     * @param postId The ID of the post to summarize
     */
    fun requestSummary(postId: String) {
        // Prevent duplicate requests
        if (_summaryLoadingPostId.value == postId) return

        viewModelScope.launch {
            _summaryLoadingPostId.value = postId
            try {
                val result = postRepository.getPostSummary(postId)
                _summaryResult.value = postId to result
            } catch (e: Exception) {
                _summaryResult.value = postId to Result.failure(e)
            } finally {
                _summaryLoadingPostId.value = null
            }
        }
    }

    /**
     * Clears the summary result after it has been handled by the UI.
     */
    fun clearSummaryResult() {
        _summaryResult.value = null
    }

    /**
     * Processes voting logic for a post using optimistic UI updates.
     * Updates UI immediately, then persists to server. Rolls back on error.
     *
     * @param post The post to vote on
     * @param isUpvote True for upvote, false for downvote
     */
    private fun handleVote(post: Post, isUpvote: Boolean) {
        viewModelScope.launch {
            // Calculate optimistic update
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
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
            val currentList = _posts.value ?: emptyList()
            _posts.value = currentList.map { p -> if (p.id == post.id) optimisticPost else p }

            try {
                // Perform actual vote action on repository
                val actualUpdatedPost =
                        if (isUpvote) {
                            postRepository.toggleUpvote(post)
                        } else {
                            postRepository.toggleDownvote(post)
                        }

                // Update with actual server response
                val finalList = _posts.value ?: emptyList()
                _posts.value = finalList.map { p -> if (p.id == post.id) actualUpdatedPost else p }
            } catch (e: Exception) {
                // Rollback optimistic update on error
                val rollbackList = _posts.value ?: emptyList()
                _posts.value = rollbackList.map { p -> if (p.id == post.id) post else p }
                _error.value = e.message
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

                // Update posts list with the updated post
                val currentList = _posts.value ?: emptyList()
                _posts.value = currentList.map { p -> if (p.id == post.id) updatedPost else p }

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
