package com.hcmus.forumus_client.data.model

data class TopicItem(
    val name: String,
    val icon: String, // Sử dụng Emoji làm icon cho nhanh, hoặc Resource ID nếu có ảnh
    var isSelected: Boolean = false
)