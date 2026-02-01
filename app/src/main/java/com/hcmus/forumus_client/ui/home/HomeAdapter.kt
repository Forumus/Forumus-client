package com.hcmus.forumus_client.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.model.PostAction
import com.hcmus.forumus_client.data.model.Topic
import com.hcmus.forumus_client.ui.common.PostViewHolder

/** RecyclerView adapter for displaying posts in the home feed. */
class HomeAdapter(
        private var items: List<Post>,
        private val onActionClick: (Post, PostAction, View) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_POST = 0
        private const val VIEW_TYPE_LOADING = 1
    }

    // Map of topic id to Topic object
    private var topicMap: Map<String, Topic> = emptyMap()

    // Flag to show loading indicator
    private var isLoadingMore = false

    // Track which post is currently loading AI summary
    private var summaryLoadingPostId: String? = null

    /** Determines the view type for a position. */
    override fun getItemViewType(position: Int): Int {
        return if (isLoadingMore && position == items.size) {
            VIEW_TYPE_LOADING
        } else {
            VIEW_TYPE_POST
        }
    }

    /** Creates a ViewHolder based on view type. */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_LOADING) {
            val view =
                    LayoutInflater.from(parent.context)
                            .inflate(R.layout.item_loading, parent, false)
            LoadingViewHolder(view)
        } else {
            val view =
                    LayoutInflater.from(parent.context).inflate(R.layout.post_item, parent, false)
            PostViewHolder(view, onActionClick)
        }
    }

    /** Binds data to the ViewHolder. */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PostViewHolder) {
            holder.bind(items[position], topicMap)
            holder.setSummaryLoading(items[position].id == summaryLoadingPostId)
        }
        // LoadingViewHolder doesn't need binding
    }

    /** Binds data with payloads for partial updates. */
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            // Full bind
            super.onBindViewHolder(holder, position, payloads)
        } else if (holder is PostViewHolder) {
            val post = items[position]
            // Handle partial updates
            for (payload in payloads) {
                when (payload) {
                    "votes" -> holder.updateVotes(post)
                    "summary_loading" -> holder.setSummaryLoading(post.id == summaryLoadingPostId)
                    "topics" -> holder.updateTopics(post, topicMap)
                    else -> {}
                }
            }
        }
    }

    /** Returns total item count including loading indicator. */
    override fun getItemCount(): Int {
        return if (isLoadingMore) items.size + 1 else items.size
    }

    /** Updates the list using DiffUtil. */
    fun submitList(newItems: List<Post>) {
        val oldItems = items
        val diffCallback = PostDiffCallback(oldItems, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    /** Updates the topic map. */
    fun setTopics(topics: List<Topic>) {
        val newTopicMap = topics.associateBy { it.id }
        // Only update if topics actually changed
        if (newTopicMap != topicMap) {
            topicMap = newTopicMap
            // Notify items to rebind with new topic data
            notifyItemRangeChanged(0, items.size, "topics")
        }
    }

    /** Sets loading indicator visibility. */
    fun setLoadingMore(isLoading: Boolean) {
        val wasLoading = isLoadingMore
        isLoadingMore = isLoading

        if (isLoading && !wasLoading) {
            // Show loading indicator
            notifyItemInserted(items.size)
        } else if (!isLoading && wasLoading) {
            // Hide loading indicator
            notifyItemRemoved(items.size)
        }
    }

    /** Sets the post ID loading AI summary. */
    fun setSummaryLoadingPostId(postId: String?) {
        val oldId = summaryLoadingPostId
        summaryLoadingPostId = postId

        // Notify items that need to update their loading state
        if (oldId != null) {
            val oldIndex = items.indexOfFirst { it.id == oldId }
            if (oldIndex >= 0) notifyItemChanged(oldIndex, "summary_loading")
        }
        if (postId != null) {
            val newIndex = items.indexOfFirst { it.id == postId }
            if (newIndex >= 0) notifyItemChanged(newIndex, "summary_loading")
        }
    }

    /** ViewHolder for the loading indicator. */
    class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view)

    /** DiffUtil callback for calculating list differences. */
    private class PostDiffCallback(
        private val oldList: List<Post>,
        private val newList: List<Post>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val old = oldList[oldItemPosition]
            val new = newList[newItemPosition]
            
            // Compare all fields that affect UI display
            return old.title == new.title &&
                   old.content == new.content &&
                   old.upvoteCount == new.upvoteCount &&
                   old.downvoteCount == new.downvoteCount &&
                   old.commentCount == new.commentCount &&
                   old.userVote == new.userVote &&
                   old.imageUrls == new.imageUrls &&
                   old.videoUrls == new.videoUrls &&
                   old.topicIds == new.topicIds &&
                   old.authorName == new.authorName &&
                   old.authorAvatarUrl == new.authorAvatarUrl
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            val old = oldList[oldItemPosition]
            val new = newList[newItemPosition]
            
            // Return specific payload for targeted updates
            return when {
                old.upvoteCount != new.upvoteCount ||
                old.downvoteCount != new.downvoteCount ||
                old.userVote != new.userVote -> "votes"
                old.topicIds != new.topicIds -> "topics"
                else -> null
            }
        }
    }
}
