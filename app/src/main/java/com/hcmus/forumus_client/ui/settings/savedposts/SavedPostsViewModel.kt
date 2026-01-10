package com.hcmus.forumus_client.ui.settings.savedposts

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.model.User
import com.hcmus.forumus_client.data.model.Report
import com.hcmus.forumus_client.data.model.Violation
import com.hcmus.forumus_client.data.repository.PostRepository
import com.hcmus.forumus_client.data.repository.UserRepository
import com.hcmus.forumus_client.data.repository.ReportRepository
import com.google.firebase.auth.FirebaseAuth
import com.hcmus.forumus_client.data.model.PostAction
import kotlinx.coroutines.launch

/**
 * ViewModel for the Saved Posts screen.
 * Manages loading saved posts from the user's followedPostIds list and handles unsaving posts.
 */
class SavedPostsViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val postRepository: PostRepository = PostRepository(),
    private val reportRepository: ReportRepository = ReportRepository()
) : ViewModel() {

    // List of saved posts
    private val _savedPosts = MutableLiveData<List<Post>>(emptyList())
    val savedPosts: LiveData<List<Post>> = _savedPosts

    // Current user
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Error message
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    /**
     * Load the current user and their saved posts.
     */
    fun loadSavedPosts() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Load current user
                val user = userRepository.getCurrentUser()
                _currentUser.value = user

                if (user == null) {
                    _error.value = "User not found"
                    _savedPosts.value = emptyList()
                    return@launch
                }

                // Load posts by IDs from followedPostIds
                if (user.followedPostIds.isEmpty()) {
                    _savedPosts.value = emptyList()
                } else {
                    val posts = postRepository.getPostsByIds(user.followedPostIds)
                    // Sort posts by creation date (most recent first)
                    _savedPosts.value = posts.sortedByDescending { it.createdAt }
                }

            } catch (e: Exception) {
                Log.e("SavedPostsViewModel", "Error loading saved posts", e)
                _error.value = "Failed to load saved posts: ${e.message}"
                _savedPosts.value = emptyList()
            } finally {
                _isLoading.value = false
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
     * Processes voting logic for a post. Updates the post via repository and refreshes the posts
     * list.
     *
     * @param post The post to vote on
     * @param isUpvote True for upvote, false for downvote
     */
    private fun handleVote(post: Post, isUpvote: Boolean) {
        viewModelScope.launch {
            try {
                // Perform vote action on repository
                val updatedPost =
                    if (isUpvote) {
                        postRepository.toggleUpvote(post)
                    } else {
                        postRepository.toggleDownvote(post)
                    }

                // Update posts list with the updated post
                val currentList = _savedPosts.value ?: emptyList()
                _savedPosts.value = currentList.map { p -> if (p.id == post.id) updatedPost else p }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    /**
     * Unsave a post (remove from user's followedPostIds).
     *
     * @param post The post to unsave
     */
    fun unsavePost(post: Post) {
        viewModelScope.launch {
            try {
                val user = _currentUser.value ?: return@launch

                // Remove post ID from followedPostIds
                val updatedFollowedPostIds = user.followedPostIds.toMutableList()
                updatedFollowedPostIds.remove(post.id)

                // Update user in Firestore
                val updatedUser = user.copy(followedPostIds = updatedFollowedPostIds)
                userRepository.saveUser(updatedUser)

                // Update local state
                _currentUser.value = updatedUser
                _savedPosts.value = _savedPosts.value?.filter { it.id != post.id }

                Log.i("SavedPostsViewModel", "Post unsaved: ${post.id}")
            } catch (e: Exception) {
                Log.e("SavedPostsViewModel", "Error unsaving post", e)
                _error.value = "Failed to unsave post: ${e.message}"
            }
        }
    }

    /**
     * Saves a report for a post when user selects a violation.
     *
     * @param post The post being reported
     * @param violation The violation category selected by the user
     */
    fun saveReport(post: Post, violation: Violation) {
        viewModelScope.launch {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

                // Check if user has already reported this post
                if (post.reportedUsers.contains(userId)) {
                    _error.value = "You have already reported this post"
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

                // Update post: increment reportCount and add userId to reportedUsers
                val updatedPost = post.copy(
                    reportCount = post.reportCount + 1,
                    reportedUsers = post.reportedUsers.toMutableList().apply { add(userId) }
                )

                // Update post in Firebase
                postRepository.updatePost(updatedPost)

                // Update local saved posts list
                val currentPosts = _savedPosts.value ?: emptyList()
                _savedPosts.value = currentPosts.map { p -> if (p.id == post.id) updatedPost else p }

                Log.i("SavedPostsViewModel", "Post reported successfully")
            } catch (e: Exception) {
                Log.e("SavedPostsViewModel", "Error reporting post", e)
                _error.value = "Failed to report post: ${e.message}"
            }
        }
    }
}
