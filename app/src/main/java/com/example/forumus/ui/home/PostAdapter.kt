package com.example.forumus.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.forumus.databinding.ItemPostCardBinding

class PostAdapter : ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    inner class PostViewHolder(private val binding: ItemPostCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            with(binding) {
                tvCommunityName.text = post.communityName
                tvTimePosted.text = "• ${post.timePosted}"
                tvPostTitle.text = post.title
                tvPostContent.text = post.content
                tvVoteCount.text = post.upvotes.toString()
                tvCommentCount.text = post.comments.toString()
                tvCommunityIcon.text = post.communityIcon

                // Load images into grid
                if (post.images.isNotEmpty()) {
                    loadPostImages(post.images)
                } else {
                    glPostImages.visibility = android.view.View.GONE
                }

                // Setup click listeners
                btnUpvote.setOnClickListener {
                    // Handle upvote
                }

                btnDownvote.setOnClickListener {
                    // Handle downvote
                }

                btnComments.setOnClickListener {
                    // Navigate to comments
                }

                btnShare.setOnClickListener {
                    // Share post
                }
            }
        }

        private fun loadPostImages(images: List<String>) {
            binding.glPostImages.removeAllViews()
            binding.glPostImages.visibility = android.view.View.VISIBLE

            val imagesToLoad = images.take(4)
            imagesToLoad.forEachIndexed { index, imageUrl ->
                val imageView = ImageView(itemView.context).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = 160
                        columnSpec = GridLayout.spec(index % 2, 1f)
                        rowSpec = GridLayout.spec(index / 2)
                    }
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }

                // Load image using Coil
                imageView.load(imageUrl) {
                    crossfade(true)
                }

                binding.glPostImages.addView(imageView)
            }

            // Add overlay for +N more images if there are more than 4
            if (images.size > 4) {
                val overlayView = ImageView(itemView.context).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = 160
                        columnSpec = GridLayout.spec(1, 1f)
                        rowSpec = GridLayout.spec(1)
                    }
                    setBackgroundColor(0x99000000.toInt())
                }
                binding.glPostImages.addView(overlayView)

                // Add text overlay showing count
                val overlayText = android.widget.TextView(itemView.context).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = 160
                        columnSpec = GridLayout.spec(1, 1f)
                        rowSpec = GridLayout.spec(1)
                    }
                    gravity = android.view.Gravity.CENTER
                    text = "+${images.size - 4}"
                    textSize = 32f
                    setTextColor(android.graphics.Color.WHITE)
                }
                binding.glPostImages.addView(overlayText)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        return PostViewHolder(
            ItemPostCardBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
    override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean =
        oldItem == newItem
}
