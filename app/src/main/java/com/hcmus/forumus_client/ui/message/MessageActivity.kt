package com.hcmus.forumus_client.ui.message

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.hcmus.forumus_client.databinding.ActivityChatBinding
import com.hcmus.forumus_client.data.model.Message

class MessageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var messageAdapter: MessageAdapter
    
    companion object {
        const val EXTRA_USER_NAME = "extra_user_name"
        const val EXTRA_USER_EMAIL = "extra_user_email"
        const val EXTRA_USER_AVATAR = "extra_user_avatar"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupUI()
        setupRecyclerView()
        loadSampleMessages()
        setupClickListeners()
    }

    private fun setupUI() {
        // Get user info from intent
        val userName = intent.getStringExtra(EXTRA_USER_NAME) ?: "Sarah Johnson"
        val userEmail = intent.getStringExtra(EXTRA_USER_EMAIL) ?: "sarahjohnson@example.com"
        
        binding.tvUserName.text = userName
        binding.tvUserEmail.text = userEmail
        
        // Set user avatar - for Sarah Johnson, use the sample avatar matching the Figma design
        if (userName == "Sarah Johnson") {
            binding.ivUserAvatar.setImageResource(android.R.color.transparent)
            binding.ivUserAvatar.background = AppCompatResources.getDrawable(this, com.hcmus.forumus_client.R.drawable.sample_user_avatar)
        } else {
            binding.ivUserAvatar.setImageResource(com.hcmus.forumus_client.R.drawable.ic_default_profile)
        }
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter()
        binding.rvMessages.apply {
            adapter = messageAdapter
            layoutManager = LinearLayoutManager(this@MessageActivity)
        }
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
            val message = Message(
                id = System.currentTimeMillis().toString(),
                content = messageText,
                timestamp = getCurrentTime(),
                isSentByCurrentUser = true
            )
            messageAdapter.addMessage(message)
            binding.etMessage.text.clear()
            
            // Scroll to bottom
            binding.rvMessages.scrollToPosition(messageAdapter.itemCount - 1)
        }
    }

    private fun loadSampleMessages() {
        val messages = listOf(
            Message(
                id = "1",
                content = "Hey! How are you doing?",
                timestamp = "10:30 AM",
                isSentByCurrentUser = false
            ),
            Message(
                id = "2",
                content = "I'm doing great! Thanks for asking 😊",
                timestamp = "10:32 AM",
                isSentByCurrentUser = true
            ),
            Message(
                id = "3",
                content = "Did you see the photos from last weekend?",
                timestamp = "10:33 AM",
                isSentByCurrentUser = false
            ),
            Message(
                id = "4",
                content = "Yes! They were amazing! I loved the sunset shots",
                timestamp = "10:35 AM",
                isSentByCurrentUser = true
            ),
            Message(
                id = "5",
                content = "Right? The lighting was perfect that day",
                timestamp = "10:36 AM",
                isSentByCurrentUser = false
            ),
            Message(
                id = "6",
                content = "We should do it again soon!",
                timestamp = "10:37 AM",
                isSentByCurrentUser = false
            ),
            Message(
                id = "7",
                content = "Absolutely! How about next Saturday?",
                timestamp = "10:38 AM",
                isSentByCurrentUser = true
            ),
            Message(
                id = "8",
                content = "Perfect! Same time, same place?",
                timestamp = "10:39 AM",
                isSentByCurrentUser = false
            ),
            Message(
                id = "9",
                content = "Sounds like a plan! 🎉",
                timestamp = "10:40 AM",
                isSentByCurrentUser = true
            )
        )
        
        messageAdapter.setMessages(messages)
    }

    private fun getCurrentTime(): String {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)
        val amPm = if (hour >= 12) "PM" else "AM"
        val displayHour = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
        return String.format("%d:%02d %s", displayHour, minute, amPm)
    }
}