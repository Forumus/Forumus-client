package com.hcmus.forumus_client.ui.chats

data class ChatItem(
    val id: String,
    val contactName: String,
    val lastMessage: String,
    val timestamp: String,
    val isUnread: Boolean,
    val unreadCount: Int,
    val profileImageUrl: String?
)