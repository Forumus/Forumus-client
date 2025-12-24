package com.hcmus.forumus_client.data.model

import com.google.firebase.firestore.DocumentId

data class Topic(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    val fillColor: String = "",
    val fillAlpha: Double = 0.1, // Độ mờ mặc định
    val postCount: Int = 0       // Số lượng bài viết
)