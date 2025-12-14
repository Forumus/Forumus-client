package com.hcmus.forumus_client.ui.common

import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.google.firebase.Timestamp
import com.hcmus.forumus_client.data.model.PostAction
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.model.VoteState
import java.text.SimpleDateFormat
import java.util.Locale
import com.hcmus.forumus_client.R

/**
 * ViewHolder for displaying a post in a RecyclerView.
 * Handles binding post data to UI elements and managing user interactions.
 *
 * @param itemView The inflated layout view for a single post item
 * @param onActionClick Callback invoked when user performs actions on the post
 */
class PostViewHolder(
    itemView: View,
    private val onActionClick: (Post, PostAction, View) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    // Author information views
    val authorAvatar: ImageView = itemView.findViewById(R.id.authorAvatar)
    val authorName: TextView = itemView.findViewById(R.id.authorName)
    val timestamp: TextView = itemView.findViewById(R.id.timestamp)

    // Post content views
    val postTitle: TextView = itemView.findViewById(R.id.postTitle)
    val postContent: TextView = itemView.findViewById(R.id.postContent)

    // Voting views
    val upvoteIcon: ImageButton = itemView.findViewById(R.id.upvoteIcon)
    val upvoteCount: TextView = itemView.findViewById(R.id.upvoteCount)
    val downvoteIcon: ImageButton = itemView.findViewById(R.id.downvoteIcon)

    // Interaction views
    val replyButton: LinearLayout = itemView.findViewById(R.id.replyButton)
    val replyCount: TextView = itemView.findViewById(R.id.replyCount)
    val shareButton: LinearLayout = itemView.findViewById(R.id.shareButton)
    val menuButton: ImageButton = itemView.findViewById(R.id.menuButton)

    // Root view for click handling
    val rootLayout: LinearLayout = itemView.findViewById(R.id.postItem)
    val topicContainer: LinearLayout = itemView.findViewById(R.id.topicContainer)

    /**
     * Binds post data to UI elements and sets up click listeners.
     *
     * @param post The post data to display
     * @param topicMap Map of topic IDs to names
     */
    fun bind(post: Post, topicMap: Map<String, com.hcmus.forumus_client.data.model.Topic>? = null) {
        // Bind author information
        authorName.text = post.authorName.ifBlank { "Anonymous" }
        timestamp.text = formatTimestamp(post.createdAt)

        // Bind post content
        postTitle.text = post.title
        postContent.text = post.content

        // Bind vote counts
        upvoteCount.text = post.upvoteCount.toString()
        replyCount.text = post.commentCount.toString()

        // Load author avatar with fallback
        authorAvatar.load(post.authorAvatarUrl) {
            crossfade(true)
            placeholder(R.drawable.default_avatar)
            error(R.drawable.default_avatar)
            transformations(CircleCropTransformation())
        }

        // Apply visual feedback for user's vote state
        applyVoteUI(post)

        // Bind topic tags
        topicContainer.removeAllViews()
        if (topicMap != null && post.topicIds.isNotEmpty()) {
            val density = itemView.resources.displayMetrics.density
            val marginEnd = (8 * density).toInt()
            val paddingH = (12 * density).toInt()
            val paddingV = (6 * density).toInt()

            post.topicIds.take(5).forEach { topicId ->
                val topic = topicMap[topicId]
                val topicName = topic?.name ?: topicId
                
                // Colors
                val defaultColor = android.graphics.Color.parseColor("#4285F4") // Default Blue
                var mainColor = defaultColor
                var alpha = 0.1
                
                if (topic != null) {
                    try {
                        if (topic.fillColor.isNotEmpty()) {
                             mainColor = android.graphics.Color.parseColor(topic.fillColor)
                        }
                        alpha = topic.fillAlpha
                        // Clamp alpha
                        if (alpha < 0.0) alpha = 0.0
                        if (alpha > 1.0) alpha = 1.0
                        
                        android.util.Log.d("PostViewHolder", "Topic: ${topic.name}, Color: ${topic.fillColor}, Alpha: ${topic.fillAlpha}")
                    } catch (e: Exception) {
                        android.util.Log.e("PostViewHolder", "Color parsing error for topic ${topic.name}", e)
                    }
                }

                // Calculate background color with alpha
                val alphaInt = (alpha * 255).toInt()
                val backgroundColor = (alphaInt shl 24) or (mainColor and 0x00FFFFFF)

                val backgroundDrawable = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = 16 * density
                    setColor(backgroundColor)
                    // No stroke for this new design
                }

                val textView = TextView(itemView.context).apply {
                    text = topicName
                    textSize = 12f
                    setTextColor(mainColor) // Text takes the main (solid) color
                    background = backgroundDrawable
                    setPadding(paddingH, paddingV, paddingH, paddingV)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, marginEnd, 0)
                    }
                }
                topicContainer.addView(textView)
            }
        }

        // Set up click listeners for all interactive elements
        rootLayout.setOnClickListener { onActionClick(post, PostAction.OPEN, it) }
        upvoteIcon.setOnClickListener { onActionClick(post, PostAction.UPVOTE, it) }
        downvoteIcon.setOnClickListener { onActionClick(post, PostAction.DOWNVOTE, it) }
        replyButton.setOnClickListener { onActionClick(post, PostAction.REPLY, it) }
        shareButton.setOnClickListener { onActionClick(post, PostAction.SHARE, it) }
        authorAvatar.setOnClickListener { onActionClick(post, PostAction.AUTHOR_PROFILE, it) }
        authorName.setOnClickListener { onActionClick(post, PostAction.AUTHOR_PROFILE, it) }
        menuButton.setOnClickListener { onActionClick(post, PostAction.MENU, it) }
    }

    /**
     * Updates vote icons based on the current user's vote state.
     *
     * @param post The post with the user's vote state
     */
    private fun applyVoteUI(post: Post) {
        when (post.userVote) {
            VoteState.UPVOTE -> {
                upvoteIcon.setImageResource(R.drawable.ic_upvote_filled)
                downvoteIcon.setImageResource(R.drawable.ic_downvote)
            }
            VoteState.DOWNVOTE -> {
                upvoteIcon.setImageResource(R.drawable.ic_upvote)
                downvoteIcon.setImageResource(R.drawable.ic_downvote_filled)
            }
            VoteState.NONE -> {
                upvoteIcon.setImageResource(R.drawable.ic_upvote)
                downvoteIcon.setImageResource(R.drawable.ic_downvote)
            }
        }
    }

    /**
     * Formats a Firestore timestamp into a human-readable relative time string.
     * Examples: "now", "5m", "2h", "3d", "Jan 15"
     *
     * @param timestamp The Firestore timestamp to format
     * @return Formatted time string, or "now" if timestamp is null or invalid
     */
    private fun formatTimestamp(timestamp: Timestamp?): String {
        return if (timestamp != null) {
            try {
                val date = timestamp.toDate()
                val now = System.currentTimeMillis()
                val diffMs = now - date.time

                when {
                    diffMs < 60 * 1000 -> "now"
                    diffMs < 60 * 60 * 1000 -> "${diffMs / (60 * 1000)}m"
                    diffMs < 24 * 60 * 60 * 1000 -> "${diffMs / (60 * 60 * 1000)}h"
                    diffMs < 7 * 24 * 60 * 60 * 1000 -> "${diffMs / (24 * 60 * 60 * 1000)}d"
                    else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
                }
            } catch (e: Exception) {
                Log.e("PostViewHolder", "Error formatting timestamp", e)
                "now"
            }
        } else {
            "now"
        }
    }
}
