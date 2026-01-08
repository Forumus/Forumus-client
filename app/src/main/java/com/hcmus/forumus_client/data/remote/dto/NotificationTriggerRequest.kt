package com.hcmus.forumus_client.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NotificationTriggerRequest(
    val type: String, // UPVOTE, COMMENT, REPLY
    val actorId: String,
    val actorName: String,
    val targetId: String,
    val targetUserId: String,
    val previewText: String
)
