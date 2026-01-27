package com.hcmus.forumus_client.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "forumus_prefs"
        // Authentication preferences
        private const val KEY_FIRST_TIME = "is_first_time"
        private const val KEY_AUTO_LOGIN = "auto_login_enabled"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_SESSION_TIMEOUT_DAYS = "session_timeout_days"
        
        // Settings preferences
        private const val KEY_DARK_MODE_ENABLED = "dark_mode_enabled"
        private const val KEY_PUSH_NOTIFICATIONS_ENABLED = "push_notifications_enabled"
        private const val KEY_EMAIL_NOTIFICATIONS_ENABLED = "email_notifications_enabled"
        private const val KEY_LANGUAGE = "language"
        
        const val DEFAULT_SESSION_TIMEOUT_DAYS = 7
    }

    // ===== Authentication Preferences =====
    
    var isFirstTime: Boolean
        get() = sharedPreferences.getBoolean(KEY_FIRST_TIME, true)
        set(value) = sharedPreferences.edit { putBoolean(KEY_FIRST_TIME, value) }
    
    var isAutoLoginEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_AUTO_LOGIN, true)
        set(value) = sharedPreferences.edit { putBoolean(KEY_AUTO_LOGIN, value) }
    
    var rememberMe: Boolean
        get() = sharedPreferences.getBoolean(KEY_REMEMBER_ME, false)
        set(value) = sharedPreferences.edit { putBoolean(KEY_REMEMBER_ME, value) }
    
    var sessionTimeoutDays: Int
        get() = sharedPreferences.getInt(KEY_SESSION_TIMEOUT_DAYS, DEFAULT_SESSION_TIMEOUT_DAYS)
        set(value) = sharedPreferences.edit { putInt(KEY_SESSION_TIMEOUT_DAYS, value) }
    
    fun getSessionTimeoutMs(): Long {
        return sessionTimeoutDays * 24L * 60L * 60L * 1000L
    }

    // ===== Settings Preferences =====

    /**
     * Dark mode preference. Defaults to false (light mode).
     */
    var isDarkModeEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_DARK_MODE_ENABLED, false)
        set(value) = sharedPreferences.edit { putBoolean(KEY_DARK_MODE_ENABLED, value) }

    /**
     * Push notifications preference. Defaults to true (enabled).
     */
    var isPushNotificationsEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_PUSH_NOTIFICATIONS_ENABLED, true)
        set(value) = sharedPreferences.edit { putBoolean(KEY_PUSH_NOTIFICATIONS_ENABLED, value) }

    /**
     * Email notifications preference. Defaults to true (enabled).
     */
    var isEmailNotificationsEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_EMAIL_NOTIFICATIONS_ENABLED, true)
        set(value) = sharedPreferences.edit { putBoolean(KEY_EMAIL_NOTIFICATIONS_ENABLED, value) }

    /**
     * Language preference. Defaults to "English".
     */
    var language: String
        get() = sharedPreferences.getString(KEY_LANGUAGE, "English") ?: "English"
        set(value) = sharedPreferences.edit { putString(KEY_LANGUAGE, value) }
}