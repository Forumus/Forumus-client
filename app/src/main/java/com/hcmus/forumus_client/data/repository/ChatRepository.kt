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
import com.hcmus.forumus_client.data.model.User

class ChatRepository {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
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
    fun listenToUserChats(
        onChatsChanged: (List<ChatItem>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration? {
        val currentUserId = getCurrentUserId()
        Log.d(TAG, "Current user ID: $currentUserId")

        if (currentUserId == null) {
            onError(Exception("User not authenticated"))
            return null
        }

        return chatsCollection
            .whereArrayContains("userIds", currentUserId)
            .orderBy("lastUpdate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to chats", error)
                    onError(error)
                    return@addSnapshotListener
                }

                // Launch a coroutine to handle the processing and fetching
                CoroutineScope(Dispatchers.IO).launch {
                    val deferredChats = snapshot?.documents?.map { doc ->
                        // Use async to fetch data in parallel for each document
                        async {
                            try {
                                val chatItem = doc.toObject(ChatItem::class.java)?.copy(id = doc.id)
                                chatItem?.let { chat ->
                                    // Set UI properties
                                    chat.timestamp = formatTimestamp(chat.lastUpdate)
                                    chat.isUnread = chat.unreadCount > 0

                                    // Fetch Name Logic
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
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing chat item", e)
                                null
                            }
                        }
                    }

                    // Wait for all async fetch operations to complete
                    val chats = deferredChats?.awaitAll()?.filterNotNull() ?: emptyList()

                    // Switch back to Main thread to update UI
                    withContext(Dispatchers.Main) {
                        onChatsChanged(chats)
                    }
                }
            }
    }
    // Listen to messages in a specific chat
    fun listenToMessages(
        chatId: String,
        onMessagesChanged: (List<Message>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return chatsCollection
            .document(chatId)
            .collection(MESSAGES_SUBCOLLECTION)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to messages", error)
                    onError(error)
                    return@addSnapshotListener
                }
                
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Message::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing message", e)
                        null
                    }
                } ?: emptyList()
                
                onMessagesChanged(messages)
            }
    }
    
    // Send a message
    suspend fun sendMessage(
        chatId: String,
        content: String,
        type: MessageType = MessageType.TEXT
    ): Result<String> {
        return try {
            val currentUserId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))
            
            val messageId = "message_" + System.currentTimeMillis()
            val timestamp = getCurrentTimestamp()
            
            val message = Message(
                id = messageId,
                content = content,
                timestamp = timestamp,
                senderId = currentUserId,
                type = type
            )
            
            // Add message to subcollection
            chatsCollection
                .document(chatId)
                .collection(MESSAGES_SUBCOLLECTION)
                .document(messageId)
                .set(message)
                .await()
            
            // Update chat's last message and timestamp
            chatsCollection
                .document(chatId)
                .update(
                    mapOf(
                        "lastMessage" to content,
                        "lastUpdate" to timestamp
                    )
                )
                .await()
            
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
    
    // Method to fetch user name asynchronously for future use
    suspend fun getUserName(userId: String): String {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val user = userDoc.toObject(User::class.java)
            user?.fullName ?: "Unknown User"
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user name", e)
            "Unknown User"
        }
    }

}