package com.hcmus.forumus_client.data.model

data class Message(
    val id: String,
    val content: String,
    val timestamp: String,
    val isSentByCurrentUser: Boolean,
    val senderName: String? = null,
    val senderEmail: String? = null,
    val senderAvatar: String? = null
)