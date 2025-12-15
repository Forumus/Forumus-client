package com.hcmus.forumus_client.ui.post.create

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hcmus.forumus_client.databinding.ItemSelectedImageBinding

// Adapter chuẩn nhận 2 tham số
class SelectedImageAdapter(
    private val onDelete: (Uri) -> Unit,
    private val onItemClick: (Uri) -> Unit
) : RecyclerView.Adapter<SelectedImageAdapter.ImageViewHolder>() {

    private val images = mutableListOf<Uri>()

    fun submitList(newImages: List<Uri>) {
        images.clear()
        images.addAll(newImages)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemSelectedImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size

    inner class ImageViewHolder(private val binding: ItemSelectedImageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(uri: Uri) {
            // Load ảnh
            Glide.with(itemView.context).load(uri).into(binding.ivSelected)

            // Xóa (Dùng onDelete)
            binding.ivRemove.setOnClickListener { onDelete(uri) }

            // Xem (Dùng onItemClick)
            binding.root.setOnClickListener { onItemClick(uri) }
        }
    }
}