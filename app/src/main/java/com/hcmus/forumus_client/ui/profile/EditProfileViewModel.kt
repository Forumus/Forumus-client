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
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val avatarPreviewUri: Uri? = null,      // ảnh mới chọn (chưa upload)
    val error: String? = null
)

class EditProfileViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val postRepository: PostRepository = PostRepository(),
    private val commentRepository: CommentRepository = CommentRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState(isLoading = true))
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    fun loadCurrentUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val user = userRepository.getCurrentUser()
                _uiState.update { it.copy(user = user, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Unable to load user") }
            }
        }
    }

    fun onPickAvatar(uri: Uri) {
        _uiState.update { it.copy(avatarPreviewUri = uri, error = null) }
    }

    /**
     * Save sẽ xử lý cả fullName và avatar (nếu có chọn ảnh).
     * - Nếu có avatarPreviewUri: upload -> lấy url -> updateProfile(profilePictureUrl)
     * - Sau đó updateProfile(fullName)
     */
    fun saveProfile(newFullName: String, onSuccess: () -> Unit) {
        val user = _uiState.value.user ?: run {
            _uiState.update { it.copy(error = "No user to save") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            try {
                val pickedUri = _uiState.value.avatarPreviewUri

                // avatarUrl cuối cùng sẽ dùng để sync vào posts/comments
                var finalAvatarUrl: String? = user.profilePictureUrl

                // 1) Nếu có ảnh mới -> upload -> update profilePictureUrl
                if (pickedUri != null) {
                    val downloadUrl = userRepository.uploadAvatar(user.uid, pickedUri)

                    // cache-bust để client (Coil) không dính ảnh cũ
                    val urlForUi = "$downloadUrl?ts=${System.currentTimeMillis()}"

                    userRepository.updateProfile(user.uid, profilePictureUrl = urlForUi)
                    finalAvatarUrl = urlForUi

                    // update state user để UI phản ánh ngay
                    _uiState.update { st ->
                        st.copy(
                            user = st.user?.copy(profilePictureUrl = urlForUi),
                            avatarPreviewUri = null
                        )
                    }
                }

                // 2) Update full name trong users
                userRepository.updateProfile(user.uid, fullName = newFullName)

                // update state user (để UI hiện tên mới ngay)
                _uiState.update { st ->
                    st.copy(user = st.user?.copy(fullName = newFullName))
                }

                // 3) Sync toàn bộ posts/comments (denormalized fields)
                // Post: posts where authorId == uid
                postRepository.updateAuthorInfoInPosts(
                    userId = user.uid,
                    newName = newFullName,
                    newAvatarUrl = finalAvatarUrl
                )

                // Comment: collectionGroup("comments") where authorId == uid
                commentRepository.updateAuthorInfoInComments(
                    userId = user.uid,
                    newName = newFullName,
                    newAvatarUrl = finalAvatarUrl
                )

                commentRepository.updateReplyToUserName(user.uid, newFullName)

                _uiState.update { it.copy(isSaving = false) }
                onSuccess()
            }  catch (e: Exception) {
                Log.e("EditProfileVM", "saveProfile failed", e)
                _uiState.update { it.copy(isSaving = false, error = e.message ?: "Failed to save profile") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}