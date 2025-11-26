package com.hcmus.forumus_client.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class TokenManager(context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "forumus_token_prefs"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_FULL_NAME = "user_full_name"
        private const val KEY_ROLE = "user_role"
        private const val KEY_EMAIL_VERIFIED = "email_verified"
        private const val KEY_LOGIN_TIME = "login_time"
        private const val KEY_SESSION_DURATION = "session_duration"
        
        // Default session duration: 7 days in milliseconds
        private const val DEFAULT_SESSION_DURATION = 7L * 24L * 60L * 60L * 1000L
    }
    
    fun saveUserSession(
        userId: String,
        email: String,
        fullName: String,
        role: String,
        emailVerified: Boolean,
        sessionDurationMs: Long = DEFAULT_SESSION_DURATION
    ) {
        val loginTime = System.currentTimeMillis()
        sharedPreferences.edit {
            putString(KEY_USER_ID, userId)
            putString(KEY_EMAIL, email)
            putString(KEY_FULL_NAME, fullName)
            putString(KEY_ROLE, role)
            putBoolean(KEY_EMAIL_VERIFIED, emailVerified)
            putLong(KEY_LOGIN_TIME, loginTime)
            putLong(KEY_SESSION_DURATION, sessionDurationMs)
        }
    }
    
    fun isSessionValid(): Boolean {
        val loginTime = sharedPreferences.getLong(KEY_LOGIN_TIME, 0L)
        val sessionDuration = sharedPreferences.getLong(KEY_SESSION_DURATION, DEFAULT_SESSION_DURATION)
        
        return if (loginTime == 0L) {
            false
        } else {
            val currentTime = System.currentTimeMillis()
            val sessionExpiry = loginTime + sessionDuration
            currentTime < sessionExpiry
        }
    }
    
    fun getUserId(): String? = sharedPreferences.getString(KEY_USER_ID, null)
    
    fun getEmail(): String? = sharedPreferences.getString(KEY_EMAIL, null)
    
    fun getFullName(): String? = sharedPreferences.getString(KEY_FULL_NAME, null)
    
    fun getRole(): String? = sharedPreferences.getString(KEY_ROLE, null)
    
    fun isEmailVerified(): Boolean = sharedPreferences.getBoolean(KEY_EMAIL_VERIFIED, false)
    
    fun getSessionExpiryTime(): Long {
        val loginTime = sharedPreferences.getLong(KEY_LOGIN_TIME, 0L)
        val sessionDuration = sharedPreferences.getLong(KEY_SESSION_DURATION, DEFAULT_SESSION_DURATION)
        return loginTime + sessionDuration
    }
    
    fun getRemainingSessionTime(): Long {
        if (!isSessionValid()) return 0L
        return getSessionExpiryTime() - System.currentTimeMillis()
    }
    
    fun clearSession() {
        sharedPreferences.edit {
            remove(KEY_USER_ID)
            remove(KEY_EMAIL)
            remove(KEY_FULL_NAME)
            remove(KEY_ROLE)
            remove(KEY_EMAIL_VERIFIED)
            remove(KEY_LOGIN_TIME)
            remove(KEY_SESSION_DURATION)
        }
    }
    
    fun hasValidSession(): Boolean {
        return getUserId() != null && isSessionValid()
    }
}