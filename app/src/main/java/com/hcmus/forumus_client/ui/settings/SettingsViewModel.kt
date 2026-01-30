package com.hcmus.forumus_client.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hcmus.forumus_client.data.local.PreferencesManager
import com.hcmus.forumus_client.data.model.User
import com.hcmus.forumus_client.data.repository.AuthRepository
import com.hcmus.forumus_client.data.repository.UserRepository
import kotlinx.coroutines.launch

/**
 * Manages settings screen state and user preferences.
 * Handles loading/saving preferences (dark mode, notifications, language) to SharedPreferences.
 */
class SettingsViewModel(
    application: Application,
    private val userRepository: UserRepository = UserRepository(),
    private val preferencesManager: PreferencesManager = PreferencesManager(application),
    private val authRepository: AuthRepository = AuthRepository(context = application)
) : AndroidViewModel(application) {

    // Current user profile information
    private val _user = MutableLiveData<User>()
    val user: LiveData<User> = _user

    // Dark mode preference state
    private val _isDarkModeEnabled = MutableLiveData<Boolean>(false)
    val isDarkModeEnabled: LiveData<Boolean> = _isDarkModeEnabled

    // Push notifications preference state
    private val _isPushNotificationsEnabled = MutableLiveData<Boolean>(true)
    val isPushNotificationsEnabled: LiveData<Boolean> = _isPushNotificationsEnabled

    // Email notifications preference state
    private val _isEmailNotificationsEnabled = MutableLiveData<Boolean>(true)
    val isEmailNotificationsEnabled: LiveData<Boolean> = _isEmailNotificationsEnabled

    // Language preference state
    private val _language = MutableLiveData<String>("English")
    val language: LiveData<String> = _language

    // Loading state indicator
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Error message (null if no error)
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Logout completed event
    private val _logoutCompleted = MutableLiveData<Boolean>()
    val logoutCompleted: LiveData<Boolean> = _logoutCompleted

    // Fetches current user from Firestore for profile header display
    fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val currentUser = userRepository.getCurrentUser()
                if (currentUser != null) {
                    _user.value = currentUser
                    _error.value = null
                } else {
                    _error.value = "Failed to load current user"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred while loading user"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Restores saved preferences from SharedPreferences
    fun loadSavedPreferences() {
        _isDarkModeEnabled.value = preferencesManager.isDarkModeEnabled
        _isPushNotificationsEnabled.value = preferencesManager.isPushNotificationsEnabled
        _isEmailNotificationsEnabled.value = preferencesManager.isEmailNotificationsEnabled
        _language.value = preferencesManager.language
    }

    fun saveDarkModePreference(isEnabled: Boolean) {
        _isDarkModeEnabled.value = isEnabled
        preferencesManager.isDarkModeEnabled = isEnabled
    }

    fun savePushNotificationsPreference(isEnabled: Boolean) {
        _isPushNotificationsEnabled.value = isEnabled
        preferencesManager.isPushNotificationsEnabled = isEnabled
    }

    fun saveEmailNotificationsPreference(isEnabled: Boolean) {
        _isEmailNotificationsEnabled.value = isEnabled
        preferencesManager.isEmailNotificationsEnabled = isEnabled
    }

    fun saveLanguagePreference(language: String) {
        _language.value = language
        preferencesManager.language = language
    }

    // Signals fragment to navigate to login
    fun logout() {
        _logoutCompleted.value = true
    }

    // Terminates Firestore listeners and signs out
    fun clearSession() {
        authRepository.logout()
    }
}