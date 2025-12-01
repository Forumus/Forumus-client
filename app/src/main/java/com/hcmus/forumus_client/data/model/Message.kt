package com.hcmus.forumus_client.data.model

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val content: String = "",
    val timestamp: Timestamp? = null,
    val senderId: String = "",
    val type: MessageType = MessageType.TEXT
) {
    // Helper property for UI
    fun isSentByCurrentUser(currentUserId: String): Boolean {
        return senderId == currentUserId
    }
}

enum class MessageType {
    TEXT,
    IMAGE
}
