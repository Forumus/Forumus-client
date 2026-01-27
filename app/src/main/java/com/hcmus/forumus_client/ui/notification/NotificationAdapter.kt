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
            is NotificationListItem.Header -> (holder as HeaderViewHolder).bind(item.titleResId)
            is NotificationListItem.Item -> (holder as NotificationViewHolder).bind(item.notification)
            is NotificationListItem.ShowMore -> (holder as ShowMoreViewHolder).bind()
        }
    }

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Notification) {
            val actor = notification.actorName
            val context = binding.root.context
            var typeText = ""
            var iconRes = com.hcmus.forumus_client.R.drawable.noti_comment
            var customTitle: String? = null

            when (notification.type) {
                "UPVOTE" -> {
                    iconRes = com.hcmus.forumus_client.R.drawable.noti_vote
                    typeText = context.getString(com.hcmus.forumus_client.R.string.noti_upvoted)
                }
                "COMMENT" -> {
                    iconRes = com.hcmus.forumus_client.R.drawable.noti_comment
                    typeText = context.getString(com.hcmus.forumus_client.R.string.noti_commented)
                }
                "REPLY" -> {
                    iconRes = com.hcmus.forumus_client.R.drawable.noti_comment
                    typeText = context.getString(com.hcmus.forumus_client.R.string.noti_replied)
                }
                "POST_DELETED" -> {
                    iconRes = com.hcmus.forumus_client.R.drawable.noti_remove_post
                    typeText = context.getString(com.hcmus.forumus_client.R.string.noti_removed_post)
                }
                "POST_REJECTED" -> {
                    iconRes = com.hcmus.forumus_client.R.drawable.noti_post_reject
                    typeText = context.getString(com.hcmus.forumus_client.R.string.noti_rejected_post)
                }
                "POST_APPROVED" -> {
                    iconRes = com.hcmus.forumus_client.R.drawable.noti_post_approve
                    typeText = context.getString(com.hcmus.forumus_client.R.string.noti_approved_post)
                }
                "STATUS_CHANGED" -> {
                    iconRes = com.hcmus.forumus_client.R.drawable.noti_status
                    customTitle = context.getString(com.hcmus.forumus_client.R.string.noti_status_changed_admin)
                }
                else -> {
                    iconRes = com.hcmus.forumus_client.R.drawable.noti_comment
                    typeText = context.getString(com.hcmus.forumus_client.R.string.noti_interacted)
                }
            }
            
            binding.ivIcon.setImageResource(iconRes)
            
            if (customTitle != null) {
                 binding.tvTitle.text = customTitle
            } else {
                 binding.tvTitle.text = "$actor $typeText"
            }
            
            binding.tvContent.text = notification.previewText

            val timeAgo = notification.createdAt?.toDate()?.time?.let {
                DateUtils.getRelativeTimeSpanString(it, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
            } ?: context.getString(com.hcmus.forumus_client.R.string.just_now)
            binding.tvTime.text = timeAgo

            // Styling Logic
            if (!notification.isRead) {
                // UNREAD: Blue dot, Surface background, Elevated
                binding.unreadIndicator.visibility = View.VISIBLE
                binding.root.setBackgroundColor(
                    binding.root.context.getColor(com.hcmus.forumus_client.R.color.surface)
                )
                binding.root.elevation = 4f // Make it pop more if needed
            } else {
                // READ: No dot, Slightly different background, Flat
                binding.unreadIndicator.visibility = View.GONE
                binding.root.setBackgroundColor(
                    binding.root.context.getColor(com.hcmus.forumus_client.R.color.bg_app)
                )
                binding.root.elevation = 0f
            }

            binding.root.setOnClickListener {
                onNotificationClick(notification)
            }
        }
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: android.widget.TextView = itemView as android.widget.TextView
        fun bind(titleResId: Int) {
            tvTitle.setText(titleResId)
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
