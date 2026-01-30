package com.hcmus.forumus_client.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hcmus.forumus_client.data.model.Comment
import com.hcmus.forumus_client.data.model.CommentAction
import com.hcmus.forumus_client.data.model.FeedItem
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.model.PostAction
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.ui.common.PostViewHolder
import com.hcmus.forumus_client.ui.common.CommentViewHolder
import android.view.View
import com.hcmus.forumus_client.data.model.Topic

/**
 * Adapter for displaying user's posts and comments in profile screen.
 * Handles two item types (posts and comments) with their respective actions.
 */
class ProfileAdapter(
    private var items: List<FeedItem> = emptyList(),
    private val onPostAction: (Post, PostAction, View) -> Unit,
    private val onCommentAction: (Comment, CommentAction) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_POST = 1
        private const val TYPE_COMMENT = 2
    }

    // Used to display topic names on posts
    private var topicMap: Map<String, Topic> = emptyMap()

    fun setTopics(topics: List<Topic>) {
        this.topicMap = topics.associateBy { it.id }
        notifyDataSetChanged()
    }

    // Note: uses notifyDataSetChanged - consider DiffUtil for large datasets
    fun submitList(newItems: List<FeedItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is FeedItem.PostItem -> TYPE_POST
            is FeedItem.CommentItem -> TYPE_COMMENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_POST -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.post_item, parent, false)
                PostViewHolder(view, onPostAction)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.comment_item, parent, false)
                CommentViewHolder(view, onCommentAction)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is FeedItem.PostItem -> (holder as PostViewHolder).bind(item.post, topicMap)
            // false = not in detail mode (no nesting display)
            is FeedItem.CommentItem -> (holder as CommentViewHolder).bind(item.comment, false)
        }
    }

    override fun getItemCount(): Int = items.size
}
