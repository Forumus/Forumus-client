package com.hcmus.forumus_client.ui.conversation

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.databinding.ActivityConversationBinding

class ConversationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConversationBinding
    private lateinit var messageAdapter: ConversationAdapter
    private val viewModel: ConversationViewModel by viewModels()
    private var chatId: String? = null
    
    companion object {
        const val EXTRA_CHAT_ID = "extra_chat_id"
        const val EXTRA_USER_NAME = "extra_user_name"
        const val EXTRA_USER_EMAIL = "extra_user_email"
        const val EXTRA_USER_PICTURE_URL = "extra_user_picture_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        chatId = intent.getStringExtra(EXTRA_CHAT_ID)
        if (chatId == null) {
            Toast.makeText(this, "Invalid chat", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        
        // Load messages
        chatId?.let { viewModel.loadMessages(it) }
    }

    private fun setupUI() {
        // Get user info from intent
        val userName = intent.getStringExtra(EXTRA_USER_NAME) ?: "Chat"
        val userEmail = intent.getStringExtra(EXTRA_USER_EMAIL) ?: ""
        val userPictureUrl = intent.getStringExtra(EXTRA_USER_PICTURE_URL) ?: ""
        
        binding.tvUserName.text = userName
        binding.tvUserEmail.text = userEmail

        Glide.with(this)
            .load(userPictureUrl)
            .placeholder(R.drawable.ic_default_profile)
            .circleCrop()
            .into(binding.ivUserAvatar)

    }

    private fun setupRecyclerView() {
        val currentUserId = viewModel.getCurrentUserId() ?: ""
        messageAdapter = ConversationAdapter(currentUserId)
        binding.rvMessages.apply {
            adapter = messageAdapter
            layoutManager = LinearLayoutManager(this@ConversationActivity)
        }
    }

    private fun setupObservers() {
        viewModel.messages.observe(this, Observer { messages ->
            messageAdapter.setMessages(messages)
            // Scroll to bottom when new messages arrive
            if (messages.isNotEmpty()) {
                binding.rvMessages.scrollToPosition(messages.size - 1)
            }
        })

        viewModel.isLoading.observe(this, Observer { isLoading ->
            // You can show/hide loading indicator here
        })

        viewModel.error.observe(this, Observer { errorMessage ->
            if (errorMessage != null) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        })

        viewModel.sendMessageResult.observe(this, Observer { success ->
            if (success == true) {
                binding.etMessage.text.clear()
            }
            viewModel.clearSendResult()
        })
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        binding.btnSend.setOnClickListener {
            sendMessage()
        }
        
        binding.btnMenu.setOnClickListener {
            // Handle menu action
        }
        
        binding.btnAttachment.setOnClickListener {
            // Handle attachment
        }
        
        binding.btnImage.setOnClickListener {
            // Handle image selection
        }
        
        binding.btnVoice.setOnClickListener {
            // Handle voice recording
        }
        
        // Handle Enter key press in EditText
        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }

    private fun sendMessage() {
        val messageText = binding.etMessage.text.toString().trim()
        if (messageText.isNotEmpty()) {
            viewModel.sendMessage(messageText)
        }
    }
}