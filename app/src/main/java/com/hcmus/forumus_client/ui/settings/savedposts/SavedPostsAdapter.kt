package com.hcmus.forumus_client.ui.settings.savedposts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.model.PostAction
import com.hcmus.forumus_client.ui.common.PostViewHolder

/**
 * RecyclerView adapter for displaying saved posts.
 * Reuses PostViewHolder for consistent post display across the app.
 *
 * @param items Initial list of saved posts
 * @param onActionClick Callback for post actions (upvote, downvote, reply, etc.)
 */
class SavedPostsAdapter(
    private var items: List<Post>,
    private val onActionClick: (Post, PostAction, View) -> Unit
) : RecyclerView.Adapter<PostViewHolder>() {

    /**
     * Creates a new PostViewHolder instance.
     *
     * @param parent The parent ViewGroup
     * @param viewType The view type
     * @return New PostViewHolder instance
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.post_item, parent, false)
        return PostViewHolder(view, onActionClick)
    }

    /**
     * Binds data to the ViewHolder at the specified position.
     *
     * @param holder The ViewHolder to bind data to
     * @param position The position of the item in the list
     */
    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(items[position])
    }

    /**
     * Returns the total number of items.
     *
     * @return Size of the items list
     */
    override fun getItemCount(): Int = items.size

    /**
     * Updates the adapter with a new list of posts.
     *
     * @param newItems The new list of saved posts to display
     */
    fun submitList(newItems: List<Post>) {
        items = newItems
        notifyDataSetChanged()
    }
}
