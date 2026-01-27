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
 * ViewModel for managing settings screen state and user preferences.
 *
 * Responsibilities:
 * - Load and display current user profile information
 * - Manage preference toggles (dark mode, push notifications, email notifications)
 * - Persist user preferences to SharedPreferences using PreferencesManager
 * - Restore saved preferences when activity is created
 * - Provide observable LiveData for preference state
 *
 * All preference changes are immediately saved to persistent storage,
 * ensuring that user preferences are retained across app sessions.
 *
 * @param application The application context for PreferencesManager
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

    // Loading state indicator
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Error message (null if no error)
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Logout completed event
    private val _logoutCompleted = MutableLiveData<Boolean>()
    val logoutCompleted: LiveData<Boolean> = _logoutCompleted

    /**
     * Load current user profile information from Firestore.
     * This populates the user profile header card with name, email, and avatar.
     */
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

    /**
     * Load all saved user preferences from SharedPreferences.
     * Called when activity is created to restore user's previous settings.
     */
    fun loadSavedPreferences() {
        _isDarkModeEnabled.value = preferencesManager.isDarkModeEnabled
        _isPushNotificationsEnabled.value = preferencesManager.isPushNotificationsEnabled
        _isEmailNotificationsEnabled.value = preferencesManager.isEmailNotificationsEnabled
    }

    /**
     * Save dark mode preference to SharedPreferences.
     * Immediately updates persistent storage.
     *
     * @param isEnabled True to enable dark mode, false to disable
     */
    fun saveDarkModePreference(isEnabled: Boolean) {
        _isDarkModeEnabled.value = isEnabled
        preferencesManager.isDarkModeEnabled = isEnabled
    }

    /**
     * Save push notifications preference to SharedPreferences.
     * Immediately updates persistent storage.
     *
     * @param isEnabled True to enable push notifications, false to disable
     */
    fun savePushNotificationsPreference(isEnabled: Boolean) {
        _isPushNotificationsEnabled.value = isEnabled
        preferencesManager.isPushNotificationsEnabled = isEnabled
    }

    /**
     * Save email notifications preference to SharedPreferences.
     * Immediately updates persistent storage.
     *
     * @param isEnabled True to enable email notifications, false to disable
     */
    fun saveEmailNotificationsPreference(isEnabled: Boolean) {
        _isEmailNotificationsEnabled.value = isEnabled
        preferencesManager.isEmailNotificationsEnabled = isEnabled
    }

    /**
     * Perform user logout.
     * Signals the fragment to navigate to login screen.
     */
    fun logout() {
        _logoutCompleted.value = true
    }

    /**
     * Clear session data and sign out.
     * Terminates Firestore to cancel all active listeners before signing out.
     */
    fun clearSession() {
        authRepository.logout()
    }
}