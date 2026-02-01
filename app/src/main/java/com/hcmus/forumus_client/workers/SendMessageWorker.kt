package com.hcmus.forumus_client.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.data.model.MessageType
import com.hcmus.forumus_client.data.repository.ChatRepository

/** Background worker for sending messages with images. */
class SendMessageWorker(context: Context, params: WorkerParameters) :
        CoroutineWorker(context, params) {

    private val chatRepository = ChatRepository()
    private val gson = Gson()

    companion object {
        private const val TAG = "SendMessageWorker"
        const val CHANNEL_ID = "message_upload_channel"
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_ID_SUCCESS = 1002
        const val NOTIFICATION_ID_FAILURE = 1003

        // Input data keys
        const val KEY_CHAT_ID = "chat_id"
        const val KEY_MESSAGE_CONTENT = "message_content"
        const val KEY_MESSAGE_TYPE = "message_type"
        const val KEY_IMAGE_URLS = "image_urls"

        // Output data keys
        const val KEY_RESULT_SUCCESS = "result_success"
        const val KEY_RESULT_MESSAGE_ID = "result_message_id"
        const val KEY_RESULT_ERROR = "result_error"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting SendMessageWorker")

        // Extract work parameters
        val chatId = inputData.getString(KEY_CHAT_ID)
        val content = inputData.getString(KEY_MESSAGE_CONTENT) ?: ""
        val messageTypeStr = inputData.getString(KEY_MESSAGE_TYPE) ?: MessageType.TEXT.name
        val imageUrlsJson = inputData.getString(KEY_IMAGE_URLS)

        if (chatId == null) {
            Log.e(TAG, "Chat ID is null")
            return Result.failure(
                    workDataOf(KEY_RESULT_SUCCESS to false, KEY_RESULT_ERROR to "Invalid chat ID")
            )
        }

        // Parse image URLs
        val imageUrls: MutableList<String> =
                if (imageUrlsJson != null) {
                    try {
                        val type = object : TypeToken<MutableList<String>>() {}.type
                        gson.fromJson(imageUrlsJson, type)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse image URLs", e)
                        mutableListOf()
                    }
                } else {
                    mutableListOf()
                }

        // Parse message type
        val messageType =
                try {
                    MessageType.valueOf(messageTypeStr)
                } catch (e: Exception) {
                    MessageType.TEXT
                }

        try {
            // Show notification for long-running work
            if (imageUrls.isNotEmpty()) {
                setForeground(createForegroundInfo(imageUrls.size))
            }

            Log.d(TAG, "Sending message with ${imageUrls.size} images")

            // Send the message (this will upload images internally)
            val result = chatRepository.sendMessage(chatId, content, messageType, imageUrls)

            return if (result.isSuccess) {
                val messageId = result.getOrNull() ?: ""
                Log.d(TAG, "Message sent successfully: $messageId")
                
                // Show success notification
                showCompletionNotification(
                    success = true,
                    imageCount = imageUrls.size
                )
                
                Result.success(
                        workDataOf(KEY_RESULT_SUCCESS to true, KEY_RESULT_MESSAGE_ID to messageId)
                )
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.e(TAG, "Failed to send message: $error")
                
                // Show failure notification
                showCompletionNotification(
                    success = false,
                    imageCount = imageUrls.size,
                    errorMessage = error
                )
                
                Result.failure(workDataOf(KEY_RESULT_SUCCESS to false, KEY_RESULT_ERROR to error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in SendMessageWorker", e)
            
            // Show failure notification
            showCompletionNotification(
                success = false,
                imageCount = imageUrls.size,
                errorMessage = e.message ?: "Unknown error"
            )
            
            return Result.failure(
                    workDataOf(
                            KEY_RESULT_SUCCESS to false,
                            KEY_RESULT_ERROR to (e.message ?: "Unknown error")
                    )
            )
        }
    }

    /** Creates foreground notification. */
    private fun createForegroundInfo(imageCount: Int): ForegroundInfo {
        createNotificationChannel()

        val notification =
                NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                        .setContentTitle("Sending message")
                        .setContentText(
                                "Uploading $imageCount ${if (imageCount == 1) "image" else "images"}..."
                        )
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setOngoing(true)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID, 
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    /** Creates notification channel. */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                    NotificationChannel(
                                    CHANNEL_ID,
                                    "Message Uploads",
                                    NotificationManager.IMPORTANCE_LOW
                            )
                            .apply { description = "Notifications for message and image uploads" }

            val notificationManager =
                    applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as
                            NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /** Shows completion notification. */
    private fun showCompletionNotification(
        success: Boolean,
        imageCount: Int,
        errorMessage: String? = null
    ) {
        createNotificationChannel()
        
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Cancel the foreground "uploading" notification
        notificationManager.cancel(NOTIFICATION_ID)
        
        val notification = if (success) {
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setContentTitle("Message sent! ✓")
                .setContentText(
                    if (imageCount > 0) {
                        "$imageCount ${if (imageCount == 1) "image" else "images"} uploaded successfully"
                    } else {
                        "Your message was sent successfully"
                    }
                )
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true) // Dismiss when tapped
                .build()
        } else {
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setContentTitle("Failed to send message")
                .setContentText(errorMessage ?: "An error occurred while uploading")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
        }
        
        val notificationId = if (success) NOTIFICATION_ID_SUCCESS else NOTIFICATION_ID_FAILURE
        notificationManager.notify(notificationId, notification)
    }
}
