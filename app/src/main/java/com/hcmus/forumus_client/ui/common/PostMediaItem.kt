package com.hcmus.forumus_client.ui.common

sealed class PostMediaItem {
    data class Image(val imageUrl: String?) : PostMediaItem()
    data class Video(
        val thumbnailUrl: String?,
        val videoUrl: String
    ) : PostMediaItem()
}
