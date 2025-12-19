package com.hcmus.forumus_client.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hcmus.forumus_client.data.model.Message
import com.hcmus.forumus_client.data.model.MessageType
import com.hcmus.forumus_client.ui.chats.ChatItem
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.*
import com.google.firebase.storage.FirebaseStorage
import androidx.core.net.toUri
import com.google.firebase.firestore.FieldValue
import com.hcmus.forumus_client.data.model.ChatType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

class ChatRepository {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val chatsCollection = firestore.collection("chats")
    private val userRepository = UserRepository()

    companion object {
        private const val TAG = "ChatRepository"
        private const val MESSAGES_SUBCOLLECTION = "messages"

        fun getCurrentUserId(): String? {
            return FirebaseAuth.getInstance().currentUser?.uid
        }
    }

    // Listen to all chats for current user
    fun getUserChatsFlow(chatType: Enum<ChatType>): Flow<List<ChatItem>> = callbackFlow {
        val currentUserId = getCurrentUserId()
        Log.d(TAG, "Current user ID: $currentUserId")

        // 1. Handle unauthenticated state by closing the flow with an exception
        if (currentUserId == null) {
            close(Exception("User not authenticated"))
            return@callbackFlow
        }

        val query = chatsCollection
            .whereArrayContains("userIds", currentUserId)

        val finalQuery = if (chatType == ChatType.UNREAD_CHATS) {
            query.whereNotEqualTo("unreadCount", 0)
        } else {
            query
        }

        val listener = finalQuery
            .orderBy("lastUpdate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to chats", error)
                    close(error) // Close the flow on error
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // We use 'launch' from the ProducerScope to keep the flow active
                    launch(Dispatchers.IO) {
                        val deferredChats = snapshot.documents.map { doc ->
                            async {
                                try {
                                    val chatItem = doc.toObject(ChatItem::class.java)?.copy(id = doc.id)

                                    // Do not load chats marked as deleted by current user
                                    if (chatItem?.chatDeleted?.get(currentUserId) == true) {
                                        return@async null
                                    }

                                    chatItem?.let { chat ->
                                        // Set UI properties
                                        chat.timestamp = formatTimestamp(chat.lastUpdate)
                                        chat.isUnread = chat.unreadCount > 0

                                        // Fetch Name Logic (Preserved from your original code)
                                        Log.d(TAG, chat.toString())
                                        val otherUserId = chat.userIds.firstOrNull { it != currentUserId }
                                        if (otherUserId != null) {
                                            try {
                                                val user = userRepository.getUserById(otherUserId)
                                                chat.contactName = user.fullName
                                                chat.profilePictureUrl = user.profilePictureUrl
                                                chat.email = user.email
                                                Log.d(TAG, "Fetched contact name: ${chat.contactName}")
                                            } catch (e: Exception) {
                                                chat.contactName = "Unknown"
                                                Log.e(TAG, "Failed to fetch user name", e)
                                            }
                                        } else {
                                            chat.contactName = "Me"
                                        }
                                        chat
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing chat item", e)
                                    null
                                }
                            }
                        }

                        // Wait for all parallel fetches to complete
                        val chats = deferredChats.awaitAll().filterNotNull()

                        // 4. Emit the result to the flow
                        trySend(chats)
                    }
                }
            }

        // 5. Cleanup: Remove listener when flow collection stops
        awaitClose {
            Log.d(TAG, "Removing chat listener")
            listener.remove()
        }
    }

    // Listen to messages in a specific chat with pagination
    fun getChatMessagesFlow(chatId: String, limit: Int = 25): Flow<List<Message>> = callbackFlow {
        val listener = chatsCollection
            .document(chatId)
            .collection(MESSAGES_SUBCOLLECTION)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limitToLast(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to messages", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // This conversion now happens on the IO thread (background)
                    // creating the objects here won't freeze your UI.
                    val messages = snapshot.toObjects(Message::class.java)
                    trySend(messages)
                }
            }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    // Load previous messages before a specific timestamp for pagination
    suspend fun loadPreviousMessages(
        chatId: String,
        beforeTimestamp: Long,
        limit: Int = 50
    ): Result<List<Message>> {
        return try {
            val com = Timestamp(beforeTimestamp / 1000, ((beforeTimestamp % 1000) * 1000000).toInt())
            
            val messages = chatsCollection
                .document(chatId)
                .collection(MESSAGES_SUBCOLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(com)
                .limit(limit.toLong())
                .get()
                .await()
                .toObjects(Message::class.java)
                .reversed() // Reverse to get ascending order
            
            Log.d(TAG, "Loaded ${messages.size} previous messages before timestamp $beforeTimestamp")
            Result.success(messages)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading previous messages", e)
            Result.failure(e)
        }
    }

    suspend fun uploadChatImage(imageUri: String, chatId: String, fileName: String): Result<String> {
        return try {
            Log.d(TAG, "Starting image upload: $fileName")
            
            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("chat_images/$chatId/$fileName")
            
            // Convert URI and upload
            val uri = imageUri.toUri()
            imageRef.putFile(uri).await()
            
            Log.d(TAG, "Upload completed, getting download URL: $fileName")
            val downloadUrl = imageRef.downloadUrl.await().toString()
            
            Log.d(TAG, "Image upload successful: $fileName -> $downloadUrl")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Image upload failed for $fileName: ${e.message}", e)
            Result.failure(Exception("Upload failed: ${e.message}"))
        }
    }
    
    // Send a message
    suspend fun sendMessage(
        chatId: String,
        content: String,
        type: MessageType = MessageType.TEXT,
        imageUrls: MutableList<String> = mutableListOf()
    ): Result<String> {
        return try {
            val currentUserId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            val messageId = "message_" + System.currentTimeMillis()
            val timestamp = getCurrentTimestamp()

            // Upload images sequentially to avoid memory issues
            if (imageUrls.isNotEmpty()) {
                for (index in imageUrls.indices) {
                    val imageUrl = imageUrls[index]
                    if (imageUrl.startsWith("content://") || imageUrl.startsWith("file://")) {
                        try {
                            val uploadResult =
                                uploadChatImage(imageUrl, chatId, "${messageId}_img_$index")
                            if (uploadResult.isSuccess) {
                                imageUrls[index] = uploadResult.getOrThrow()
                                Log.d(TAG, "Successfully uploaded image $index")
                            } else {
                                Log.e(TAG, "Failed to upload image: $imageUrl")
                                return Result.failure(Exception("Image upload failed - please try again"))
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error uploading image: ${e.message}")
                            return Result.failure(Exception("Image upload error: ${e.message}"))
                        }
                    }
                }
            }

            val message = Message(
                id = messageId,
                content = content,
                timestamp = timestamp,
                senderId = currentUserId,
                type = type,
                imageUrls = imageUrls
            )

            // Save message to Firestore without nested timeouts
            try {
                chatsCollection
                    .document(chatId)
                    .collection(MESSAGES_SUBCOLLECTION)
                    .document(messageId)
                    .set(message)
                    .await()

                Log.d(TAG, "Message saved successfully: $messageId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save message: ${e.message}")
                throw Exception("Failed to send message: ${e.message}")
            }

            // Update chat metadata separately - non-blocking
            try {
                chatsCollection
                    .document(chatId)
                    .update(
                        mapOf(
                            "lastMessage" to content,
                            "lastUpdate" to timestamp,
                            "unreadCount" to FieldValue.increment(1),
                            "isUnread" to true
                        )
                    )
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Chat metadata update failed, but message was sent: ${e.message}")
            }

            Result.success(messageId)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            Result.failure(e)
        }
    }
    
    // Create a new chat
    suspend fun createChat(otherUserId: String): ChatItem {
        return try {
            val currentUserId = getCurrentUserId()
                ?: throw Exception("User not authenticated")

            val chatId = "chat_" + System.currentTimeMillis()
            val timestamp = getCurrentTimestamp()
            
            val chatItem = ChatItem(
                id = chatId,
                lastMessage = "",
                lastUpdate = timestamp,
                unreadCount = 0,
                userIds = listOf(currentUserId, otherUserId),
                chatDeleted = mapOf(
                    currentUserId to false,
                    otherUserId to false
                )
            )
            
            chatsCollection
                .document(chatId)
                .set(chatItem)
                .await()
            
            chatItem
        } catch (e: Exception) {
            Log.e(TAG, "Error creating chat", e)
            throw e
        }
    }
    
    // Get or create chat between two users
    suspend fun getOrCreateChat(otherUserId: String): ChatItem {
        return try {
            val currentUserId = getCurrentUserId()
                ?: throw Exception("User not authenticated")
            
            // First, try to find existing chat
            val existingChats = chatsCollection
                .whereArrayContains("userIds", currentUserId)
                .get()
                .await()
            
            val existingChat = existingChats.documents.find { doc ->
                val userIds = doc.get("userIds") as? List<*>
                userIds?.contains(otherUserId) == true
            }

            val chat: ChatItem = if (existingChat != null) {
                ChatItem(
                    id = existingChat.id,
                    lastMessage = existingChat.getString("lastMessage") ?: "",
                    lastUpdate = existingChat.getTimestamp("lastUpdate"),
                    unreadCount = (existingChat.getLong("unreadCount") ?: 0L).toInt(),
                    userIds = existingChat.get("userIds") as? List<String> ?: emptyList()
                )
            } else {
                createChat(otherUserId)
            }

            // Update chatDeleted status for current user
            chatsCollection
                .document(chat.id)
                .update("chatDeleted.$currentUserId", false)
                .await()

            val otherUserId = chat.userIds.firstOrNull { it != currentUserId }
            if (otherUserId != null) {
                try {
                    // Assuming getUserById is blocking or suspending
                    val user = userRepository.getUserById(otherUserId)
                    chat.contactName = user.fullName
                    chat.profilePictureUrl = user.profilePictureUrl
                    chat.email = user.email
                    Log.d(TAG, "Fetched contact name: ${chat.contactName}")
                } catch (e: Exception) {
                    chat.contactName = "Unknown"
                    Log.e(TAG, "Failed to fetch user name", e)
                }
            } else {
                chat.contactName = "Me"
            }

            Log.d(TAG, "Returning chat: $chat")

            chat
        } catch (e: Exception) {
            Log.e(TAG, "Error getting or creating chat", e)
            throw e
        }
    }
    
    // Mark messages as read
    suspend fun markMessagesAsRead(chatId: String): Result<Unit> {
        return try {
            chatsCollection
                .document(chatId)
                .update("unreadCount", 0)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking messages as read", e)
            Result.failure(e)
        }
    }
    
    // Delete a chat
    suspend fun deleteChat(chatId: String): Result<Unit> {
        return try {
            val currentUserId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            // Mark chat as deleted for current user
            chatsCollection
                .document(chatId)
                .update("chatDeleted.$currentUserId", true)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting chat", e)
            Result.failure(e)
        }
    }
    
    // Delete a message (mark as deleted and remove images from storage)
    suspend fun deleteMessage(chatId: String, messageId: String): Result<Unit> {
        return try {
            val currentUserId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            // Get the message first to check ownership and get image URLs
            val messageDoc = chatsCollection
                .document(chatId)
                .collection(MESSAGES_SUBCOLLECTION)
                .document(messageId)
                .get()
                .await()

            val message = messageDoc.toObject(Message::class.java)
                ?: return Result.failure(Exception("Message not found"))

            // Check if user owns the message
            if (message.senderId != currentUserId) {
                return Result.failure(Exception("You can only delete your own messages"))
            }

            // Delete images from Firebase Storage if any
            if (message.imageUrls.isNotEmpty()) {
                for (imageUrl in message.imageUrls) {
                    try {
                        val imageRef = storage.getReferenceFromUrl(imageUrl)
                        imageRef.delete().await()
                        Log.d(TAG, "Deleted image from storage: $imageUrl")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting image from storage: ${e.message}")
                        // Continue even if image deletion fails
                    }
                }
            }

            // Update message to mark as deleted
            val updates = hashMapOf<String, Any>(
                "type" to MessageType.DELETED,
                "content" to "Deleted message",
                "imageUrls" to emptyList<String>()
            )

            chatsCollection
                .document(chatId)
                .collection(MESSAGES_SUBCOLLECTION)
                .document(messageId)
                .update(updates)
                .await()

            Log.d(TAG, "Message marked as deleted: $messageId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting message", e)
            Result.failure(e)
        }
    }
    
    private fun getCurrentTimestamp(): Timestamp {
        return Timestamp.now()
    }
    
    private fun formatTimestamp(timestamp: Timestamp?): String {
        return try {
            if (timestamp == null) return "unknown"
            
            val date = timestamp.toDate()
            val now = Date()
            val diff = now.time - date.time
            
            when {
                diff < 60 * 1000 -> "now"
                diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m"
                diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h"
                else -> "${diff / (24 * 60 * 60 * 1000)}d"
            }
        } catch (e: Exception) {
            "unknown"
        }
    }

}