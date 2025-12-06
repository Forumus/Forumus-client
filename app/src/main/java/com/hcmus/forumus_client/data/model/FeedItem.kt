package com.hcmus.forumus_client.data.model

sealed class FeedItem {
    data class PostItem(val post: Post) : FeedItem()
    data class CommentItem(val comment: Comment) : FeedItem()
}

fun FeedItem.createdAtMillis(): Long {
    return when (this) {
        is FeedItem.PostItem ->
            this.post.createdAt?.toDate()?.time ?: 0L
        is FeedItem.CommentItem ->
            this.comment.createdAt?.toDate()?.time ?: 0L
    }
}
