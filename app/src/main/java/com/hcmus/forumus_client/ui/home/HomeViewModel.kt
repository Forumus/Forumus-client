package com.hcmus.forumus_client.ui.home

import android.widget.Toast
import androidx.lifecycle.*
import com.hcmus.forumus_client.data.repository.PostRepository
import com.hcmus.forumus_client.data.repository.UserRepository
import com.hcmus.forumus_client.data.repository.ReportRepository
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.model.PostStatus
import com.hcmus.forumus_client.data.model.PostAction
import com.hcmus.forumus_client.data.model.Report
import com.hcmus.forumus_client.data.model.Violation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.internal.platform.PlatformRegistry.applicationContext

/**
 * ViewModel for the Home screen.
 * Manages loading posts and users, handling voting interactions.
 */
class HomeViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val postRepository: PostRepository = PostRepository(),
    private val reportRepository: ReportRepository = ReportRepository()
) : ViewModel() {

    // List of posts for the home feed
    private val _posts = MutableLiveData<List<Post>>(emptyList())
    val posts: LiveData<List<Post>> = _posts

    // Loading state indicator
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error message for UI display
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // List of topics for the drawer
    private val _topics = MutableLiveData<List<com.hcmus.forumus_client.data.model.Topic>>(emptyList())
    val topics: LiveData<List<com.hcmus.forumus_client.data.model.Topic>> = _topics

    enum class SortOption {
        NONE, NEW, TRENDING
    }

    // Sorting state
    private val _sortOption = MutableLiveData(SortOption.NONE)
    val sortOption: LiveData<SortOption> = _sortOption

    // Selected topics for filtering
    private val _selectedTopics = MutableLiveData<Set<String>>(emptySet())
    val selectedTopics: LiveData<Set<String>> = _selectedTopics

    // Keep track of the original list to support un-sorting
    private var originalPosts: List<Post> = emptyList()

    /**
     * Fetches topics from Firestore.
     */
    fun loadTopics() {
        viewModelScope.launch {
            try {
                val snapshot = FirebaseFirestore.getInstance().collection("topics").get().await()
                val topicList = snapshot.toObjects(com.hcmus.forumus_client.data.model.Topic::class.java)
                _topics.value = topicList
            } catch (e: Exception) {
                // Handle error or use default topics if needed
                e.printStackTrace()
            }
        }
    }

    /**
     * Fetches posts from the repository and updates the posts LiveData.
     * Manages loading state and error handling.
     */
    fun loadPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = postRepository.getPosts(100)
                originalPosts = result // Save original order

                applyFilters()

                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Applie filtering logic.
     */
    private fun applyFilters() {
        val selected = _selectedTopics.value ?: emptySet()
        var filteredList = if (selected.isEmpty()) {
            originalPosts
        } else {
            originalPosts.filter { post ->
                // Post must have at least one topic in the selected set
                // Note: post.topicIds is List<String>, selected is Set<String>
                post.topicIds.any { it in selected }
            }
        }

        val sort = _sortOption.value ?: SortOption.NONE
        filteredList = when (sort) {
            SortOption.NEW -> filteredList.sortedByDescending { it.createdAt }
            SortOption.TRENDING -> filteredList.sortedByDescending {
                // Trending score = Total interactions (upvotes + downvotes + comments)
                it.upvoteCount + it.downvoteCount + it.commentCount
            }
            SortOption.NONE -> filteredList
        }

        _posts.value = filteredList
    }

    /**
     * Toggles the sort option.
     * If the same option is clicked, it toggles off to NONE.
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
        applyFilters()
    }

    /**
     * Toggles the selection of a topic for filtering.
     * Enforces a maximum of 5 selected topics.
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
        applyFilters()
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
            else -> Unit
        }
    }

    /**
     * Processes voting logic for a post.
     * Updates the post via repository and refreshes the posts list.
     *
     * @param post The post to vote on
     * @param isUpvote True for upvote, false for downvote
     */
    private fun handleVote(post: Post, isUpvote: Boolean) {
        viewModelScope.launch {
            try {
                // Perform vote action on repository
                val updatedPost = if (isUpvote) {
                    postRepository.toggleUpvote(post)
                } else {
                    postRepository.toggleDownvote(post)
                }

                // Update posts list with the updated post
                val currentList = _posts.value ?: emptyList()
                _posts.value = currentList.map { p ->
                    if (p.id == post.id) updatedPost else p
                }

            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    /**
     * Saves a report for a post when user selects a violation.
     * Creates a Report object, saves it to Firebase, increments reportCount,
     * and adds userId to reportedUsers list in the post.
     *
     * @param post The post being reported
     * @param violation The violation category selected by the user
     */
    fun saveReport(post: Post, violation: Violation) {
        viewModelScope.launch {
            try {
                val currentUser = userRepository.getCurrentUser()
                val userId = currentUser?.uid ?: FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

                // Check if user has already reported this post
                if (post.reportedUsers.contains(userId)) {
                    Toast.makeText(applicationContext, "You have already reported this post", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Create report object
                val report = Report(
                    postId = post.id,
                    authorId = userId,
                    nameViolation = violation.name,
                    descriptionViolation = violation
                )

                // Save report to Firebase
                reportRepository.saveReport(report)

                Toast.makeText(applicationContext, "Post reported", Toast.LENGTH_SHORT).show()

                // Update post: increment reportCount and add userId to reportedUsers
                val updatedPost = post.copy(
                    reportCount = post.reportCount + 1,
                    reportedUsers = post.reportedUsers.toMutableList().apply { add(userId) }
                )

                // Update post in Firebase via repository
                postRepository.updatePost(updatedPost)

                // Update posts list with the updated post
                val currentList = _posts.value ?: emptyList()
                _posts.value = currentList.map { p ->
                    if (p.id == post.id) updatedPost else p
                }

                _error.value = null

            } catch (e: Exception) {
                _error.value = "Failed to report post: ${e.message}"
            }
        }
    }
}
