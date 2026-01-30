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
import coil.request.CachePolicy
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

    val authorAvatar: ImageView = itemView.findViewById(R.id.authorAvatar)
    val authorName: TextView = itemView.findViewById(R.id.authorName)
    val timestamp: TextView = itemView.findViewById(R.id.timestamp)

    val postTitle: TextView = itemView.findViewById(R.id.postTitle)
    val postContent: TextView = itemView.findViewById(R.id.postContent)

    val upvoteIcon: ImageButton = itemView.findViewById(R.id.upvoteIcon)
    val upvoteCount: TextView = itemView.findViewById(R.id.upvoteCount)
    val downvoteIcon: ImageButton = itemView.findViewById(R.id.downvoteIcon)

    val replyButton: LinearLayout = itemView.findViewById(R.id.replyButton)
    val replyCount: TextView = itemView.findViewById(R.id.replyCount)
    val shareButton: LinearLayout = itemView.findViewById(R.id.shareButton)
    val menuButton: ImageButton = itemView.findViewById(R.id.menuButton)

    val summaryButton: LinearLayout = itemView.findViewById(R.id.summaryButton)
    val summaryLoadingContainer: FrameLayout = itemView.findViewById(R.id.summaryLoadingContainer)

    val rootLayout: LinearLayout = itemView.findViewById(R.id.postItem)

    private val rvPostImages: RecyclerView = itemView.findViewById(R.id.rvPostImages)

    val topicContainer: LinearLayout = itemView.findViewById(R.id.topicContainer)
    val imagesAdapter = PostMediaAdapter()

    private val locationButton: LinearLayout = itemView.findViewById(R.id.locationButton)
    private val locationText: TextView = itemView.findViewById(R.id.locationText)

    var imagesLayoutManager: GridLayoutManager? = null

    private var isContentExpanded = false
    private var fullContent = ""
    private val maxCollapsedLines = 4
    private val showMoreText = " Show more..."

    init {
        rvPostImages.adapter = imagesAdapter
    }

    fun bind(post: Post, topicMap: Map<String, Topic>? = null) {
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

        postTitle.text = post.title
        
        isContentExpanded = false
        fullContent = post.content
        setupExpandableContent()

        upvoteCount.text = post.upvoteCount.toString()
        replyCount.text = post.commentCount.toString()

        authorAvatar.load(post.authorAvatarUrl) {
            crossfade(true)
            placeholder(R.drawable.default_avatar)
            error(R.drawable.default_avatar)
            transformations(CircleCropTransformation())
            memoryCachePolicy(CachePolicy.ENABLED)
            diskCachePolicy(CachePolicy.ENABLED)
        }

        applyVoteUI(post)

        setupMedia(post)

        setupLocation(post)

        topicContainer.removeAllViews()
        if (topicMap != null && post.topicIds.isNotEmpty()) {
            val density = itemView.resources.displayMetrics.density
            val marginEnd = (8 * density).toInt()
            val paddingH = (12 * density).toInt()
            val paddingV = (6 * density).toInt()

            post.topicIds.take(5).forEach { topicId ->
                val topic = topicMap[topicId]
                val topicName = topic?.name ?: topicId

                val defaultColor = "#4285F4".toColorInt()
                var mainColor = defaultColor
                var alpha = 0.1

                if (topic != null) {
                    try {
                        if (topic.fillColor.isNotEmpty()) {
                            mainColor = android.graphics.Color.parseColor(topic.fillColor)
                        }
                        alpha = topic.fillAlpha
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

                val alphaInt = (alpha * 255).toInt()
                val backgroundColor = (alphaInt shl 24) or (mainColor and 0x00FFFFFF)

                val backgroundDrawable = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = 16 * density
                    setColor(backgroundColor)
                }

                val textView = TextView(itemView.context).apply {
                    text = topicName
                    textSize = 12f
                    setTextColor(mainColor)
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

        rootLayout.setOnClickListener { onActionClick(post, PostAction.OPEN, it) }
        upvoteIcon.setOnClickListener { 
            if (upvoteIcon.isEnabled) {
                upvoteIcon.isEnabled = false
                downvoteIcon.isEnabled = false
                onActionClick(post, PostAction.UPVOTE, it)
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
                onActionClick(post, PostAction.DOWNVOTE, it)
                downvoteIcon.postDelayed({
                    upvoteIcon.isEnabled = true
                    downvoteIcon.isEnabled = true
                }, 300)
            }
        }
        replyButton.setOnClickListener { onActionClick(post, PostAction.REPLY, it) }
        shareButton.setOnClickListener { onActionClick(post, PostAction.SHARE, it) }
        summaryButton.setOnClickListener { 
            if (summaryButton.isEnabled) {
                onActionClick(post, PostAction.SUMMARY, it)
            }
        }
        authorAvatar.setOnClickListener { onActionClick(post, PostAction.AUTHOR_PROFILE, it) }
        authorName.setOnClickListener { onActionClick(post, PostAction.AUTHOR_PROFILE, it) }
        menuButton.setOnClickListener { onActionClick(post, PostAction.MENU, it) }
    }

    fun setSummaryLoading(isLoading: Boolean) {
        summaryButton.visibility = if (isLoading) View.GONE else View.VISIBLE
        summaryLoadingContainer.visibility = if (isLoading) View.VISIBLE else View.GONE
        summaryButton.isEnabled = !isLoading
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

    private fun setupMedia(post: Post) {
        val imageUrls = post.imageUrls ?: emptyList()
        val videoUrls = post.videoUrls ?: emptyList()
        val videoThumbs = post.videoThumbnailUrls ?: emptyList()

        val mediaItems = mutableListOf<PostMediaItem>()

        imageUrls.forEach { url ->
            mediaItems += PostMediaItem.Image(url)
        }

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

        if (count == 1) {
            if (imagesLayoutManager == null || imagesLayoutManager?.spanCount != 1) {
                imagesLayoutManager = GridLayoutManager(context, 1)
                rvPostImages.layoutManager = imagesLayoutManager
            }
        } else {
            if (imagesLayoutManager == null || imagesLayoutManager?.spanCount != 2) {
                imagesLayoutManager = GridLayoutManager(context, 2)
                rvPostImages.layoutManager = imagesLayoutManager
            }

            imagesLayoutManager?.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when {
                        count == 2 -> 1
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

        imagesAdapter.setOnMediaClickListener { clickedIndex ->
            com.hcmus.forumus_client.ui.media.MediaViewerNavigator.open(itemView, post, clickedIndex)
        }
    }

    private fun setupLocation(post: Post) {
        if (post.locationName != null && post.locationName!!.isNotBlank()) {
            locationButton.visibility = View.VISIBLE
            locationText.text = post.locationName
            
            locationButton.setOnClickListener {
                openLocationInMaps(post)
            }
        } else {
            locationButton.visibility = View.GONE
        }
    }

    private fun openLocationInMaps(post: Post) {
        val lat = post.latitude
        val lng = post.longitude
        
        if (lat != null && lng != null) {
            try {
                val gmmIntentUri = android.net.Uri.parse("geo:$lat,$lng?q=$lat,$lng(${post.locationName})")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                
                if (mapIntent.resolveActivity(itemView.context.packageManager) != null) {
                    itemView.context.startActivity(mapIntent)
                } else {
                    val browserIntent = Intent(
                        Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
                    )
                    itemView.context.startActivity(browserIntent)
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    itemView.context,
                    "${post.locationName}\nLat: $lat, Long: $lng",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            android.widget.Toast.makeText(
                itemView.context,
                post.locationName,
                android.widget.Toast.LENGTH_SHORT
            ).show()
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
                Log.e("PostViewHolder", "Error formatting timestamp", e)
                "now"
            }
        } else {
            "now"
        }
    }

    private fun setupExpandableContent() {
        postContent.text = fullContent
        postContent.maxLines = Int.MAX_VALUE
        
        postContent.post {
            val lineCount = postContent.lineCount
            
            if (lineCount > maxCollapsedLines) {
                if (!isContentExpanded) {
                    setCollapsedContent()
                } else {
                    setExpandedContent()
                }
                
                postContent.setOnClickListener {
                    isContentExpanded = !isContentExpanded
                    if (isContentExpanded) {
                        setExpandedContent()
                    } else {
                        setCollapsedContent()
                    }
                }
            } else {
                postContent.text = fullContent
                postContent.setOnClickListener(null)
            }
        }
    }

    private fun setCollapsedContent() {
        postContent.maxLines = maxCollapsedLines
        
        val collapsedText = fullContent
        val spannable = SpannableString(collapsedText + showMoreText)
        
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

    private fun setExpandedContent() {
        postContent.maxLines = Int.MAX_VALUE
        postContent.text = fullContent
    }

    fun updateVotes(post: Post) {
        upvoteCount.text = post.upvoteCount.toString()
        applyVoteUI(post)
    }

    fun updateTopics(post: Post, topicMap: Map<String, Topic>?) {
        topicContainer.removeAllViews()
        if (topicMap != null && post.topicIds.isNotEmpty()) {
            val density = itemView.resources.displayMetrics.density
            val marginEnd = (8 * density).toInt()
            val paddingH = (12 * density).toInt()
            val paddingV = (6 * density).toInt()

            post.topicIds.take(5).forEach { topicId ->
                val topic = topicMap[topicId]
                val topicName = topic?.name ?: topicId

                val defaultColor = "#4285F4".toColorInt()
                var mainColor = defaultColor
                var alpha = 0.1

                if (topic != null) {
                    try {
                        if (topic.fillColor.isNotEmpty()) {
                            mainColor = android.graphics.Color.parseColor(topic.fillColor)
                        }
                        alpha = topic.fillAlpha
                        if (alpha < 0.0) alpha = 0.0
                        if (alpha > 1.0) alpha = 1.0
                    } catch (e: Exception) {
                        android.util.Log.e("PostViewHolder", "Color parsing error", e)
                    }
                }

                val alphaInt = (alpha * 255).toInt()
                val backgroundColor = (alphaInt shl 24) or (mainColor and 0x00FFFFFF)

                val backgroundDrawable = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = 16 * density
                    setColor(backgroundColor)
                }

                val textView = TextView(itemView.context).apply {
                    text = topicName
                    textSize = 12f
                    setTextColor(mainColor)
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
    }
}
