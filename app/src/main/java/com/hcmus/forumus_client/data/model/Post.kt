package com.hcmus.forumus_client.data.model

data class Post(
	val id: String,
	val communityName: String,
	val communityIconLetter: String,
	val timePosted: String, // e.g. "1h" or formatted date
	val title: String,
	val content: String,
	val voteCount: Int,
	val commentCount: Int,
	val imageUrls: List<String> = emptyList(),
	val userVote: VoteState = VoteState.NONE
)