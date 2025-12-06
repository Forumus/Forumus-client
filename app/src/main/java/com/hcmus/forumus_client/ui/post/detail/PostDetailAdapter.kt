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
    private var items: List<FeedItem> = emptyList(),
    private val onPostAction: (Post, PostAction) -> Unit,
    private val onCommentAction: (Comment, CommentAction) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        // View type constants for determining which ViewHolder to use
        private const val TYPE_POST = 1
        private const val TYPE_COMMENT = 2
    }

    /**
     * Updates the adapter with a new list of items and refreshes the entire view.
     *
     * This is a simple update mechanism - for large datasets consider using
     * DiffUtil for more efficient updates.
     *
     * @param newItems The new list of feed items to display
     */
    fun submitList(newItems: List<FeedItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    /**
     * Returns the view type for the item at the specified position.
     *
     * @param position The position of the item in the adapter
     * @return TYPE_POST if item is a PostItem, TYPE_COMMENT if item is a CommentItem
     */
    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
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
        when (val item = items[position]) {
            is FeedItem.PostItem -> (holder as PostViewHolder).bind(item.post)
            is FeedItem.CommentItem -> (holder as CommentViewHolder).bind(item.comment, true)
        }
    }

    /**
     * Returns the total number of items in the adapter.
     *
     * @return The size of the items list
     */
    override fun getItemCount(): Int = items.size
}
