package com.hcmus.forumus_client.ui.post.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.model.User
import com.hcmus.forumus_client.data.repository.PostRepository
import com.hcmus.forumus_client.data.repository.UserRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import android.content.Context

/**
 * ViewModel for managing post creation functionality.
 * Handles user loading, media management, and post submission.
 */
class CreatePostViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val postRepository: PostRepository = PostRepository()
) : ViewModel() {

    // Current authenticated user
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    // Post title
    private val _postTitle = MutableLiveData<String>("")
    val postTitle: LiveData<String> = _postTitle

    // Post content
    private val _postContent = MutableLiveData<String>("")
    val postContent: LiveData<String> = _postContent

    // List of selected image URIs (local paths)
    private val _selectedImageUris = MutableLiveData<MutableList<String>>(mutableListOf())
    val selectedImageUris: LiveData<MutableList<String>> = _selectedImageUris

    // List of selected video URIs (local paths)
    private val _selectedVideoUris = MutableLiveData<MutableList<String>>(mutableListOf())
    val selectedVideoUris: LiveData<MutableList<String>> = _selectedVideoUris

    // Loading state indicator
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Success message
    private val _successMessage = MutableLiveData<String?>(null)
    val successMessage: LiveData<String?> = _successMessage

    // Error message for UI display
    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    /**
     * Loads the currently authenticated user from the repository.
     */
    fun loadUser(userId: String) {
        viewModelScope.launch {
            try {
                val user = userRepository.getUserById(userId)
                _currentUser.value = user
            } catch (e: Exception) {
                _error.value = "Failed to load user: ${e.message}"
                _currentUser.value = null
            }
        }
    }

    /**
     * Updates post title.
     *
     * @param title The new title text
     */
    fun setPostTitle(title: String) {
        _postTitle.value = title
    }

    /**
     * Updates post content.
     *
     * @param content The new content text
     */
    fun setPostContent(content: String) {
        _postContent.value = content
    }

    /**
     * Adds an image URI to the selected images list.
     *
     * @param imageUri The URI of the image to add
     */
    fun addImageUri(imageUri: String) {
        val currentList = _selectedImageUris.value ?: mutableListOf()
        currentList.add(imageUri)
        _selectedImageUris.value = currentList
    }

    /**
     * Removes an image URI from the selected images list.
     *
     * @param imageUri The URI to remove
     */
    fun removeImageUri(imageUri: String) {
        val currentList = _selectedImageUris.value ?: mutableListOf()
        currentList.remove(imageUri)
        _selectedImageUris.value = currentList
    }

    /**
     * Adds a video URI to the selected videos list.
     *
     * @param videoUri The URI of the video to add
     */
    fun addVideoUri(videoUri: String) {
        val currentList = _selectedVideoUris.value ?: mutableListOf()
        currentList.add(videoUri)
        _selectedVideoUris.value = currentList
    }

    /**
     * Removes a video URI from the selected videos list.
     *
     * @param videoUri The URI to remove
     */
    fun removeVideoUri(videoUri: String) {
        val currentList = _selectedVideoUris.value ?: mutableListOf()
        currentList.remove(videoUri)
        _selectedVideoUris.value = currentList
    }

    /**
     * Clears all selected media (images and videos).
     */
    fun clearAllMedia() {
        _selectedImageUris.value = mutableListOf()
        _selectedVideoUris.value = mutableListOf()
    }

    /**
     * Submits the post to the repository for saving.
     * Validates content, creates Post object, and calls PostRepository.savePost().
     */
    fun submitPost(context: Context) {
        // Validate input
        val content = _postContent.value?.trim() ?: ""
        if (content.isEmpty()) {
            _error.value = "Post content cannot be empty"
            return
        }

        val user = _currentUser.value
        if (user == null) {
            _error.value = "User not loaded, cannot create post"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                // Create post object
                val post = Post(
                    authorId = user.uid,
                    authorName = user.fullName,
                    authorAvatarUrl = user.profilePictureUrl,
                    createdAt = Timestamp.now(),
                    title = _postTitle.value ?: "",
                    content = content,
                    upvoteCount = 0,
                    downvoteCount = 0,
                    commentCount = 0,
                    reportCount = 0,
                    imageUrls = _selectedImageUris.value ?: mutableListOf(),
                    videoUrls = _selectedVideoUris.value ?: mutableListOf(),
                    videoThumbnailUrls = mutableListOf(),
                    votedUsers = mutableMapOf(),
                    reportedUsers = mutableListOf()
                )

                // Call repository to save post
                postRepository.savePost(context, post)

                // Clear form and show success
                _postTitle.value = ""
                _postContent.value = ""
                _selectedImageUris.value = mutableListOf()
                _selectedVideoUris.value = mutableListOf()
                _successMessage.value = "Post created successfully!"
                _error.value = null

            } catch (e: Exception) {
                _error.value = "Failed to create post: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}