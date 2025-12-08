package com.hcmus.forumus_client.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class Post(
	var id: String = "",
	var authorId: String = "",
	var authorName: String = "",
	var authorAvatarUrl: String? = "",

	var createdAt: Timestamp? = null,
	var title: String = "",
	var content: String = "",

	var upvoteCount: Int = 0,
	var downvoteCount: Int = 0,
	var commentCount: Int = 0,
	var reportCount: Int = 0,

	var imageUrls: MutableList<String> = mutableListOf(),
	var videoUrls: MutableList<String> = mutableListOf(),
	var videoThumbnailUrls: MutableList<String?> = mutableListOf(),

	var votedUsers: MutableMap<String, VoteState> = mutableMapOf(),
	var reportedUsers: MutableList<String> = mutableListOf(),

	@get:Exclude
	@set:Exclude
	var userVote: VoteState = VoteState.NONE
)