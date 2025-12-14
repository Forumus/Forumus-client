package com.hcmus.forumus_client.ui.conversation

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hcmus.forumus_client.databinding.ItemMessageImageBinding

class MessageImageAdapter(
    private var onImageClick: ((String, Int) -> Unit)? = null
) : RecyclerView.Adapter<MessageImageAdapter.MessageImageViewHolder>() {

    private val images: MutableList<String> = mutableListOf()

    fun setImageUrls(imageUrls: List<String>) {
        val diffCallback = ImageDiffCallback(images, imageUrls)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        images.clear()
        images.addAll(imageUrls)
        diffResult.dispatchUpdatesTo(this)
    }

    fun setOnImageClickListener(listener: (String, Int) -> Unit) {
        this.onImageClick = listener
    }
    
    private class ImageDiffCallback(
        private val oldList: List<String>,
        private val newList: List<String>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
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
            
            val requestOptions = RequestOptions()
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .override(200, 200) // Limit size to prevent OOM
                .timeout(10000) // 10 second timeout
            
            Glide.with(binding.root.context)
                .load(imageUrl)
                .apply(requestOptions)
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
