package com.hcmus.forumus_client.ui.image

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.hcmus.forumus_client.databinding.ItemFullscreenImageBinding

class FullscreenImageAdapter(
    private val imageUrls: List<String>,
    private val onImageClick: () -> Unit
) : RecyclerView.Adapter<FullscreenImageAdapter.FullscreenImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FullscreenImageViewHolder {
        val binding = ItemFullscreenImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FullscreenImageViewHolder(binding, onImageClick)
    }

    override fun onBindViewHolder(holder: FullscreenImageViewHolder, position: Int) {
        holder.bind(imageUrls[position])
    }

    override fun getItemCount(): Int = imageUrls.size

    class FullscreenImageViewHolder(
        private val binding: ItemFullscreenImageBinding,
        private val onImageClick: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(imageUrl: String) {
            binding.pbLoading.visibility = View.VISIBLE
            
            Glide.with(binding.root.context)
                .load(imageUrl)
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
                .into(binding.ivFullscreenImage)

            binding.ivFullscreenImage.setOnClickListener {
                onImageClick()
            }
        }
    }
}