package com.hcmus.forumus_client.ui.conversation

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hcmus.forumus_client.databinding.ItemImagePreviewBinding

class ImagePreviewAdapter(
    private val onRemoveClick: (Uri) -> Unit
) : RecyclerView.Adapter<ImagePreviewAdapter.ImagePreviewViewHolder>() {

    private val images: MutableList<Uri> = mutableListOf()

    fun setImages(imageList: List<Uri>) {
        images.clear()
        images.addAll(imageList)
        notifyDataSetChanged()
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
            Glide.with(binding.root.context)
                .load(imageUri)
                .centerCrop()
                .into(binding.ivImage)

            binding.btnRemove.setOnClickListener {
                onRemoveClick(imageUri)
            }
        }
    }
}
