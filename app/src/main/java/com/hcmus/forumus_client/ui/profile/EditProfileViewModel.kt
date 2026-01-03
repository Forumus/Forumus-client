package com.hcmus.forumus_client.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hcmus.forumus_client.data.model.User
import com.hcmus.forumus_client.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val avatarPreviewUri: Uri? = null,      // ảnh mới chọn (chưa upload)
    val error: String? = null
)

class EditProfileViewModel(
    private val repo: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState(isLoading = true))
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    fun loadCurrentUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val user = repo.getCurrentUser()
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

                // 1) Nếu có ảnh mới thì upload + update url
                if (pickedUri != null) {
                    val downloadUrl = repo.uploadAvatar(user.uid, pickedUri)

                    // cache-bust nhẹ để Coil không dính ảnh cũ khi ghi đè cùng path
                    val urlForUi = "$downloadUrl?ts=${System.currentTimeMillis()}"

                    repo.updateProfile(user.uid, profilePictureUrl = urlForUi)

                    // update state user để UI phản ánh ngay
                    _uiState.update { st ->
                        st.copy(
                            user = st.user?.copy(profilePictureUrl = urlForUi),
                            avatarPreviewUri = null
                        )
                    }
                }

                // 2) Update full name
                repo.updateProfile(user.uid, fullName = newFullName)

                _uiState.update { it.copy(isSaving = false) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Failed to save profile") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}