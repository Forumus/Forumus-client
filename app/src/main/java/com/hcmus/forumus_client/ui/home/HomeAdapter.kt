package com.hcmus.forumus_client.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.model.PostAction
import com.hcmus.forumus_client.data.model.Topic
import com.hcmus.forumus_client.ui.common.PostViewHolder

/**
 * RecyclerView adapter for displaying posts in the home feed. Manages the list of posts and
 * delegates rendering to PostViewHolder. Supports showing a loading indicator at the bottom when
 * fetching more posts.
 *
 * @param items Initial list of posts
 * @param onActionClick Callback for post actions (upvote, downvote, reply, etc.)
 */
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

    /**
     * Determines the view type for a given position. Returns VIEW_TYPE_LOADING if showing loading
     * indicator, otherwise VIEW_TYPE_POST.
     */
    override fun getItemViewType(position: Int): Int {
        return if (isLoadingMore && position == items.size) {
            VIEW_TYPE_LOADING
        } else {
            VIEW_TYPE_POST
        }
    }

    /**
     * Creates a new ViewHolder instance based on the view type.
     *
     * @param parent The parent ViewGroup
     * @param viewType The view type (POST or LOADING)
     * @return New ViewHolder instance
     */
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

    /**
     * Binds data to the ViewHolder at the specified position.
     *
     * @param holder The ViewHolder to bind data to
     * @param position The position of the item in the list
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PostViewHolder) {
            holder.bind(items[position], topicMap)
        }
        // LoadingViewHolder doesn't need binding
    }

    /**
     * Returns the total number of items including the loading indicator if shown.
     *
     * @return Size of the items list plus 1 if loading indicator is shown
     */
    override fun getItemCount(): Int {
        return if (isLoadingMore) items.size + 1 else items.size
    }

    /**
     * Updates the adapter with a new list of posts.
     *
     * @param newItems The new list of posts to display
     */
    fun submitList(newItems: List<Post>) {
        items = newItems
        notifyDataSetChanged()
    }

    /**
     * Updates the adapter with the list of topics.
     *
     * @param topics The list of topics to map
     */
    fun setTopics(topics: List<Topic>) {
        topicMap = topics.associateBy { it.id }
        notifyDataSetChanged()
    }

    /**
     * Sets whether the loading indicator should be shown.
     *
     * @param isLoading True to show loading indicator, false to hide
     */
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

    /** ViewHolder for the loading indicator. */
    class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
