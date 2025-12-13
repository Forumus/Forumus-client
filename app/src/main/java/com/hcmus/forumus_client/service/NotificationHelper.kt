package com.hcmus.forumus_client.service

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Helper class to prepare notification data.
 * Note: Actual FCM notification sending must be done from a server/Cloud Function.
 */
object NotificationHelper {
    
    private const val TAG = "NotificationHelper"
    
    /**
     * Get FCM token for a specific user.
     * This token is needed by your backend to send notifications.
     */
    suspend fun getUserFcmToken(userId: String): String? {
        return try {
            val userDoc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .await()
            
            userDoc.getString("fcmToken").also {
                Log.d(TAG, "Retrieved FCM token for user $userId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving FCM token for user $userId", e)
            null
        }
    }
    
    /**
     * Get sender information for notification.
     */
    suspend fun getSenderInfo(senderId: String): SenderInfo? {
        return try {
            val userDoc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(senderId)
                .get()
                .await()
            
            SenderInfo(
                fullName = userDoc.getString("fullName") ?: "Unknown User",
                email = userDoc.getString("email") ?: "",
                pictureUrl = userDoc.getString("pictureUrl") ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving sender info", e)
            null
        }
    }
    
    /**
     * Save current user's FCM token to Firestore.
     * Call this when the app starts or when the user logs in.
     */
    suspend fun saveCurrentUserFcmToken(token: String): Boolean {
        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .update("fcmToken", token)
                    .await()
                Log.d(TAG, "FCM token saved successfully")
                true
            } else {
                Log.w(TAG, "No authenticated user to save token")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving FCM token", e)
            false
        }
    }
}

data class SenderInfo(
    val fullName: String,
    val email: String,
    val pictureUrl: String
)
