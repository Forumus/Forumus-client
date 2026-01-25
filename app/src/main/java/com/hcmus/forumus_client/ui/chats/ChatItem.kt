package com.hcmus.forumus_client.ui.chats

import com.google.firebase.Timestamp

data class ChatItem(
    val id: String = "",
    val lastMessage: String = "",
    val lastUpdate: Timestamp? = null,
    val unreadCounts: Map<String, Int> = emptyMap(),
    val userIds: List<String> = emptyList(),
    val chatDeleted: Map<String, Boolean> = emptyMap()
) {
    // UI helper properties
    var contactName: String = ""
    var email: String = ""
    var profilePictureUrl: String? = null
    var timestamp: String = ""
    
    // Computed property for current user's unread count
    fun getUnreadCount(userId: String): Int = unreadCounts[userId] ?: 0
    
    // Computed property for current user's unread status
    fun isUnread(userId: String): Boolean = getUnreadCount(userId) > 0
}