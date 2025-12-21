package com.hcmus.forumus_client.ui.notification

import android.graphics.Color
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hcmus.forumus_client.data.model.Notification
import com.hcmus.forumus_client.databinding.ItemNotificationBinding

class NotificationAdapter(
    private val onNotificationClick: (Notification) -> Unit,
    private val onShowMoreClick: () -> Unit
) : ListAdapter<NotificationListItem, RecyclerView.ViewHolder>(NotificationDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
        private const val VIEW_TYPE_SHOW_MORE = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is NotificationListItem.Header -> VIEW_TYPE_HEADER
            is NotificationListItem.Item -> VIEW_TYPE_ITEM
            is NotificationListItem.ShowMore -> VIEW_TYPE_SHOW_MORE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = inflater.inflate(com.hcmus.forumus_client.R.layout.item_notification_header, parent, false)
                HeaderViewHolder(view)
            }
            VIEW_TYPE_SHOW_MORE -> {
                val view = inflater.inflate(com.hcmus.forumus_client.R.layout.item_notification_show_more, parent, false)
                ShowMoreViewHolder(view)
            }
            else -> {
                val binding = ItemNotificationBinding.inflate(inflater, parent, false)
                NotificationViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is NotificationListItem.Header -> (holder as HeaderViewHolder).bind(item.title)
            is NotificationListItem.Item -> (holder as NotificationViewHolder).bind(item.notification)
            is NotificationListItem.ShowMore -> (holder as ShowMoreViewHolder).bind()
        }
    }

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Notification) {
            val actor = notification.actorName
            val typeText = when (notification.type) {
                "UPVOTE" -> {
                    binding.ivIcon.setImageResource(com.hcmus.forumus_client.R.drawable.noti_vote)
                    "upvoted your post"
                }
                "COMMENT" -> {
                    binding.ivIcon.setImageResource(com.hcmus.forumus_client.R.drawable.noti_comment)
                    "commented on your post"
                }
                "REPLY" -> {
                    binding.ivIcon.setImageResource(com.hcmus.forumus_client.R.drawable.noti_comment)
                    "replied to your comment"
                }
                else -> {
                    binding.ivIcon.setImageResource(com.hcmus.forumus_client.R.drawable.noti_comment)
                    "interacted with you"
                }
            }
            binding.tvTitle.text = "$actor $typeText"
            binding.tvContent.text = notification.previewText

            val timeAgo = notification.createdAt?.toDate()?.time?.let {
                DateUtils.getRelativeTimeSpanString(it, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
            } ?: "Just now"
            binding.tvTime.text = timeAgo

            // Styling Logic
            if (!notification.isRead) {
                // UNREAD: Blue dot, White background, Elevated
                binding.unreadIndicator.visibility = View.VISIBLE
                binding.root.setBackgroundColor(Color.WHITE)
                binding.root.elevation = 4f // Make it pop more if needed
            } else {
                // READ: No dot, Gray background (Lighter/Dimmer), Flat
                binding.unreadIndicator.visibility = View.GONE
                binding.root.setBackgroundColor(Color.parseColor("#F2F4F7")) // Very light gray for read
                binding.root.elevation = 0f
            }

            binding.root.setOnClickListener {
                onNotificationClick(notification)
            }
        }
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: android.widget.TextView = itemView as android.widget.TextView
        fun bind(title: String) {
            tvTitle.text = title
        }
    }

    inner class ShowMoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener { onShowMoreClick() }
        }
        fun bind() {
            // Static text
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationListItem>() {
        override fun areItemsTheSame(oldItem: NotificationListItem, newItem: NotificationListItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NotificationListItem, newItem: NotificationListItem): Boolean {
            return oldItem == newItem
        }
    }
}
