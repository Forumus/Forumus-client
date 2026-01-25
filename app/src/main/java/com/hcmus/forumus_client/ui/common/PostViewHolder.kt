package com.hcmus.forumus_client.ui.common

import android.util.Log
import android.view.View
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import android.content.Intent
import com.google.firebase.Timestamp
import com.hcmus.forumus_client.data.model.PostAction
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.model.VoteState
import java.text.SimpleDateFormat
import java.util.Locale
import com.hcmus.forumus_client.R
import kotlin.math.min
import com.hcmus.forumus_client.data.model.Topic
import androidx.core.graphics.toColorInt

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

    // AI Summary views
    val summaryButton: LinearLayout = itemView.findViewById(R.id.summaryButton)
    val summaryLoadingContainer: FrameLayout = itemView.findViewById(R.id.summaryLoadingContainer)

    // Root view for click handling
    val rootLayout: LinearLayout = itemView.findViewById(R.id.postItem)

    // RecyclerView for displaying post media (ảnh + video)
    private val rvPostImages: RecyclerView = itemView.findViewById(R.id.rvPostImages)

    val topicContainer: LinearLayout = itemView.findViewById(R.id.topicContainer)
    val imagesAdapter = PostMediaAdapter()

    var imagesLayoutManager: GridLayoutManager? = null

    // Expandable text state
    private var isContentExpanded = false
    private var fullContent = ""
    private val maxCollapsedLines = 4
    private val showMoreText = " Show more..."

    init {
        rvPostImages.adapter = imagesAdapter
    }

    fun bind(post: Post, topicMap: Map<String, Topic>? = null) {
        // Bind author information (show role next to name with color)
        val name = post.authorName.ifBlank { "Anonymous" }
        val roleLabel = when (post.authorRole) {
            com.hcmus.forumus_client.data.model.UserRole.TEACHER -> "Teacher"
            com.hcmus.forumus_client.data.model.UserRole.ADMIN -> "Admin"
            else -> "Student"
        }
        val display = "$name  ·  $roleLabel"
        val spannable = SpannableString(display)
        val roleStart = display.indexOf('·').takeIf { it >= 0 }?.plus(2) ?: name.length
        val roleColorRes = when (post.authorRole) {
            com.hcmus.forumus_client.data.model.UserRole.TEACHER -> R.color.role_teacher
            com.hcmus.forumus_client.data.model.UserRole.ADMIN -> R.color.role_admin
            else -> R.color.role_student
        }

        val roleColor = androidx.core.content.ContextCompat.getColor(itemView.context, roleColorRes)
        spannable.setSpan(ForegroundColorSpan(roleColor), roleStart, display.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        authorName.text = spannable
        timestamp.text = formatTimestamp(post.createdAt)

        // Bind post content
        postTitle.text = post.title
        
        // Reset expansion state when binding new content
        isContentExpanded = false
        fullContent = post.content
        setupExpandableContent()

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

        // Set up media (ảnh + video)
        setupMedia(post)

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
                val defaultColor = "#4285F4".toColorInt() // Default Blue
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

                        android.util.Log.d(
                            "PostViewHolder",
                            "Topic: ${topic.name}, Color: ${topic.fillColor}, Alpha: ${topic.fillAlpha}"
                        )
                    } catch (e: Exception) {
                        android.util.Log.e(
                            "PostViewHolder",
                            "Color parsing error for topic ${topic.name}",
                            e
                        )
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
        summaryButton.setOnClickListener { onActionClick(post, PostAction.SUMMARY, it) }
        authorAvatar.setOnClickListener { onActionClick(post, PostAction.AUTHOR_PROFILE, it) }
        authorName.setOnClickListener { onActionClick(post, PostAction.AUTHOR_PROFILE, it) }
        menuButton.setOnClickListener { onActionClick(post, PostAction.MENU, it) }
    }

    /**
     * Toggle loading state for the AI Summary button.
     * Shows/hides the loading indicator and summary button accordingly.
     */
    fun setSummaryLoading(isLoading: Boolean) {
        summaryButton.visibility = if (isLoading) View.GONE else View.VISIBLE
        summaryLoadingContainer.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

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
     * Gộp imageUrls + videoThumbnailUrls + videoUrls thành 1 list media,
     * sau đó set lên RecyclerView. Layout 1–2–3 item vẫn giữ như cũ.
     */
    private fun setupMedia(post: Post) {
        val imageUrls = post.imageUrls ?: emptyList()
        val videoUrls = post.videoUrls ?: emptyList()
        val videoThumbs = post.videoThumbnailUrls ?: emptyList()

        val mediaItems = mutableListOf<PostMediaItem>()

        // Thêm ảnh
        imageUrls.forEach { url ->
            mediaItems += PostMediaItem.Image(url)
        }

        // Thêm video (ghép thumbnail + video theo index)
        val pairCount = min(videoUrls.size, videoThumbs.size)
        for (i in 0 until pairCount) {
            val videoUrl = videoUrls[i]
            val thumbUrl = videoThumbs[i]
            mediaItems += PostMediaItem.Video(thumbUrl, videoUrl)
        }

        if (mediaItems.isEmpty()) {
            rvPostImages.visibility = View.GONE
            return
        }

        rvPostImages.visibility = View.VISIBLE

        val context = rvPostImages.context
        val count = mediaItems.size

        // 1 media: full width
        if (count == 1) {
            if (imagesLayoutManager == null || imagesLayoutManager?.spanCount != 1) {
                imagesLayoutManager = GridLayoutManager(context, 1)
                rvPostImages.layoutManager = imagesLayoutManager
            }
        } else {
            // >= 2 media: dùng Grid 2 cột, item đầu full width khi >=3
            if (imagesLayoutManager == null || imagesLayoutManager?.spanCount != 2) {
                imagesLayoutManager = GridLayoutManager(context, 2)
                rvPostImages.layoutManager = imagesLayoutManager
            }

            imagesLayoutManager?.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when {
                        // 2 media: cả 2 cùng span 1
                        count == 2 -> 1
                        // 3 hoặc >3: item đầu tiên full width
                        position == 0 -> 2
                        else -> 1
                    }
                }
            }
        }

        if (rvPostImages.itemDecorationCount == 0) {
            val spacingPx = (2 * rvPostImages.resources.displayMetrics.density).toInt()
            rvPostImages.addItemDecoration(
                GridSpacingItemDecoration(
                    spacing = spacingPx
                )
            )
        }

        imagesAdapter.submitMedia(mediaItems)

        // Open Media Viewer when any media item is clicked. Convert to parcelable MediaViewerItem.
        imagesAdapter.setOnMediaClickListener { clickedIndex ->
            com.hcmus.forumus_client.ui.media.MediaViewerNavigator.open(itemView, post, clickedIndex)
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

    /**
     * Set up expandable content functionality.
     * If content is too long (exceeds maxCollapsedLines), show truncated version with "Show more..."
     * Clicking on the text toggles between expanded and collapsed states.
     */
    private fun setupExpandableContent() {
        // First, set full content to measure it
        postContent.text = fullContent
        postContent.maxLines = Int.MAX_VALUE
        
        // Post a task to check line count after layout
        postContent.post {
            val lineCount = postContent.lineCount
            
            if (lineCount > maxCollapsedLines) {
                // Content is long enough to collapse
                if (!isContentExpanded) {
                    // Show collapsed version with "Show more..."
                    setCollapsedContent()
                } else {
                    // Show full content
                    setExpandedContent()
                }
                
                // Set up click listener to toggle expansion
                postContent.setOnClickListener {
                    isContentExpanded = !isContentExpanded
                    if (isContentExpanded) {
                        setExpandedContent()
                    } else {
                        setCollapsedContent()
                    }
                }
            } else {
                // Content is short, no need for expansion
                postContent.text = fullContent
                postContent.setOnClickListener(null)
            }
        }
    }

    /**
     * Display collapsed version of the content with "Show more..." at the end
     */
    private fun setCollapsedContent() {
        postContent.maxLines = maxCollapsedLines
        
        // Create spannable text with "Show more..." in a different color
        val collapsedText = fullContent
        val spannable = SpannableString(collapsedText + showMoreText)
        
        // Make "Show more..." clickable and styled
        val showMoreColor = androidx.core.content.ContextCompat.getColor(
            itemView.context, 
            R.color.primary
        )
        spannable.setSpan(
            ForegroundColorSpan(showMoreColor),
            collapsedText.length,
            spannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        
        postContent.text = spannable
    }

    /**
     * Display full expanded content
     */
    private fun setExpandedContent() {
        postContent.maxLines = Int.MAX_VALUE
        postContent.text = fullContent
    }
}
