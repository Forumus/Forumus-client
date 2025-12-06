package com.hcmus.forumus_client.ui.common

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.google.firebase.Timestamp
import com.hcmus.forumus_client.data.model.CommentAction
import com.hcmus.forumus_client.data.model.Comment
import com.hcmus.forumus_client.data.model.VoteState
import java.text.SimpleDateFormat
import java.util.Locale
import com.hcmus.forumus_client.R

/**
 * ViewHolder for displaying a comment in a RecyclerView.
 * Supports both feed mode (flat) and detail mode (threaded with indentation).
 * Handles binding comment data to UI elements and managing user interactions.
 *
 * @param itemView The inflated layout view for a single comment item
 * @param onActionClick Callback invoked when user performs actions on the comment
 */
class CommentViewHolder(
    itemView: View,
    private val onActionClick: (Comment, CommentAction) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    // Author information views
    val authorAvatar: ImageView = itemView.findViewById(R.id.authorAvatar)
    val authorName: TextView = itemView.findViewById(R.id.authorName)
    val timestamp: TextView = itemView.findViewById(R.id.timestamp)

    // Original poster and reply metadata views
    val tvOriginalPoster: TextView = itemView.findViewById(R.id.tvOriginalPoster)
    val tvReplyText: TextView = itemView.findViewById(R.id.tvReplyText)
    val replyToUser: TextView = itemView.findViewById(R.id.replyToUser)

    // Comment content view
    val contentText: TextView = itemView.findViewById(R.id.postContent)

    // Voting views
    val upvoteIcon: ImageButton = itemView.findViewById(R.id.upvoteIcon)
    val upvoteCount: TextView = itemView.findViewById(R.id.upvoteCount)
    val downvoteIcon: ImageButton = itemView.findViewById(R.id.downvoteIcon)

    // Interaction views
    val replyButton: LinearLayout = itemView.findViewById(R.id.replyButton)
    val replyCount: TextView = itemView.findViewById(R.id.replyCount)

    // Root view for click handling and layout adjustments
    val rootLayout: LinearLayout = itemView.findViewById(R.id.commentItem)

    /**
     * Binds comment data to UI elements and sets up click listeners.
     * Applies indentation and styling based on comment hierarchy and display mode.
     *
     * @param comment The comment data to display
     * @param isDetailMode If true, applies indentation for nested replies
     */
    fun bind(comment: Comment, isDetailMode: Boolean) {
        // Apply indentation for nested comments in detail view
        if (isDetailMode) {
            val paddingStartDp = if (comment.parentCommentId != null) 47 else 15
            val paddingStartPx = (paddingStartDp * rootLayout.context.resources.displayMetrics.density).toInt()

            rootLayout.setPadding(
                paddingStartPx,
                rootLayout.paddingTop,
                rootLayout.paddingRight,
                rootLayout.paddingBottom
            )

            // Remove bottom margin for detail view (replies shown directly below)
            val layoutParams = rootLayout.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.bottomMargin = 0
            rootLayout.layoutParams = layoutParams
        }

        // Bind author information
        authorName.text = comment.authorName.ifBlank { "Anonymous" }
        timestamp.text = formatTimestamp(comment.createdAt)
        contentText.text = comment.content

        // Load author avatar with fallback
        authorAvatar.load(comment.authorAvatarUrl) {
            placeholder(R.drawable.default_avatar)
            error(R.drawable.default_avatar)
            crossfade(true)
            transformations(CircleCropTransformation())
        }

        // Show "Original Poster" badge if comment is from the post author
        tvOriginalPoster.visibility = if (comment.isOriginalPoster) View.VISIBLE else View.GONE

        // Show reply-to information if this comment is a reply
        if (comment.replyToUserName != null) {
            tvReplyText.visibility = View.VISIBLE
            replyToUser.visibility = View.VISIBLE
            replyToUser.text = comment.replyToUserName
        } else {
            tvReplyText.visibility = View.GONE
            replyToUser.visibility = View.GONE
        }

        // Bind vote counts and apply vote UI
        upvoteCount.text = comment.upvoteCount.toString()
        applyVoteUI(comment)
        replyCount.text = comment.commentCount.toString()

        // Set up click listeners for all interactive elements
        rootLayout.setOnClickListener { onActionClick(comment, CommentAction.OPEN) }
        upvoteIcon.setOnClickListener { onActionClick(comment, CommentAction.UPVOTE) }
        downvoteIcon.setOnClickListener { onActionClick(comment, CommentAction.DOWNVOTE) }
        replyButton.setOnClickListener { onActionClick(comment, CommentAction.REPLY) }
        authorAvatar.setOnClickListener { onActionClick(comment, CommentAction.AUTHOR_PROFILE) }
        authorName.setOnClickListener { onActionClick(comment, CommentAction.AUTHOR_PROFILE) }
        replyToUser.setOnClickListener { onActionClick(comment, CommentAction.REPLIED_USER_PROFILE) }
    }

    /**
     * Updates vote icons based on the current user's vote state.
     *
     * @param comment The comment with the user's vote state
     */
    private fun applyVoteUI(comment: Comment) {
        when (comment.userVote) {
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
                Log.e("CommentViewHolder", "Error formatting timestamp", e)
                "now"
            }
        } else {
            "now"
        }
    }
}

