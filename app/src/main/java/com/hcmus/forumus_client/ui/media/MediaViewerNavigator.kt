package com.hcmus.forumus_client.ui.media

import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.data.model.Post

object MediaViewerNavigator {
    /** Opens media viewer for a post at given index. */
    fun open(view: View, post: Post, startIndex: Int) {
        val activity = view.context as? FragmentActivity ?: return

        // Build media list in original order (images then videos paired with thumbnails)
        val imageUrls = post.imageUrls ?: emptyList()
        val videoUrls = post.videoUrls ?: emptyList()
        val videoThumbs = post.videoThumbnailUrls ?: emptyList()

        val mediaList = ArrayList<MediaViewerItem>()
        imageUrls.forEach { url ->
            mediaList.add(MediaViewerItem(type = MediaViewerItem.Type.IMAGE, imageUrl = url))
        }

        val pairCount = kotlin.math.min(videoUrls.size, videoThumbs.size)
        for (i in 0 until pairCount) {
            mediaList.add(
                MediaViewerItem(
                    type = MediaViewerItem.Type.VIDEO,
                    imageUrl = videoThumbs[i],
                    videoUrl = videoUrls[i],
                    thumbnailUrl = videoThumbs[i]
                )
            )
        }

        // Set media list into activity-scoped ViewModel so MediaViewerFragment can observe it
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(activity.application)
        val vm = ViewModelProvider(activity, factory).get(MediaViewerViewModel::class.java)
        vm.setMediaList(mediaList, startIndex)

        try {
            val navController = view.findNavController()
            navController.navigate(R.id.mediaViewerFragment)
        } catch (_: Exception) {
            // ignore navigation errors
        }
    }
}
