package com.hcmus.forumus_client.data.model

import com.google.firebase.firestore.DocumentId

data class Topic(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val icon: String = "", // Resource name or URL
    val count: Int = 0
)