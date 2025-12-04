package com.hcmus.forumus_client.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.hcmus.forumus_client.data.model.Message
import com.hcmus.forumus_client.data.model.MessageType
import com.hcmus.forumus_client.ui.chats.ChatItem
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.*
import com.google.firebase.storage.FirebaseStorage
import androidx.core.net.toUri
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

class ChatRepository {
    
    private val firestore = FirebaseFirestore.getInstance()
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
    fun getUserChatsFlow(): Flow<List<ChatItem>> = callbackFlow {
        val currentUserId = getCurrentUserId()
        Log.d(TAG, "Current user ID: $currentUserId")

        // 1. Handle unauthenticated state by closing the flow with an exception
        if (currentUserId == null) {
            close(Exception("User not authenticated"))
            return@callbackFlow
        }

        // 2. Setup the Firestore listener
        val listener = chatsCollection
            .whereArrayContains("userIds", currentUserId)
            .orderBy("lastUpdate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to chats", error)
                    close(error) // Close the flow on error
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // 3. Process data asynchronously on IO thread
                    // We use 'launch' from the ProducerScope to keep the flow active
                    launch(Dispatchers.IO) {
                        val deferredChats = snapshot.documents.map { doc ->
                            async {
                                try {
                                    val chatItem = doc.toObject(ChatItem::class.java)?.copy(id = doc.id)
                                    chatItem?.let { chat ->
                                        // Set UI properties
                                        chat.timestamp = formatTimestamp(chat.lastUpdate)
                                        chat.isUnread = chat.unreadCount > 0

                                        // Fetch Name Logic (Preserved from your original code)
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

    // Listen to messages in a specific chat
    fun getChatMessagesFlow(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = chatsCollection
            .document(chatId)
            .collection(MESSAGES_SUBCOLLECTION)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limitToLast(50)
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
                            "lastUpdate" to timestamp
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
                userIds = listOf(currentUserId, otherUserId)
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