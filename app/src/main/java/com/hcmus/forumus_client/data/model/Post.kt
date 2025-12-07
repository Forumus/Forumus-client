package com.hcmus.forumus_client.data.model

import com.google.firebase.Timestamp

data class Post(
	// --- CÁC TRƯỜNG DỮ LIỆU ---
	var id: String = "",
	val title: String = "",
	val content: String = "",
	val imageUrls: List<String> = emptyList(),

	// Thông tin Firebase (User & Topic)
	val authorId: String = "",
	val authorName: String = "",
	val authorAvatarUrl: String = "",
	val topics: List<String> = emptyList(),

	// Thông tin UI Home (Giữ tên cũ để không lỗi code cũ)
	val communityName: String = "General",
	val communityIconLetter: String = "G",
	val timePosted: String = "Just now",

	// Biến đếm
	val voteCount: Int = 0,
	val commentCount: Int = 0,
	val viewCount: Int = 0,
	val reportedCount: Int = 0,

	// TRẠNG THÁI VOTE (QUAN TRỌNG: Dùng String "UP", "DOWN", "NONE")
	val userVote: String = "NONE",

	// Timestamp
	val createdAt: Timestamp = Timestamp.now()
)