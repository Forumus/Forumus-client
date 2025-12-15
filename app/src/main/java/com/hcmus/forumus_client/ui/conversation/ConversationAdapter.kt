package com.hcmus.forumus_client.ui.conversation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.hcmus.forumus_client.data.model.Message
import com.hcmus.forumus_client.data.model.MessageType
import com.hcmus.forumus_client.databinding.ItemMessageReceivedBinding
import com.hcmus.forumus_client.databinding.ItemMessageSentBinding
import java.text.SimpleDateFormat
import java.util.*

class ConversationAdapter(
    private val currentUserId: String,
    private val viewPool: RecyclerView.RecycledViewPool, // OPTIMIZATION: Shared Pool
    private val onImageClick: ((List<String>, Int) -> Unit)? = null,
    private val onMessageLongClick: ((Message) -> Unit)? = null
) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2

        fun formatTimestamp(timestamp: Timestamp?): String {
            return try {
                if (timestamp == null) return "unknown"
                val date = timestamp.toDate()
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                timeFormat.format(date)
            } catch (e: Exception) {
                "unknown"
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.isSentByCurrentUser(currentUserId)) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val binding = ItemMessageSentBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                SentMessageViewHolder(binding, viewPool, onImageClick, onMessageLongClick)
            }
            VIEW_TYPE_RECEIVED -> {
                val binding = ItemMessageReceivedBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ReceivedMessageViewHolder(binding, viewPool, onImageClick)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }

    // --- View Holders ---

    class SentMessageViewHolder(
        private val binding: ItemMessageSentBinding,
        viewPool: RecyclerView.RecycledViewPool,
        private val onImageClick: ((List<String>, Int) -> Unit)?,
        private val onMessageLongClick: ((Message) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentMessage: Message? = null

        init {
            // Setup long-click listener on the message container
            binding.root.setOnLongClickListener {
                currentMessage?.let { message ->
                    onMessageLongClick?.invoke(message)
                }
                true
            }
        }

        fun bind(message: Message) {
            currentMessage = message
            
            if (message.content.trim().isNotEmpty()) {
                binding.tvMessageText.text = message.content
                binding.tvMessageText.visibility = View.VISIBLE
            } else {
                binding.tvMessageText.visibility = View.GONE
            }

            binding.tvTimestamp.text = formatTimestamp(message.timestamp)

            // Update images view
            binding.messageImagesView.setImageUrls(message.imageUrls)
            binding.messageImagesView.setOnImageClickListener { urls, position ->
                onImageClick?.invoke(urls, position)
            }

            if (message.type == MessageType.DELETED) {
                binding.llMessageBubble.background = AppCompatResources.getDrawable(
                    binding.root.context,
                    com.hcmus.forumus_client.R.drawable.message_delete_background
                )
            }
        }
    }

    class ReceivedMessageViewHolder(
        private val binding: ItemMessageReceivedBinding,
        viewPool: RecyclerView.RecycledViewPool,
        private val onImageClick: ((List<String>, Int) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            if (message.content.trim().isNotEmpty()) {
                binding.tvMessageText.text = message.content
                binding.tvMessageText.visibility = View.VISIBLE
            } else {
                binding.tvMessageText.visibility = View.GONE
            }

            binding.tvTimestamp.text = formatTimestamp(message.timestamp)

            // Update images view
            binding.messageImagesView.setImageUrls(message.imageUrls)
            binding.messageImagesView.setOnImageClickListener { urls, position ->
                onImageClick?.invoke(urls, position)
            }
        }
    }
}