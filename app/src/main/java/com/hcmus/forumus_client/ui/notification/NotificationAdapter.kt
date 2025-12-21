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
    private val onNotificationClick: (Notification) -> Unit
) : ListAdapter<Notification, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Notification) {
            val actor = notification.actorName
            val typeText = when (notification.type) {
                "UPVOTE" -> "upvoted your post"
                "COMMENT" -> "commented on your post"
                "REPLY" -> "replied to your comment"
                else -> "interacted with you"
            }
            binding.tvTitle.text = "$actor $typeText"
            binding.tvContent.text = notification.previewText

            val timeAgo = notification.createdAt?.toDate()?.time?.let {
                DateUtils.getRelativeTimeSpanString(it, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
            } ?: "Just now"
            binding.tvTime.text = timeAgo

            if (!notification.isRead) {
                binding.unreadIndicator.visibility = View.VISIBLE
                binding.root.setBackgroundColor(Color.parseColor("#F5F5F5")) // Slight highlight
            } else {
                binding.unreadIndicator.visibility = View.GONE
                binding.root.setBackgroundColor(Color.WHITE)
            }

            binding.root.setOnClickListener {
                onNotificationClick(notification)
            }
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem == newItem
        }
    }
}
