package com.hcmus.forumus_client.ui.common

// Một item media có thể là ảnh hoặc video
sealed class PostMediaItem {
    data class Image(val imageUrl: String?) : PostMediaItem()
    data class Video(
        val thumbnailUrl: String?, // URL của thumbnail video
        val videoUrl: String      // URL video gốc (để mở player sau này)
    ) : PostMediaItem()
}
