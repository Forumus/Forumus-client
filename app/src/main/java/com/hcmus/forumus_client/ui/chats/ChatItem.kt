package com.hcmus.forumus_client.ui.chats

import com.google.firebase.Timestamp

data class ChatItem(
    val id: String = "",
    val lastMessage: String = "",
    val lastUpdate: Timestamp? = null,
    val unreadCount: Int = 0,
    val userIds: List<String> = emptyList()
) {
    // UI helper properties
    var contactName: String = ""
    var email: String = ""
    var profilePictureUrl: String? = null
    var isUnread: Boolean = false
    var timestamp: String = ""
}