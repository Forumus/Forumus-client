package com.hcmus.forumus_client.ui.common

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import coil.load
import com.hcmus.forumus_client.R
import android.view.LayoutInflater
import android.view.ViewGroup
import android.util.Log

class PostImagesAdapter(
    private val onImageClick: (position: Int) -> Unit
) : RecyclerView.Adapter<PostImagesAdapter.ImageViewHolder>() {

    private var images: List<String> = emptyList()
    private var totalCount: Int = 0

    fun submitImages(urls: List<String>) {
        totalCount = urls.size
        images = if (urls.size <= 3) urls else urls.take(3)
        notifyDataSetChanged()
    }

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPostImage: ImageView = itemView.findViewById(R.id.ivPostImage)
        private val overlay: View = itemView.findViewById(R.id.overlay)
        private val tvMoreCount: TextView = itemView.findViewById(R.id.tvMoreCount)

        fun bind(url: String, position: Int) {
            ivPostImage.load(url) {
                crossfade(true)
                placeholder(R.drawable.image_loading)
                error(R.drawable.error_image)
            }

            // Mặc định ẩn overlay
            overlay.visibility = View.GONE
            tvMoreCount.visibility = View.GONE

            // Nếu > 3 ảnh, item thứ 3 (index 2) hiện overlay +N
            if (totalCount > 3 && position == 2) {
                val moreCount = totalCount - 3
                overlay.visibility = View.VISIBLE
                tvMoreCount.visibility = View.VISIBLE
                tvMoreCount.text = "+$moreCount"
            }

            itemView.setOnClickListener {
                onImageClick(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun getItemCount(): Int = images.size

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position], position)
    }
}
