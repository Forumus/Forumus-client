package com.hcmus.forumus_client.ui.common

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import coil.load
import com.hcmus.forumus_client.R
import android.view.LayoutInflater
import android.view.ViewGroup

class PostMediaAdapter(
    private val onMediaClick: (position: Int) -> Unit
) : RecyclerView.Adapter<PostMediaAdapter.ImageViewHolder>() {

    private var mediaItems: List<PostMediaItem> = emptyList()
    private var totalCount: Int = 0

    /**
     * Truyền vào list media (ảnh + video). Adapter sẽ chỉ hiển thị tối đa 3 item,
     * nhưng totalCount vẫn là tổng số để tính overlay +N.
     */
    fun submitMedia(items: List<PostMediaItem>) {
        totalCount = items.size
        mediaItems = if (items.size <= 3) items else items.take(3)
        notifyDataSetChanged()
    }

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPostImage: ImageView = itemView.findViewById(R.id.ivPostImage)
        private val overlay: View = itemView.findViewById(R.id.overlay)
        private val tvMoreCount: TextView = itemView.findViewById(R.id.tvMoreCount)
        private val ivPlayIcon: ImageView = itemView.findViewById(R.id.ivPlayIcon)

        fun bind(item: PostMediaItem, position: Int) {
            when (item) {
                is PostMediaItem.Image -> {
                    ivPostImage.load(item.imageUrl) {
                        crossfade(true)
                        placeholder(R.drawable.image_loading)
                        error(R.drawable.error_image)
                    }
                    ivPlayIcon.visibility = View.GONE
                }
                is PostMediaItem.Video -> {
                    ivPostImage.load(item.thumbnailUrl) {
                        crossfade(true)
                        placeholder(R.drawable.image_loading)
                        error(R.drawable.gray_background)
                    }
                    // Hiển thị icon play cho video
                    ivPlayIcon.visibility = View.VISIBLE
                }
            }

            // Mặc định ẩn overlay +N
            overlay.visibility = View.GONE
            tvMoreCount.visibility = View.GONE

            // Nếu > 3 media, item thứ 3 (index 2) hiện overlay +N
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
            .inflate(R.layout.item_post_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun getItemCount(): Int = mediaItems.size

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(mediaItems[position], position)
    }
}
