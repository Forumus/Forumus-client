package com.hcmus.forumus_client.data.model

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val content: String = "",
    val timestamp: Timestamp? = null,
    val senderId: String = "",
    val type: MessageType = MessageType.TEXT,
    val imageUrls: List<String> = emptyList()
) {
    // Helper property for UI
    fun isSentByCurrentUser(currentUserId: String): Boolean {
        return senderId == currentUserId
    }
    
    // Validate that imageUrls doesn't exceed max limit
    fun isValid(): Boolean {
        return imageUrls.size <= MAX_IMAGES_PER_MESSAGE
    }
    
    companion object {
        const val MAX_IMAGES_PER_MESSAGE = 5
    }
}

enum class MessageType {
    TEXT,
    IMAGE,
    DELETED
}
