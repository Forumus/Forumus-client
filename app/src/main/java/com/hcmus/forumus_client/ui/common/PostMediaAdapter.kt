package com.hcmus.forumus_client.ui.common

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import coil.load
import coil.request.CachePolicy
import com.hcmus.forumus_client.R
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ProgressBar

class PostMediaAdapter(
    private var onMediaClick: (position: Int) -> Unit = {}
) : RecyclerView.Adapter<PostMediaAdapter.ImageViewHolder>() {

    private var allItems: List<PostMediaItem> = emptyList()
    private var displayedItems: List<PostMediaItem> = emptyList()
    private var totalCount: Int = 0

    // Shows max 3 items, rest shown as +N overlay on 3rd item
    fun submitMedia(items: List<PostMediaItem>) {
        totalCount = items.size
        allItems = items
        displayedItems = if (items.size <= 3) items else items.take(3)
        notifyDataSetChanged()
    }

    fun setOnMediaClickListener(listener: (position: Int) -> Unit) {
        onMediaClick = listener
    }

    fun getAllItems(): List<PostMediaItem> = allItems

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPostImage: ImageView = itemView.findViewById(R.id.ivPostImage)
        private val overlay: View = itemView.findViewById(R.id.overlay)
        private val tvMoreCount: TextView = itemView.findViewById(R.id.tvMoreCount)
        private val ivPlayIcon: ImageView = itemView.findViewById(R.id.ivPlayIcon)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.mediaProgress)

        fun bind(item: PostMediaItem, position: Int) {
            progressBar.visibility = View.VISIBLE
            ivPlayIcon.visibility = View.GONE

            when (item) {
                is PostMediaItem.Image -> {
                    ivPostImage.load(item.imageUrl) {
                        crossfade(true)
                        error(R.drawable.error_image)
                        placeholder(R.drawable.gray_background)
                        memoryCachePolicy(CachePolicy.ENABLED)
                        diskCachePolicy(CachePolicy.ENABLED)
                        networkCachePolicy(CachePolicy.ENABLED)

                        listener(
                            onStart = { progressBar.visibility = View.VISIBLE },
                            onSuccess = { _, _ -> progressBar.visibility = View.GONE },
                            onError = { _, _ -> progressBar.visibility = View.GONE }
                        )
                    }
                }
                is PostMediaItem.Video -> {
                    ivPostImage.load(item.thumbnailUrl) {
                        crossfade(true)
                        error(R.drawable.gray_background)
                        placeholder(R.drawable.gray_background)
                        memoryCachePolicy(CachePolicy.ENABLED)
                        diskCachePolicy(CachePolicy.ENABLED)
                        networkCachePolicy(CachePolicy.ENABLED)
                        
                        listener(
                            onStart = { progressBar.visibility = View.VISIBLE },
                            onSuccess = { _, _ -> progressBar.visibility = View.GONE },
                            onError = { _, _ -> progressBar.visibility = View.GONE }
                        )
                    }
                    ivPlayIcon.visibility = View.VISIBLE
                }
            }

            overlay.visibility = View.GONE
            tvMoreCount.visibility = View.GONE

            if (totalCount > 3 && position == 2) {
                val moreCount = totalCount - 3
                overlay.visibility = View.VISIBLE
                tvMoreCount.visibility = View.VISIBLE
                tvMoreCount.text = "+$moreCount"
            }

            itemView.setOnClickListener {
                onMediaClick(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post_media, parent, false)
        return ImageViewHolder(view)
    }

    override fun getItemCount(): Int = displayedItems.size

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(displayedItems[position], position)
    }
}
