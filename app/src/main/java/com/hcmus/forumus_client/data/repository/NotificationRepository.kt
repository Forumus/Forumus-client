package com.hcmus.forumus_client.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hcmus.forumus_client.data.model.Notification
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NotificationRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getNotificationsFlow(limit: Long = 50): kotlinx.coroutines.flow.Flow<List<Notification>> = kotlinx.coroutines.flow.callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            close()
            return@callbackFlow
        }

        val registration = firestore.collection("users")
            .document(userId)
            .collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val notifications = snapshot.toObjects(Notification::class.java)
                    trySend(notifications)
                }
            }

        awaitClose { registration.remove() }
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
