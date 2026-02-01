package com.hcmus.forumus_client.ui.post.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
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

/** Adapter for displaying a post and its comments. */
class PostDetailAdapter(
    private val onPostAction: (Post, PostAction, View) -> Unit,
    private val onCommentAction: (Comment, CommentAction) -> Unit,
) : ListAdapter<FeedItem, RecyclerView.ViewHolder>(FeedItemDiffCallback()) {

    companion object {
        private const val TYPE_POST = 1
        private const val TYPE_COMMENT = 2
    }

    private var topicMap: Map<String, Topic> = emptyMap()
    private var isSummaryLoading: Boolean = false

    fun setTopics(topics: List<Topic>) {
        val newTopicMap = topics.associateBy { it.id }
        if (newTopicMap != topicMap) {
            this.topicMap = newTopicMap
            if (currentList.isNotEmpty() && currentList[0] is FeedItem.PostItem) {
                notifyItemChanged(0, "topics")
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
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
        when (val item = getItem(position)) {
            is FeedItem.PostItem -> {
                (holder as PostViewHolder).bind(item.post, topicMap)
                holder.setSummaryLoading(isSummaryLoading)
            }
            is FeedItem.CommentItem -> (holder as CommentViewHolder).bind(item.comment, true)
        }
    }

    // Partial updates via payloads to avoid full rebind
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            val item = getItem(position)
            for (payload in payloads) {
                when (payload) {
                    "votes" -> {
                        when (item) {
                            is FeedItem.PostItem -> (holder as PostViewHolder).updateVotes(item.post)
                            is FeedItem.CommentItem -> (holder as CommentViewHolder).updateVotes(item.comment)
                        }
                    }
                    "summary_loading" -> {
                        if (holder is PostViewHolder && item is FeedItem.PostItem) {
                            holder.setSummaryLoading(isSummaryLoading)
                        }
                    }
                    "topics" -> {
                        if (holder is PostViewHolder && item is FeedItem.PostItem) {
                            holder.updateTopics(item.post, topicMap)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun setSummaryLoading(isLoading: Boolean) {
        isSummaryLoading = isLoading
        if (itemCount > 0 && getItem(0) is FeedItem.PostItem) {
            notifyItemChanged(0, "summary_loading")
        }
    }
}

class FeedItemDiffCallback : DiffUtil.ItemCallback<FeedItem>() {
    override fun areItemsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
        return when {
            oldItem is FeedItem.PostItem && newItem is FeedItem.PostItem ->
                oldItem.post.id == newItem.post.id
            oldItem is FeedItem.CommentItem && newItem is FeedItem.CommentItem ->
                oldItem.comment.id == newItem.comment.id
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: FeedItem, newItem: FeedItem): Any? {
        return when {
            oldItem is FeedItem.PostItem && newItem is FeedItem.PostItem -> {
                val old = oldItem.post
                val new = newItem.post
                when {
                    old.upvoteCount != new.upvoteCount ||
                    old.downvoteCount != new.downvoteCount ||
                    old.userVote != new.userVote -> "votes"
                    old.topicIds != new.topicIds -> "topics"
                    else -> null
                }
            }
            oldItem is FeedItem.CommentItem && newItem is FeedItem.CommentItem -> {
                val old = oldItem.comment
                val new = newItem.comment
                if (old.upvoteCount != new.upvoteCount ||
                    old.downvoteCount != new.downvoteCount ||
                    old.userVote != new.userVote) "votes" else null
            }
            else -> null
        }
    }
}
