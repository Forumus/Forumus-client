package com.hcmus.forumus_client.ui.settings.savedposts

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.model.User
import com.hcmus.forumus_client.data.repository.PostRepository
import com.hcmus.forumus_client.data.repository.UserRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for the Saved Posts screen.
 * Manages loading saved posts from the user's followedPostIds list and handles unsaving posts.
 */
class SavedPostsViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val postRepository: PostRepository = PostRepository()
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
}
