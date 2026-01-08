package com.hcmus.forumus_client.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Notification(
    var id: String = "",
    var type: String = "", // UPVOTE, COMMENT, REPLY
    var actorId: String = "",
    var actorName: String = "",
    var targetId: String = "", // Post/Comment ID
    var previewText: String = "",
    var createdAt: Timestamp? = null,
    @get:PropertyName("isRead") @set:PropertyName("isRead") var isRead: Boolean = false
)
