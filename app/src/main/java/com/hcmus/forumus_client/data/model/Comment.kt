package com.hcmus.forumus_client.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class Comment(
    var id: String = "",
    var postId: String = "",

    var authorId: String = "",
    var authorName: String = "",
    var authorRole: UserRole = UserRole.STUDENT,
    var authorAvatarUrl: String? = "",

    var createdAt: Timestamp? = null,
    var isOriginalPoster: Boolean = false,
    var content: String = "",

    var upvoteCount: Int = 0,
    var downvoteCount: Int = 0,
    var commentCount: Int = 0,

    var replyToUserId: String? = null,
    var replyToUserName: String? = null,

    var parentCommentId: String? = null,

    var votedUsers: MutableMap<String, VoteState> = mutableMapOf(),

    @get:Exclude
    @set:Exclude
    var userVote: VoteState = VoteState.NONE,

    @get:Exclude
    @set:Exclude
    var isRepliesExpanded: Boolean = false
)