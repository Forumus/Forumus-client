package com.hcmus.forumus_client.ui.common

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
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

    // Original poster badge and reply context
    val opBadge: TextView = itemView.findViewById(R.id.opBadge)
    val replyContextContainer: LinearLayout = itemView.findViewById(R.id.replyContextContainer)
    val replyToUser: TextView = itemView.findViewById(R.id.replyToUser)

    // Threading support
    val indentationSpace: View = itemView.findViewById(R.id.indentationSpace)
    val threadLine: View = itemView.findViewById(R.id.threadLine)

    // Comment content view
    val contentText: TextView = itemView.findViewById(R.id.commentContent)

    // Voting views
    val upvoteIcon: ImageButton = itemView.findViewById(R.id.upvoteIcon)
    val upvoteCount: TextView = itemView.findViewById(R.id.upvoteCount)
    val downvoteIcon: ImageButton = itemView.findViewById(R.id.downvoteIcon)

    // Interaction views
    val replyButton: ImageButton = itemView.findViewById(R.id.replyButton)
    val replyCount: TextView = itemView.findViewById(R.id.replyCount)

    // View replies button
    val viewRepliesButton: LinearLayout = itemView.findViewById(R.id.viewRepliesButton)
    val viewRepliesText: TextView = itemView.findViewById(R.id.viewRepliesText)
    val viewRepliesChevron: ImageView = itemView.findViewById(R.id.viewRepliesChevron)

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
        // Apply indentation and thread line for nested comments in detail view
        if (isDetailMode && comment.parentCommentId != null) {
            // Show thread line for replies
            threadLine.visibility = View.VISIBLE
            
            // Calculate indentation based on nesting level (max 2 levels)
            val indentationDp = 24 // 24dp per level
            val indentationPx = (indentationDp * rootLayout.context.resources.displayMetrics.density).toInt()
            
            val layoutParams = indentationSpace.layoutParams
            layoutParams.width = indentationPx
            indentationSpace.layoutParams = layoutParams
        } else {
            threadLine.visibility = View.GONE
            val layoutParams = indentationSpace.layoutParams
            layoutParams.width = 0
            indentationSpace.layoutParams = layoutParams
        }

        // Bind author information (show role next to name with color)
        val name = comment.authorName.ifBlank { "Anonymous" }
        val roleLabel = when (comment.authorRole) {
            com.hcmus.forumus_client.data.model.UserRole.TEACHER -> "Teacher"
            com.hcmus.forumus_client.data.model.UserRole.ADMIN -> "Admin"
            else -> "Student"
        }
        val display = "$name  ·  $roleLabel"
        val spannable = SpannableString(display)
        val roleStart = display.indexOf('·').takeIf { it >= 0 }?.plus(2) ?: name.length
        val roleColorRes = when (comment.authorRole) {
            com.hcmus.forumus_client.data.model.UserRole.TEACHER -> R.color.role_teacher
            com.hcmus.forumus_client.data.model.UserRole.ADMIN -> R.color.role_admin
            else -> R.color.role_student
        }

        val roleColor = androidx.core.content.ContextCompat.getColor(itemView.context, roleColorRes)
        spannable.setSpan(ForegroundColorSpan(roleColor), roleStart, display.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        authorName.text = spannable
        timestamp.text = formatTimestamp(comment.createdAt)
        contentText.text = comment.content

        // Load author avatar with fallback
        authorAvatar.load(comment.authorAvatarUrl) {
            placeholder(R.drawable.default_avatar)
            error(R.drawable.default_avatar)
            crossfade(true)
            transformations(CircleCropTransformation())
        }

        // Show "OP" badge if comment is from the post author
        opBadge.visibility = if (comment.isOriginalPoster) View.VISIBLE else View.GONE

        // Show reply context if this comment is a reply
        if (comment.replyToUserName != null) {
            replyContextContainer.visibility = View.VISIBLE
            replyToUser.text = comment.replyToUserName
        } else {
            replyContextContainer.visibility = View.GONE
        }

        // Bind vote counts and apply vote UI
        upvoteCount.text = comment.upvoteCount.toString()
        applyVoteUI(comment)
        replyCount.text = comment.commentCount.toString()

        // Show/hide view replies button based on comment count and comment level
        // Only show for root-level comments (no parent) that have replies
        if (comment.commentCount > 0 && comment.parentCommentId == null) {
            viewRepliesButton.visibility = View.VISIBLE
            
            val resources = itemView.resources
            if (comment.isRepliesExpanded) {
                // Expanded state: "Hide x replies" and rotated arrow
                viewRepliesText.text = resources.getQuantityString(
                    R.plurals.hide_replies, 
                    comment.commentCount, 
                    comment.commentCount
                )
                viewRepliesChevron.rotation = 180f
            } else {
                // Collapsed state: "View x replies" and normal arrow
                viewRepliesText.text = resources.getQuantityString(
                    R.plurals.view_replies, 
                    comment.commentCount, 
                    comment.commentCount
                )
                viewRepliesChevron.rotation = 0f
            }
        } else {
            viewRepliesButton.visibility = View.GONE
        }


        // Set up click listeners for all interactive elements
        rootLayout.setOnClickListener { onActionClick(comment, CommentAction.OPEN) }
        upvoteIcon.setOnClickListener { 
            // Briefly disable to prevent duplicate taps during optimistic update
            if (upvoteIcon.isEnabled) {
                upvoteIcon.isEnabled = false
                downvoteIcon.isEnabled = false
                onActionClick(comment, CommentAction.UPVOTE)
                // Re-enable after brief delay
                upvoteIcon.postDelayed({
                    upvoteIcon.isEnabled = true
                    downvoteIcon.isEnabled = true
                }, 300)
            }
        }
        downvoteIcon.setOnClickListener { 
            // Briefly disable to prevent duplicate taps during optimistic update
            if (downvoteIcon.isEnabled) {
                upvoteIcon.isEnabled = false
                downvoteIcon.isEnabled = false
                onActionClick(comment, CommentAction.DOWNVOTE)
                // Re-enable after brief delay
                downvoteIcon.postDelayed({
                    upvoteIcon.isEnabled = true
                    downvoteIcon.isEnabled = true
                }, 300)
            }
        }
        replyButton.setOnClickListener { onActionClick(comment, CommentAction.REPLY) }
        viewRepliesButton.setOnClickListener { onActionClick(comment, CommentAction.VIEW_REPLIES) }
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
                    else -> SimpleDateFormat(itemView.context.getString(R.string.date_format_short_month_day), Locale.getDefault()).format(date)
                }
            } catch (e: Exception) {
                Log.e("CommentViewHolder", "Error formatting timestamp", e)
                "now"
            }
        } else {
            "now"
        }
    }

    /**
     * Update only vote-related UI elements without full rebinding.
     * Used for optimistic UI updates.
     */
    fun updateVotes(comment: Comment) {
        upvoteCount.text = comment.upvoteCount.toString()
        applyVoteUI(comment)
    }
}

