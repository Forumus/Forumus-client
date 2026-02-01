package com.hcmus.forumus_client.ui.media

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.CachePolicy
import com.github.chrisbanes.photoview.PhotoView
import com.hcmus.forumus_client.R
import kotlin.math.min

/** ViewPager2 adapter for media items. */
class MediaViewerPagerAdapter : RecyclerView.Adapter<MediaViewerPagerAdapter.MediaPageViewHolder>() {

    private var mediaItems: List<MediaViewerItem> = emptyList()

    fun submitList(items: List<MediaViewerItem>) {
        mediaItems = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaPageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media_viewer_page, parent, false)
        return MediaPageViewHolder(view)
    }

    override fun getItemCount(): Int = mediaItems.size

    override fun onBindViewHolder(holder: MediaPageViewHolder, position: Int) {
        holder.bind(mediaItems[position])
    }

    class MediaPageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivMedia: PhotoView = itemView.findViewById(R.id.ivMedia)
        private val vvMedia: VideoView = itemView.findViewById(R.id.vvMedia)

        fun bind(item: MediaViewerItem) {
            try {
                if (item.type == MediaViewerItem.Type.IMAGE) {
                    vvMedia.visibility = View.GONE
                    vvMedia.stopPlayback()
                    ivMedia.visibility = View.VISIBLE

                    // Configure PhotoView for zoom functionality
                    ivMedia.maximumScale = 5.0f
                    ivMedia.mediumScale = 2.5f
                    ivMedia.minimumScale = 1.0f
                    ivMedia.adjustViewBounds = true
                    ivMedia.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER

                    ivMedia.load(item.imageUrl) {
                        crossfade(true)
                        error(R.drawable.error_image)
                        // Enable caching for full resolution images
                        memoryCachePolicy(CachePolicy.ENABLED)
                        diskCachePolicy(CachePolicy.ENABLED)
                        networkCachePolicy(CachePolicy.ENABLED)
                    }
                } else {
                    ivMedia.visibility = View.GONE
                    vvMedia.visibility = View.VISIBLE

                    try {
                        vvMedia.setVideoURI(Uri.parse(item.videoUrl))

                        // Adjust VideoView size to preserve aspect ratio (contain behavior)
                        vvMedia.setOnPreparedListener { mp ->
                            try {
                                val videoWidth = mp.videoWidth
                                val videoHeight = mp.videoHeight

                                vvMedia.post {
                                    val containerW = vvMedia.rootView.width
                                    val containerH = vvMedia.rootView.height

                                    if (videoWidth > 0 && videoHeight > 0 && containerW > 0 && containerH > 0) {
                                        val scale = min(
                                            containerW.toFloat() / videoWidth,
                                            containerH.toFloat() / videoHeight
                                        )
                                        val newW = (videoWidth * scale).toInt()
                                        val newH = (videoHeight * scale).toInt()

                                        val lp = vvMedia.layoutParams
                                        lp.width = newW
                                        lp.height = newH
                                        vvMedia.layoutParams = lp
                                    }

                                    vvMedia.start()
                                }
                            } catch (e: Exception) {
                                Log.e("MediaViewerPagerAdapter", "Error sizing video", e)
                                vvMedia.start()
                            }
                        }

                        val mc = MediaController(itemView.context)
                        mc.setAnchorView(vvMedia)
                        vvMedia.setMediaController(mc)
                        vvMedia.requestFocus()
                    } catch (e: Exception) {
                        Log.e("MediaViewerPagerAdapter", "Error playing video", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("MediaViewerPagerAdapter", "Error showing media", e)
            }
        }
    }
}
