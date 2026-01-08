package com.hcmus.forumus_client.ui.media

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaViewerItem(
    val type: Type,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val thumbnailUrl: String? = null
) : Parcelable {
    enum class Type {
        IMAGE, VIDEO
    }
}
