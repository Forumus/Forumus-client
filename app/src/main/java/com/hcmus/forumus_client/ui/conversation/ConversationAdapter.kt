package com.hcmus.forumus_client.ui.conversation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.marginTop
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.Timestamp
import com.hcmus.forumus_client.data.model.Message
import com.hcmus.forumus_client.data.model.MessageType
import com.hcmus.forumus_client.data.repository.PostRepository
import com.hcmus.forumus_client.databinding.ItemMessageReceivedBinding
import com.hcmus.forumus_client.databinding.ItemMessageSentBinding
import com.hcmus.forumus_client.utils.SharePostUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConversationAdapter(
        private val currentUserId: String,
        private val viewPool: RecyclerView.RecycledViewPool, // OPTIMIZATION: Shared Pool
        private val onImageClick: ((List<String>, Int) -> Unit)? = null,
        private val onMessageLongClick: ((Message) -> Unit)? = null,
        private val onLinkClick: ((String) -> Unit)? = null
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
                val binding =
                        ItemMessageSentBinding.inflate(
                                LayoutInflater.from(parent.context),
                                parent,
                                false
                        )
                SentMessageViewHolder(
                        binding,
                        viewPool,
                        onImageClick,
                        onMessageLongClick,
                        onLinkClick
                )
            }
            VIEW_TYPE_RECEIVED -> {
                val binding =
                        ItemMessageReceivedBinding.inflate(
                                LayoutInflater.from(parent.context),
                                parent,
                                false
                        )
                ReceivedMessageViewHolder(binding, viewPool, onImageClick, onLinkClick)
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
            private val onMessageLongClick: ((Message) -> Unit)?,
            private val onLinkClick: ((String) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentMessage: Message? = null
        private var loadPostJob: Job? = null
        private val postRepository = PostRepository()
        
        // Cache for inflated shared post view and its children
        private var sharedPostContainer: View? = null
        private var ivThumbnail: ImageView? = null
        private var tvTitle: TextView? = null
        private var tvContent: TextView? = null

        init {
            // Setup long-click listener on the message container
            binding.root.setOnLongClickListener {
                currentMessage?.let { message -> onMessageLongClick?.invoke(message) }
                true
            }
        }

        fun bind(message: Message) {
            currentMessage = message

            // Cancel any previous post loading job
            loadPostJob?.cancel()

            if (message.content.trim().isNotEmpty()) {
                // Check if content is a share URL
                if (SharePostUtil.isShareUrl(message.content)) {
                    // Hide regular text and message bubble, show shared post preview
                    binding.tvMessageText.visibility = View.GONE
                    binding.llMessageBubble.visibility = View.GONE
                    loadAndDisplaySharedPost(message.content)
                } else {
                    // Hide shared post preview if recycled
                    sharedPostContainer?.visibility = View.GONE

                    // Show message bubble and regular text - reset any link styling from recycled view
                    binding.llMessageBubble.visibility = View.VISIBLE
                    binding.tvMessageText.text = message.content
                    binding.tvMessageText.setTextColor(
                            binding.root.context.getColor(com.hcmus.forumus_client.R.color.white)
                    )
                    binding.tvMessageText.movementMethod = null
                    binding.tvMessageText.visibility = View.VISIBLE
                }
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
                binding.llMessageBubble.background =
                        AppCompatResources.getDrawable(
                                binding.root.context,
                                com.hcmus.forumus_client.R.drawable.message_delete_background
                        )
            }
        }

        private fun loadAndDisplaySharedPost(shareUrl: String) {
            loadPostJob =
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            val post =
                                    withContext(Dispatchers.IO) {
                                        SharePostUtil.validateShareUrl(shareUrl, postRepository)
                                                .getOrNull()
                                    }

                            if (post != null) {
                                displaySharedPostPreview(post, shareUrl)
                            } else {
                                // Fallback: show URL if post not found
                                renderAsLink(shareUrl)
                            }
                        } catch (e: Exception) {
                            // Fallback: show URL on error
                            renderAsLink(shareUrl)
                        }
                    }
        }

        private fun displaySharedPostPreview(
                post: com.hcmus.forumus_client.data.model.Post,
                shareUrl: String
        ) {
            // Inflate ViewStub if not already inflated
            if (sharedPostContainer == null) {
                val viewStub: android.view.ViewStub = 
                        binding.root.findViewById(com.hcmus.forumus_client.R.id.stub_shared_post)
                sharedPostContainer = viewStub.inflate()
                
                // Cache the child views
                ivThumbnail = sharedPostContainer?.findViewById(com.hcmus.forumus_client.R.id.iv_post_thumbnail)
                tvTitle = sharedPostContainer?.findViewById(com.hcmus.forumus_client.R.id.tv_post_title)
                tvContent = sharedPostContainer?.findViewById(com.hcmus.forumus_client.R.id.tv_post_content)
            }

            sharedPostContainer?.visibility = View.VISIBLE

            tvTitle?.text = post.title.ifEmpty { "Shared Post" }
            tvContent?.text = "${post.content.substring(0, minOf(100, post.content.length))}...".ifEmpty { "No content" }
            tvContent?.visibility = if (post.content.isNotEmpty()) View.VISIBLE else View.GONE

            // Load thumbnail if available
            if (post.imageUrls.isNotEmpty() && ivThumbnail != null) {
                Glide.with(binding.root.context)
                        .load(post.imageUrls.first())
                        .apply(RequestOptions().centerCrop())
                        .into(ivThumbnail!!)
                ivThumbnail?.visibility = View.VISIBLE
            } else {
                ivThumbnail?.visibility = View.GONE
            }

            // Click to open post
            sharedPostContainer?.setOnClickListener { onLinkClick?.invoke(shareUrl) }
        }

        private fun renderAsLink(url: String) {
            val spannableString = android.text.SpannableString(url)

            // Add custom click span with proper styling
            val clickableSpan =
                    object : android.text.style.ClickableSpan() {
                        override fun onClick(widget: View) {
                            onLinkClick?.invoke(url)
                        }

                        override fun updateDrawState(ds: android.text.TextPaint) {
                            super.updateDrawState(ds)
                            ds.color = binding.root.context.getColor(com.hcmus.forumus_client.R.color.primary)
                            ds.isUnderlineText = true
                        }
                    }

            spannableString.setSpan(
                    clickableSpan,
                    0,
                    url.length,
                    android.text.SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Set text and enable link movement
            binding.tvMessageText.text = spannableString
            binding.tvMessageText.movementMethod =
                    android.text.method.LinkMovementMethod.getInstance()
            binding.tvMessageText.highlightColor = android.graphics.Color.TRANSPARENT
            binding.tvMessageText.visibility = View.VISIBLE
        }
    }

    class ReceivedMessageViewHolder(
            private val binding: ItemMessageReceivedBinding,
            viewPool: RecyclerView.RecycledViewPool,
            private val onImageClick: ((List<String>, Int) -> Unit)?,
            private val onLinkClick: ((String) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        private var loadPostJob: Job? = null
        private val postRepository = PostRepository()
        
        // Cache for inflated shared post view and its children
        private var sharedPostContainer: View? = null
        private var ivThumbnail: ImageView? = null
        private var tvTitle: TextView? = null
        private var tvContent: TextView? = null

        fun bind(message: Message) {
            // Cancel any previous post loading job
            loadPostJob?.cancel()

            if (message.content.trim().isNotEmpty()) {
                // Check if content is a share URL
                if (SharePostUtil.isShareUrl(message.content)) {
                    loadAndDisplaySharedPost(message.content)
                } else {
                    // Hide shared post preview if recycled
                    sharedPostContainer?.visibility = View.GONE

                    // Show message bubble and regular text - reset any link styling from recycled view
                    binding.root.findViewById<View>(
                        com.hcmus.forumus_client.R.id.ll_message_bubble
                    )?.visibility = View.VISIBLE
                    binding.tvMessageText.text = message.content
                    binding.tvMessageText.setTextColor(
                            binding.root.context.getColor(com.hcmus.forumus_client.R.color.text_primary)
                    )
                    binding.tvMessageText.movementMethod = null
                    binding.tvMessageText.visibility = View.VISIBLE
                }
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

        private fun loadAndDisplaySharedPost(shareUrl: String) {
            loadPostJob =
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            val post =
                                    withContext(Dispatchers.IO) {
                                        SharePostUtil.validateShareUrl(shareUrl, postRepository)
                                                .getOrNull()
                                    }

                            if (post != null) {
                                // Hide regular text and find the message bubble to hide it
                                binding.tvMessageText.visibility = View.GONE
                                // Hide the message container background for messages
                                binding.llMessageBubble.visibility = View.GONE

                                displaySharedPostPreview(post, shareUrl)
                            } else {
                                // Fallback: show URL if post not found
                                renderAsLink(shareUrl)
                            }
                        } catch (e: Exception) {
                            // Fallback: show URL on error
                            renderAsLink(shareUrl)
                        }
                    }
        }

        private fun displaySharedPostPreview(
                post: com.hcmus.forumus_client.data.model.Post,
                shareUrl: String
        ) {
            // Inflate ViewStub if not already inflated
            if (sharedPostContainer == null) {
                val viewStub: android.view.ViewStub = 
                        binding.root.findViewById(com.hcmus.forumus_client.R.id.stub_shared_post)
                sharedPostContainer = viewStub.inflate()
                
                // Cache the child views
                ivThumbnail = sharedPostContainer?.findViewById(com.hcmus.forumus_client.R.id.iv_post_thumbnail)
                tvTitle = sharedPostContainer?.findViewById(com.hcmus.forumus_client.R.id.tv_post_title)
                tvContent = sharedPostContainer?.findViewById(com.hcmus.forumus_client.R.id.tv_post_content)
            }

            sharedPostContainer?.visibility = View.VISIBLE

            tvTitle?.text = post.title.ifEmpty { "Shared Post" }
            tvContent?.text = post.content.ifEmpty { "No content" }
            tvContent?.visibility = if (post.content.isNotEmpty()) View.VISIBLE else View.GONE

            // Load thumbnail if available
            if (post.imageUrls.isNotEmpty() && ivThumbnail != null) {
                Glide.with(binding.root.context)
                        .load(post.imageUrls.first())
                        .apply(RequestOptions().centerCrop())
                        .into(ivThumbnail!!)
                ivThumbnail?.visibility = View.VISIBLE
            } else {
                ivThumbnail?.visibility = View.GONE
            }

            // Click to open post
            sharedPostContainer?.setOnClickListener { onLinkClick?.invoke(shareUrl) }
        }

        private fun renderAsLink(url: String) {
            val spannableString = android.text.SpannableString(url)

            // Add custom click span with proper styling
            val clickableSpan =
                    object : android.text.style.ClickableSpan() {
                        override fun onClick(widget: View) {
                            onLinkClick?.invoke(url)
                        }

                        override fun updateDrawState(ds: android.text.TextPaint) {
                            super.updateDrawState(ds)
                            ds.color = binding.root.context.getColor(com.hcmus.forumus_client.R.color.primary)
                            ds.isUnderlineText = true
                        }
                    }

            spannableString.setSpan(
                    clickableSpan,
                    0,
                    url.length,
                    android.text.SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Set text and enable link movement
            binding.tvMessageText.text = spannableString
            binding.tvMessageText.movementMethod =
                    android.text.method.LinkMovementMethod.getInstance()
            binding.tvMessageText.highlightColor = android.graphics.Color.TRANSPARENT
            binding.tvMessageText.visibility = View.VISIBLE
        }
    }
}
