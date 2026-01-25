package com.hcmus.forumus_client.ui.chats

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.databinding.ItemChatMessageBinding
import android.util.Log
import com.bumptech.glide.Glide
import com.hcmus.forumus_client.data.repository.ChatRepository

class ChatsAdapter(
    private val onChatClick: (ChatItem) -> Unit,
    private val onChatDelete: (ChatItem) -> Unit
) : ListAdapter<ChatItem, ChatsAdapter.ChatViewHolder>(ChatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChatViewHolder(
        private val binding: ItemChatMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            // Set up click listener once in init
            Log.d("ChatsAdapter", "Setting up click listener for ViewHolder")
            binding.chatItemContainer.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val chatItem = getItem(position)
                    Log.d("ChatsAdapter", "Chat item clicked: ${chatItem.contactName}")
                    onChatClick(chatItem)
                }
            }
            
            // Set up long-click listener for context menu
            binding.chatItemContainer.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val chatItem = getItem(position)
                    Log.d("ChatsAdapter", "Chat item long-clicked: ${chatItem.contactName}")
                    onChatDelete(chatItem)
                    true
                } else {
                    false
                }
            }
        }

        fun bind(chatItem: ChatItem) {
            binding.contactName.text = chatItem.contactName
            binding.lastMessage.text = chatItem.lastMessage

            // Load profile image with Glide (with null safety)
            if (!chatItem.profilePictureUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(chatItem.profilePictureUrl)
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .circleCrop()
                    .into(binding.profileImage)
            } else {
                // Set default image if URL is null or empty
                binding.profileImage.setImageResource(R.drawable.ic_default_profile)
            }
            
            // Get current user ID and unread status
            val currentUserId = ChatRepository.getCurrentUserId()
            val isUnreadForCurrentUser = currentUserId?.let { chatItem.isUnread(it) } ?: false
            val unreadCountForCurrentUser = currentUserId?.let { chatItem.getUnreadCount(it) } ?: 0
            
            // Set timestamp and color based on unread status
            binding.messageTime.text = chatItem.timestamp
            if (isUnreadForCurrentUser) {
                binding.messageTime.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.chat_time_blue)
                )
                binding.lastMessage.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.chat_message_unread)
                )
                binding.contactName.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.text_primary)
                )
            } else {
                binding.messageTime.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.chat_time_gray)
                )
                binding.lastMessage.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.chat_message_read)
                )
                binding.contactName.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.text_primary)
                )
            }
            
            // Handle unread count visibility
            if (isUnreadForCurrentUser && unreadCountForCurrentUser > 0) {
                binding.unreadCount.visibility = View.VISIBLE
                binding.unreadCountText.text = unreadCountForCurrentUser.toString()
            } else {
                binding.unreadCount.visibility = View.GONE
            }
        }
    }

    private class ChatDiffCallback : DiffUtil.ItemCallback<ChatItem>() {
        override fun areItemsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
            return oldItem == newItem
        }
    }
}