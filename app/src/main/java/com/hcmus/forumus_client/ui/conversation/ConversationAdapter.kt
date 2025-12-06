package com.hcmus.forumus_client.ui.conversation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.hcmus.forumus_client.data.model.Message
import com.hcmus.forumus_client.databinding.ItemMessageReceivedBinding
import com.hcmus.forumus_client.databinding.ItemMessageSentBinding
import java.text.SimpleDateFormat
import java.util.*

class ConversationAdapter(
    private val currentUserId: String,
    private val viewPool: RecyclerView.RecycledViewPool, // OPTIMIZATION: Shared Pool
    private val onImageClick: ((List<String>, Int) -> Unit)? = null
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
                SentMessageViewHolder(binding, viewPool, onImageClick)
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
        private val onImageClick: ((List<String>, Int) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        // 1. Create the adapter ONLY ONCE
        private val messageImageAdapter = MessageImageAdapter()

        init {
            // 2. Setup RecyclerView ONLY ONCE
            binding.rvMessageImages.apply {
                setRecycledViewPool(viewPool) // SHARE VIEWS
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false).apply {
                    stackFromEnd = true
                }
                adapter = messageImageAdapter // Attach adapter immediately
                setHasFixedSize(true) // Optimization
                isNestedScrollingEnabled = false // Optimization for smooth scrolling
                itemAnimator = null // Remove animations to reduce flicker on scroll
            }
        }

        fun bind(message: Message) {
            if (message.content.trim().isNotEmpty()) {
                binding.tvMessageText.text = message.content
                binding.tvMessageText.visibility = View.VISIBLE
            } else {
                binding.tvMessageText.visibility = View.GONE
            }

            binding.tvTimestamp.text = formatTimestamp(message.timestamp)

            if (message.imageUrls.isNotEmpty()) {
                binding.rvMessageImages.visibility = View.VISIBLE

                // 3. IMPORTANT: Update the EXISTING adapter. Do NOT create a new one.
                // We also update the click listener callback with the current message's images
                messageImageAdapter.apply {
                    setImageUrls(message.imageUrls)
                    setOnImageClickListener { _, position ->
                        onImageClick?.invoke(message.imageUrls, position)
                    }
                }
            } else {
                binding.rvMessageImages.visibility = View.GONE
                // Clear memory in the adapter if hidden
                messageImageAdapter.setImageUrls(emptyList())
            }
        }
    }

    class ReceivedMessageViewHolder(
        private val binding: ItemMessageReceivedBinding,
        viewPool: RecyclerView.RecycledViewPool,
        private val onImageClick: ((List<String>, Int) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        private val messageImageAdapter = MessageImageAdapter()

        init {
            binding.rvMessageImages.apply {
                setRecycledViewPool(viewPool)
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false).apply {
                    stackFromEnd = true
                }
                adapter = messageImageAdapter
                setHasFixedSize(true)
                isNestedScrollingEnabled = false
                itemAnimator = null
            }
        }

        fun bind(message: Message) {
            if (message.content.trim().isNotEmpty()) {
                binding.tvMessageText.text = message.content
                binding.tvMessageText.visibility = View.VISIBLE
            } else {
                binding.tvMessageText.visibility = View.GONE
            }

            binding.tvTimestamp.text = formatTimestamp(message.timestamp)

            if (message.imageUrls.isNotEmpty()) {
                binding.rvMessageImages.visibility = View.VISIBLE

                // Update EXISTING adapter
                messageImageAdapter.apply {
                    setImageUrls(message.imageUrls)
                    setOnImageClickListener { _, position ->
                        onImageClick?.invoke(message.imageUrls, position)
                    }
                }
            } else {
                binding.rvMessageImages.visibility = View.GONE
                messageImageAdapter.setImageUrls(emptyList())
            }
        }
    }
}