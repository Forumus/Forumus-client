package com.hcmus.forumus_client.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hcmus.forumus_client.data.model.User
import com.hcmus.forumus_client.data.repository.UserRepository
import com.hcmus.forumus_client.data.repository.PostRepository
import com.hcmus.forumus_client.data.repository.CommentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Log

data class EditProfileUiState(
    val user: User? = null,
    val isSaving: Boolean = false,
    val avatarPreviewUri: Uri? = null,
    val error: String? = null
)

class EditProfileViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val postRepository: PostRepository = PostRepository(),
    private val commentRepository: CommentRepository = CommentRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    /** Gọi khi Fragment nhận user từ MainSharedViewModel */
    fun setUser(user: User?) {
        _uiState.update { it.copy(user = user) }
    }

    fun onPickAvatar(uri: Uri) {
        _uiState.update { it.copy(avatarPreviewUri = uri, error = null) }
    }

    /**
     * Save và trả về updatedUser để Fragment cập nhật MainSharedViewModel
     */
    fun saveProfile(newFullName: String, onSuccess: (updatedUser: User) -> Unit) {
        val user = _uiState.value.user ?: run {
            _uiState.update { it.copy(error = "No user to save") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            try {
                val pickedUri = _uiState.value.avatarPreviewUri
                var finalAvatarUrl: String? = user.profilePictureUrl

                // 1) Upload avatar nếu có
                if (pickedUri != null) {
                    val downloadUrl = userRepository.uploadAvatar(user.uid, pickedUri)
                    val urlForUi = "$downloadUrl?ts=${System.currentTimeMillis()}"

                    userRepository.updateProfile(user.uid, profilePictureUrl = urlForUi)
                    finalAvatarUrl = urlForUi

                    _uiState.update { st ->
                        st.copy(
                            avatarPreviewUri = null,
                            user = st.user?.copy(profilePictureUrl = urlForUi)
                        )
                    }
                }

                // 2) Update full name
                userRepository.updateProfile(user.uid, fullName = newFullName)
                _uiState.update { st -> st.copy(user = st.user?.copy(fullName = newFullName)) }

                // 3) Sync posts/comments (có thể cần index)
                postRepository.updateAuthorInfoInPosts(user.uid, newFullName, finalAvatarUrl)
                commentRepository.updateAuthorInfoInComments(user.uid, newFullName, finalAvatarUrl)

                // optional
                // commentRepository.updateReplyToUserName(user.uid, newFullName)

                val updated = _uiState.value.user ?: user.copy(
                    fullName = newFullName,
                    profilePictureUrl = finalAvatarUrl
                )

                _uiState.update { it.copy(isSaving = false) }
                onSuccess(updated)
            } catch (e: Exception) {
                Log.e("EditProfileVM", "saveProfile failed", e)
                _uiState.update { it.copy(isSaving = false, error = e.message ?: "Failed to save profile") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
