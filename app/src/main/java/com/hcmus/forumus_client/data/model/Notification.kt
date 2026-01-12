package com.hcmus.forumus_client.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Notification(
    var id: String = "",
    var type: String = "", // UPVOTE, COMMENT, REPLY
    var actorId: String = "",
    var actorName: String = "",
    val targetId: String = "", // Post/Comment ID
    val previewText: String = "",
    val originalPostTitle: String? = null,
    val originalPostContent: String? = null,
    val rejectionReason: String? = null,
    val createdAt: Timestamp? = null,
    @get:PropertyName("isRead") @set:PropertyName("isRead") var isRead: Boolean = false
)
