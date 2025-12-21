package com.hcmus.forumus_client.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hcmus.forumus_client.data.model.Notification
import kotlinx.coroutines.tasks.await

class NotificationRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun getNotifications(limit: Long = 50): List<Notification> {
        val userId = auth.currentUser?.uid ?: return emptyList()

        return firestore.collection("users")
            .document(userId)
            .collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
            .toObjects(Notification::class.java)
    }

    suspend fun markAsRead(notificationId: String) {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(userId)
            .collection("notifications")
            .document(notificationId)
            .update("isRead", true)
            .await()
    }
}
