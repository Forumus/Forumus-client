package com.hcmus.forumus_client.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.prefs.Preferences

class PreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "forumus_prefs"
        private const val KEY_FIRST_TIME = "is_first_time"
        private const val KEY_AUTO_LOGIN = "auto_login_enabled"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_SESSION_TIMEOUT_DAYS = "session_timeout_days"
        
        const val DEFAULT_SESSION_TIMEOUT_DAYS = 7
    }

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
}