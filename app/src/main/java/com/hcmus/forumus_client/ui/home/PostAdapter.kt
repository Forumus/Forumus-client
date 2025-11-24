package com.hcmus.forumus_client.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.data.model.Post

class PostAdapter(
    private val listener: PostInteractionListener? = null
) : ListAdapter<Post, PostAdapter.PostViewHolder>(DiffCallback) {

    interface PostInteractionListener {
        fun onUpvote(post: Post)
        fun onDownvote(post: Post)
        fun onComments(post: Post)
        fun onShare(post: Post)
        fun onPostClicked(post: Post)
    }

    object DiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post_card, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        holder.bind(post)
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCommunityIcon: TextView = itemView.findViewById(R.id.tv_community_icon)
        private val tvCommunityName: TextView = itemView.findViewById(R.id.tv_community_name)
        private val tvTimePosted: TextView = itemView.findViewById(R.id.tv_time_posted)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_post_title)
        private val tvContent: TextView = itemView.findViewById(R.id.tv_post_content)
        private val tvVoteCount: TextView = itemView.findViewById(R.id.tv_vote_count)
        private val tvCommentCount: TextView = itemView.findViewById(R.id.tv_comment_count)
        private val btnUpvote: ImageButton = itemView.findViewById(R.id.btn_upvote)
        private val btnDownvote: ImageButton = itemView.findViewById(R.id.btn_downvote)
        private val btnComments: ImageButton = itemView.findViewById(R.id.btn_comments)
        private val btnShare: ImageButton = itemView.findViewById(R.id.btn_share)
        private val imagesGrid: ViewGroup = itemView.findViewById(R.id.gl_post_images)

        fun bind(post: Post) {
            tvCommunityIcon.text = post.communityIconLetter
            tvCommunityName.text = post.communityName
            tvTimePosted.text = "• ${post.timePosted}"
            tvTitle.text = post.title
            tvContent.text = post.content
            tvVoteCount.text = post.voteCount.toString()
            tvCommentCount.text = post.commentCount.toString()

            // Simple image handling placeholder: remove previous children
            imagesGrid.removeAllViews()
            // Could load images with an image library; placeholder icons for now
            val context = itemView.context
            post.imageUrls.take(4).forEach { _ ->
                val iv = ImageView(context)
                iv.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    240 // fixed height placeholder
                )
                iv.scaleType = ImageView.ScaleType.CENTER_CROP
                iv.setImageResource(R.drawable.ic_default_profile) // placeholder
                imagesGrid.addView(iv)
            }

            itemView.setOnClickListener { listener?.onPostClicked(post) }
            btnUpvote.setOnClickListener { listener?.onUpvote(post) }
            btnDownvote.setOnClickListener { listener?.onDownvote(post) }
            btnComments.setOnClickListener { listener?.onComments(post) }
            btnShare.setOnClickListener { listener?.onShare(post) }
        }
    }
}