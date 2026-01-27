package com.hcmus.forumus_client.ui.post.detail

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
     *
     * @param topics The list of topics to map
     */
    fun setTopics(topics: List<Topic>) {
        this.topicMap = topics.associateBy { it.id }
        notifyDataSetChanged()
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
