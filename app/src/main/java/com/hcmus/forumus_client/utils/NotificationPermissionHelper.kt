package com.hcmus.forumus_client.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging

object NotificationPermissionHelper {
    
    private const val TAG = "NotificationPermission"
    private const val PERMISSION_REQUEST_CODE = 1001
    
    private var permissionCallback: ((Boolean) -> Unit)? = null
    
    /**
     * Check if notification permission is granted.
     */
    fun hasPermission(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Notifications are allowed by default on Android 12 and below
            true
        }
    }
    
    /**
     * Request notification permission if needed (Android 13+).
     * For Android 12 and below, this will immediately call the callback with true.
     * 
     * @param activity The activity to request permission from
     * @param callback Called with true if permission is granted, false otherwise
     */
    fun requestPermissionIfNeeded(
        activity: Activity,
        callback: (Boolean) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                hasPermission(activity) -> {
                    Log.d(TAG, "Notification permission already granted")
                    callback(true)
                    initializeFCM()
                }
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) -> {
                    Log.d(TAG, "Should show permission rationale")
                    // You can show an explanation dialog here
                    permissionCallback = callback
                    requestPermission(activity)
                }
                else -> {
                    Log.d(TAG, "Requesting notification permission")
                    permissionCallback = callback
                    requestPermission(activity)
                }
            }
        } else {
            // Android 12 and below - notifications are allowed by default
            Log.d(TAG, "Android 12 or below - notification permission not needed")
            callback(true)
            initializeFCM()
        }
    }
    
    /**
     * Request the notification permission.
     */
    private fun requestPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                PERMISSION_REQUEST_CODE
            )
        }
    }
    
    /**
     * Handle the permission request result.
     * Call this from your Activity's onRequestPermissionsResult().
     */
    fun handlePermissionResult(
        requestCode: Int,
        grantResults: IntArray,
        callback: ((Boolean) -> Unit)? = null
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val granted = grantResults.isNotEmpty() && 
                         grantResults[0] == PackageManager.PERMISSION_GRANTED
            
            if (granted) {
                Log.d(TAG, "Notification permission granted")
                initializeFCM()
            } else {
                Log.d(TAG, "Notification permission denied")
            }
            
            // Call the stored callback
            permissionCallback?.invoke(granted)
            permissionCallback = null
            
            // Call the provided callback
            callback?.invoke(granted)
        }
    }
    
    /**
     * Initialize FCM and get the token.
     */
    private fun initializeFCM() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener {
                if (!it.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", it.exception)
                    return@addOnCompleteListener
                }

                // Get new FCM registration token
                val token = it.result
                Log.d(TAG, "FCM Token: $token")
            }
    }
    
    /**
     * Force refresh the FCM token.
     * Use this after login or when you suspect the token might be stale.
     */
    fun refreshFCMToken() {
        FirebaseMessaging.getInstance().deleteToken()
            .addOnSuccessListener {
                Log.d(TAG, "Old token deleted, getting new token...")
                FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { newToken ->
                        Log.d(TAG, "New FCM Token: $newToken")
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to refresh FCM token", e)
            }
    }
}
