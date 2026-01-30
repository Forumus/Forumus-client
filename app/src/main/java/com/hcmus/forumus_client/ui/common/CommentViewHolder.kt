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

class CommentViewHolder(
    itemView: View,
    private val onActionClick: (Comment, CommentAction) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    val authorAvatar: ImageView = itemView.findViewById(R.id.authorAvatar)
    val authorName: TextView = itemView.findViewById(R.id.authorName)
    val timestamp: TextView = itemView.findViewById(R.id.timestamp)

    val opBadge: TextView = itemView.findViewById(R.id.opBadge)
    val replyContextContainer: LinearLayout = itemView.findViewById(R.id.replyContextContainer)
    val replyToUser: TextView = itemView.findViewById(R.id.replyToUser)

    val indentationSpace: View = itemView.findViewById(R.id.indentationSpace)
    val threadLine: View = itemView.findViewById(R.id.threadLine)

    val contentText: TextView = itemView.findViewById(R.id.commentContent)

    val upvoteIcon: ImageButton = itemView.findViewById(R.id.upvoteIcon)
    val upvoteCount: TextView = itemView.findViewById(R.id.upvoteCount)
    val downvoteIcon: ImageButton = itemView.findViewById(R.id.downvoteIcon)

    val replyButton: ImageButton = itemView.findViewById(R.id.replyButton)
    val replyCount: TextView = itemView.findViewById(R.id.replyCount)

    val viewRepliesButton: LinearLayout = itemView.findViewById(R.id.viewRepliesButton)
    val viewRepliesText: TextView = itemView.findViewById(R.id.viewRepliesText)
    val viewRepliesChevron: ImageView = itemView.findViewById(R.id.viewRepliesChevron)

    val rootLayout: LinearLayout = itemView.findViewById(R.id.commentItem)

    fun bind(comment: Comment, isDetailMode: Boolean) {
        if (isDetailMode && comment.parentCommentId != null) {
            threadLine.visibility = View.VISIBLE
            
            val indentationDp = 24
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

        authorAvatar.load(comment.authorAvatarUrl) {
            placeholder(R.drawable.default_avatar)
            error(R.drawable.default_avatar)
            crossfade(true)
            transformations(CircleCropTransformation())
        }

        opBadge.visibility = if (comment.isOriginalPoster) View.VISIBLE else View.GONE

        if (comment.replyToUserName != null) {
            replyContextContainer.visibility = View.VISIBLE
            replyToUser.text = comment.replyToUserName
        } else {
            replyContextContainer.visibility = View.GONE
        }

        upvoteCount.text = comment.upvoteCount.toString()
        applyVoteUI(comment)
        replyCount.text = comment.commentCount.toString()

        // Only root-level comments with replies show the expand button
        if (comment.commentCount > 0 && comment.parentCommentId == null) {
            viewRepliesButton.visibility = View.VISIBLE
            
            val resources = itemView.resources
            if (comment.isRepliesExpanded) {
                viewRepliesText.text = resources.getQuantityString(
                    R.plurals.hide_replies, 
                    comment.commentCount, 
                    comment.commentCount
                )
                viewRepliesChevron.rotation = 180f
            } else {
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

        rootLayout.setOnClickListener { onActionClick(comment, CommentAction.OPEN) }
        upvoteIcon.setOnClickListener { 
            if (upvoteIcon.isEnabled) {
                upvoteIcon.isEnabled = false
                downvoteIcon.isEnabled = false
                onActionClick(comment, CommentAction.UPVOTE)
                upvoteIcon.postDelayed({
                    upvoteIcon.isEnabled = true
                    downvoteIcon.isEnabled = true
                }, 300)
            }
        }
        downvoteIcon.setOnClickListener { 
            if (downvoteIcon.isEnabled) {
                upvoteIcon.isEnabled = false
                downvoteIcon.isEnabled = false
                onActionClick(comment, CommentAction.DOWNVOTE)
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

    fun updateVotes(comment: Comment) {
        upvoteCount.text = comment.upvoteCount.toString()
        applyVoteUI(comment)
    }
}

