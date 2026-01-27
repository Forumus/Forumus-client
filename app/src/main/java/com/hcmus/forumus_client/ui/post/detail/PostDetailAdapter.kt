package com.hcmus.forumus_client.ui.post.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
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
 * RecyclerView adapter that displays a mixed list of posts and comments in post detail.
 *
 * Supports two item types:
 * - PostItem: Shows post content with voting capabilities
 * - CommentItem: Shows comment content with voting and detail mode for nesting
 *
 * Uses callbacks to handle user interactions (upvote, downvote, open details, etc.)
 * and delegates rendering to specialized ViewHolder classes (PostViewHolder, CommentViewHolder).
 *
 * @param items Initial list of feed items (posts and comments)
 * @param onPostAction Callback when user interacts with a post
 * @param onCommentAction Callback when user interacts with a comment
 */
class PostDetailAdapter(
    private val onPostAction: (Post, PostAction, View) -> Unit,
    private val onCommentAction: (Comment, CommentAction) -> Unit,
) : androidx.recyclerview.widget.ListAdapter<FeedItem, RecyclerView.ViewHolder>(FeedItemDiffCallback()) {

    companion object {
        // View type constants for determining which ViewHolder to use
        private const val TYPE_POST = 1
        private const val TYPE_COMMENT = 2
    }

    // Map of topic id to Topic object
    private var topicMap: Map<String, Topic> = emptyMap()

    // Track if AI summary is loading for the post
    private var isSummaryLoading: Boolean = false

    /**
     * Updates the adapter with the list of topics.
     * Only triggers update if topics actually changed to prevent redundant re-renders.
     *
     * @param topics The list of topics to map
     */
    fun setTopics(topics: List<Topic>) {
        val newTopicMap = topics.associateBy { it.id }
        // Only update if topics actually changed
        if (newTopicMap != topicMap) {
            this.topicMap = newTopicMap
            // Only notify the post item (always at position 0) with topics payload
            if (items.isNotEmpty() && items[0] is FeedItem.PostItem) {
                notifyItemChanged(0, "topics")
            }
        }
    }

    /**
     * Updates the adapter with a new list of items using DiffUtil for efficient updates.
     * This prevents unnecessary re-renders and maintains scroll position.
     *
     * @param newItems The new list of feed items to display
     */
    fun submitList(newItems: List<FeedItem>) {
        val oldItems = items
        val diffCallback = FeedItemDiffCallback(oldItems, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    /**
     * Returns the view type for the item at the specified position.
     *
     * @param position The position of the item in the adapter
     * @return TYPE_POST if item is a PostItem, TYPE_COMMENT if item is a CommentItem
     */
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is FeedItem.PostItem -> TYPE_POST
            is FeedItem.CommentItem -> TYPE_COMMENT
        }
    }

    /**
     * Creates the appropriate ViewHolder based on the item view type.
     *
     * Inflates the corresponding layout and wraps it with the proper ViewHolder class.
     * Attaches the action callbacks to enable communication between ViewHolder and Activity.
     *
     * @param parent The parent ViewGroup
     * @param viewType The type of view to create (TYPE_POST or TYPE_COMMENT)
     * @return The created ViewHolder (PostViewHolder or CommentViewHolder)
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_POST -> {
                // Inflate post item layout and create PostViewHolder
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.post_item, parent, false)
                PostViewHolder(view, onPostAction)
            }
            else -> {
                // Inflate comment item layout and create CommentViewHolder
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.comment_item, parent, false)
                CommentViewHolder(view, onCommentAction)
            }
        }
    }

    /**
     * Binds the data at the specified position to the corresponding ViewHolder.
     *
     * Extracts the appropriate data object from the FeedItem and calls the
     * ViewHolder's bind method to update the views.
     *
     * @param holder The ViewHolder to bind data to
     * @param position The position of the item in the adapter
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is FeedItem.PostItem -> {
                (holder as PostViewHolder).bind(item.post, topicMap)
                holder.setSummaryLoading(isSummaryLoading)
            }
            is FeedItem.CommentItem -> (holder as CommentViewHolder).bind(item.comment, true)
        }
    }

    /**
     * Binds data with payloads for partial updates.
     * This prevents full item rebinding when only specific fields change.
     */
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            // Full bind
            super.onBindViewHolder(holder, position, payloads)
        } else {
            val item = items[position]
            // Handle partial updates
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

    /**
     * Returns the total number of items in the adapter.
     *
     * @return The size of the items list
     */
    override fun getItemCount(): Int = items.size

    /**
     * Updates the AI summary loading state for the post.
     * Since there's only one post at position 0, we notify that item.
     *
     * @param isLoading True if summary is being fetched, false otherwise
     */
    fun setSummaryLoading(isLoading: Boolean) {
        isSummaryLoading = isLoading
        // Post is always at position 0, notify to update loading state
        if (itemCount > 0 && getItem(0) is FeedItem.PostItem) {
            notifyItemChanged(0, "summary_loading")
        }
    }

    /**
     * DiffUtil callback for calculating the difference between two lists of feed items.
     * This enables efficient, targeted updates instead of full list redraws.
     */
    private class FeedItemDiffCallback(
        private val oldList: List<FeedItem>,
        private val newList: List<FeedItem>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            
            return when {
                oldItem is FeedItem.PostItem && newItem is FeedItem.PostItem ->
                    oldItem.post.id == newItem.post.id
                oldItem is FeedItem.CommentItem && newItem is FeedItem.CommentItem ->
                    oldItem.comment.id == newItem.comment.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            
            return when {
                oldItem is FeedItem.PostItem && newItem is FeedItem.PostItem -> {
                    val old = oldItem.post
                    val new = newItem.post
                    old.title == new.title &&
                    old.content == new.content &&
                    old.upvoteCount == new.upvoteCount &&
                    old.downvoteCount == new.downvoteCount &&
                    old.commentCount == new.commentCount &&
                    old.userVote == new.userVote &&
                    old.topicIds == new.topicIds
                }
                oldItem is FeedItem.CommentItem && newItem is FeedItem.CommentItem -> {
                    val old = oldItem.comment
                    val new = newItem.comment
                    old.content == new.content &&
                    old.upvoteCount == new.upvoteCount &&
                    old.downvoteCount == new.downvoteCount &&
                    old.commentCount == new.commentCount &&
                    old.userVote == new.userVote
                }
                else -> false
            }
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            
            // Return specific payload for targeted updates
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
}

class FeedItemDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<FeedItem>() {
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
}
