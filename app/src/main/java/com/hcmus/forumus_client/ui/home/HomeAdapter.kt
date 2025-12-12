package com.hcmus.forumus_client.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.model.PostAction
import com.hcmus.forumus_client.ui.common.PostViewHolder
import android.view.View

/**
 * RecyclerView adapter for displaying posts in the home feed.
 * Manages the list of posts and delegates rendering to PostViewHolder.
 *
 * @param items Initial list of posts
 * @param onActionClick Callback for post actions (upvote, downvote, reply, etc.)
 */
class HomeAdapter(
    private var items: List<Post>,
    private val onActionClick: (Post, PostAction, View) -> Unit
) : RecyclerView.Adapter<PostViewHolder>() {


    // Map of topic id to topic name
    private var topicMap: Map<String, String> = emptyMap()

    /**
     * Creates a new PostViewHolder instance for each post item.
     *
     * @param parent The parent ViewGroup
     * @param viewType The view type (not used in this adapter)
     * @return New PostViewHolder instance
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.post_item, parent, false)
        return PostViewHolder(view, onActionClick)
    }

    /**
     * Binds post data to the ViewHolder at the specified position.
     *
     * @param holder The PostViewHolder to bind data to
     * @param position The position of the post in the list
     */
    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(items[position], topicMap)
    }

    /**
     * Returns the total number of posts in the adapter.
     *
     * @return Size of the items list
     */
    override fun getItemCount() = items.size

    /**
     * Updates the adapter with a new list of posts.
     * Uses notifyDataSetChanged for simplicity (consider DiffUtil for large lists).
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
    fun setTopics(topics: List<com.hcmus.forumus_client.data.model.Topic>) {
        topicMap = topics.associate { it.id to it.name }
        notifyDataSetChanged()
    }
}