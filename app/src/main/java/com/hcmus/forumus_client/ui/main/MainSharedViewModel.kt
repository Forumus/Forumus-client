package com.hcmus.forumus_client.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.hcmus.forumus_client.data.model.User
import com.hcmus.forumus_client.data.repository.UserRepository
import com.hcmus.forumus_client.ui.home.HomeViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.hcmus.forumus_client.data.model.Topic

/** Shared ViewModel for MainActivity and its Fragments. */
class MainSharedViewModel(
    private val userRepository: UserRepository = UserRepository(),
) : ViewModel() {

    // Current authenticated user
    private val _currentUser = MutableLiveData<User?>(null)
    val currentUser: LiveData<User?> = _currentUser

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Error messages
    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    /** Loads the currently authenticated user. */
    fun loadCurrentUser() {
        if (_currentUser.value != null) {
            // User already loaded, skip
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val user = userRepository.getCurrentUser()
                _currentUser.value = user
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to load user: ${e.message}"
                _currentUser.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Refreshes user data from Firestore. */
    fun refreshCurrentUser() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                _currentUser.value = userRepository.getCurrentUser()
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to refresh user: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Directly sets the current user without Firestore call. */
    fun setCurrentUser(user: User?) {
        _currentUser.value = user
    }

    // Note: media preview is now handled by MediaViewerViewModel in ui.media
}
