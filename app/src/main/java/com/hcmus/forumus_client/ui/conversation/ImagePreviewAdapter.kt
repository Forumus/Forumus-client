package com.hcmus.forumus_client.ui.conversation

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hcmus.forumus_client.databinding.ItemImagePreviewBinding

class ImagePreviewAdapter(
    private val onRemoveClick: (Uri) -> Unit
) : RecyclerView.Adapter<ImagePreviewAdapter.ImagePreviewViewHolder>() {

    private val images: MutableList<Uri> = mutableListOf()

    fun setImages(imageList: List<Uri>) {
        val diffCallback = UriDiffCallback(images, imageList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        images.clear()
        images.addAll(imageList)
        diffResult.dispatchUpdatesTo(this)
    }
    
    private class UriDiffCallback(
        private val oldList: List<Uri>,
        private val newList: List<Uri>
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImagePreviewViewHolder {
        val binding = ItemImagePreviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImagePreviewViewHolder(binding, onRemoveClick)
    }

    override fun onBindViewHolder(holder: ImagePreviewViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size

    class ImagePreviewViewHolder(
        private val binding: ItemImagePreviewBinding,
        private val onRemoveClick: (Uri) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(imageUri: Uri) {
            // Clear previous image
            binding.ivImage.setImageDrawable(null)
            
            val requestOptions = RequestOptions()
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .override(100, 100) // Small preview size
                .timeout(5000) // 5 second timeout for previews
            
            Glide.with(binding.root.context)
                .load(imageUri)
                .apply(requestOptions)
                .thumbnail(0.2f) // Quick thumbnail
                .into(binding.ivImage)

            binding.btnRemove.setOnClickListener {
                onRemoveClick(imageUri)
            }
        }
    }
}
