package com.hcmus.forumus_client.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PreferenceManager(context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "forumus_prefs"
        private const val KEY_FIRST_TIME = "is_first_time"
    }
    
    var isFirstTime: Boolean
        get() = sharedPreferences.getBoolean(KEY_FIRST_TIME, true)
        set(value) = sharedPreferences.edit { putBoolean(KEY_FIRST_TIME, value) }
}