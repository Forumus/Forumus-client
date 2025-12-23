package com.hcmus.forumus_client.data.model

import com.google.firebase.firestore.DocumentId

data class Topic(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val icon: String = "", // Resource name or URL
    val fillColor: String = "",
    val fillAlpha: Double = 0.1,
    val count: Int = 0
)