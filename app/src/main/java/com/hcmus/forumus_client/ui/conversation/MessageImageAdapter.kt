package com.hcmus.forumus_client.ui.conversation

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.hcmus.forumus_client.databinding.ItemMessageImageBinding

class MessageImageAdapter(
    private val onImageClick: ((String, Int) -> Unit)? = null
) : RecyclerView.Adapter<MessageImageAdapter.MessageImageViewHolder>() {

    private val images: MutableList<String> = mutableListOf()

    fun setImages(imageList: List<Uri>) {
        images.clear()
        images.addAll(imageList.map { it.toString() })
        notifyDataSetChanged()
    }

    fun setImageUrls(imageUrls: List<String>) {
        images.clear()
        images.addAll(imageUrls)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageImageViewHolder {
        val binding = ItemMessageImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageImageViewHolder(binding, onImageClick)
    }

    override fun onBindViewHolder(holder: MessageImageViewHolder, position: Int) {
        holder.bind(images[position], position, images)
    }

    override fun getItemCount(): Int = images.size

    class MessageImageViewHolder(
        private val binding: ItemMessageImageBinding,
        private val onImageClick: ((String, Int) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(imageUrl: String, position: Int, allImages: List<String>) {
            // Show loading indicator
            binding.pbLoading.visibility = View.VISIBLE
            
            Glide.with(binding.root.context)
                .load(imageUrl)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.pbLoading.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.pbLoading.visibility = View.GONE
                        return false
                    }
                })
                .into(binding.ivImage)

            // Set click listener for fullscreen view
            binding.root.setOnClickListener {
                onImageClick?.invoke(imageUrl, position)
            }
        }
    }
}
