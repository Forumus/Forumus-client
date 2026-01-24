package com.hcmus.forumus_client.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ForumusFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "chat_notifications"
        private const val CHANNEL_NAME = "Chat Messages"
        private const val CHANNEL_DESCRIPTION = "Notifications for new chat messages"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "=====================================")
        Log.d(TAG, "NEW FCM TOKEN GENERATED!")
        Log.d(TAG, "Token: $token")
        Log.d(TAG, "=====================================")

        // Save token to Firestore for the current user
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId == null) {
            Log.w(TAG, "User not authenticated - cannot save FCM token")
            Log.w(TAG, "Token will be saved when user logs in")
            return
        }

        Log.d(TAG, "Saving token for user: $userId")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .update("fcmToken", token)
                    .await()
                Log.d(TAG, "✅ FCM token saved to Firestore successfully!")
                Log.d(TAG, "User: $userId")
                Log.d(TAG, "Token: $token")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to save FCM token to Firestore", e)
                Log.e(TAG, "Error: ${e.message}")
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        // Handle data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data: ${remoteMessage.data}")
            val type = remoteMessage.data["type"]

            if (type == "general_notification") {
                val targetId = remoteMessage.data["targetId"] ?: ""
                val title = remoteMessage.notification?.title ?: "New Notification"
                val body = remoteMessage.notification?.body ?: "You have a new update"

                showGeneralNotification(title, body, targetId)
                return
            }

            val senderName = remoteMessage.data["senderName"] ?: "New Message"
            val messageContent = remoteMessage.data["messageContent"] ?: ""
            val chatId = remoteMessage.data["chatId"] ?: ""
            val senderId = remoteMessage.data["senderId"] ?: ""
            val senderEmail = remoteMessage.data["senderEmail"] ?: ""
            val senderPictureUrl = remoteMessage.data["senderPictureUrl"] ?: ""

            // Don't show notification if we are currently chatting in this chat
            // This would require checking the current activity/fragment state
            // For now, we'll just show it

            showNotification(senderName, messageContent, chatId, senderId, senderEmail, senderPictureUrl)
            return
        }

        // Handle notification payload (if sent from Firebase Console)
        remoteMessage.notification?.let {
            Log.d(TAG, "Notification title: ${it.title}")
            Log.d(TAG, "Notification body: ${it.body}")
            showNotification(it.title ?: "New Message", it.body ?: "", "", "", "", "")
        }
    }

    private fun showNotification(
        title: String,
        message: String,
        chatId: String,
        senderId: String,
        senderEmail: String,
        senderPictureUrl: String
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("chatId", chatId)
            putExtra("senderName", title)
            putExtra("senderEmail", senderEmail)
            putExtra("senderPictureUrl", senderPictureUrl)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            chatId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // You'll need to create this icon
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(chatId.hashCode(), notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showGeneralNotification(title: String, messageBody: String, postId: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("postId", postId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}