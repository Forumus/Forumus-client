package com.hcmus.forumus_client.ui.conversation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.databinding.ActivityConversationBinding
import com.hcmus.forumus_client.ui.image.FullscreenImageActivity

class ConversationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConversationBinding
    private lateinit var messageAdapter: ConversationAdapter
    private lateinit var imagePreviewAdapter: ImagePreviewAdapter
    private val viewModel: ConversationViewModel by viewModels()
    private var chatId: String? = null
    private var selectedImageUris: MutableList<Uri> = mutableListOf()
    
    companion object {
        const val EXTRA_CHAT_ID = "extra_chat_id"
        const val EXTRA_USER_NAME = "extra_user_name"
        const val EXTRA_USER_EMAIL = "extra_user_email"
        const val EXTRA_USER_PICTURE_URL = "extra_user_picture_url"
        private const val MAX_IMAGES = 5
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            if (uris.size + selectedImageUris.size > MAX_IMAGES) {
                Toast.makeText(
                    this,
                    "Maximum $MAX_IMAGES images allowed",
                    Toast.LENGTH_SHORT
                ).show()
                val availableSlots = MAX_IMAGES - selectedImageUris.size
                selectedImageUris.addAll(uris.take(availableSlots))
            } else {
                selectedImageUris.addAll(uris)
            }
            updateImagePreview()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
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
        
        // Setup message adapter with image click handler
        messageAdapter = ConversationAdapter(currentUserId) { imageUrls, initialPosition ->
            openFullscreenImageView(imageUrls, initialPosition)
        }
        
        binding.rvMessages.apply {
            adapter = messageAdapter
            layoutManager = LinearLayoutManager(this@ConversationActivity)
        }

        // Setup image preview adapter
        imagePreviewAdapter = ImagePreviewAdapter { uri ->
            selectedImageUris.remove(uri)
            updateImagePreview()
        }
        binding.rvImagePreview.apply {
            adapter = imagePreviewAdapter
            layoutManager = LinearLayoutManager(this@ConversationActivity, LinearLayoutManager.HORIZONTAL, false)
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
                selectedImageUris.clear()
                updateImagePreview()
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
            checkPermissionAndPickImages()
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

    private fun checkPermissionAndPickImages() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                openImagePicker()
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun openImagePicker() {
        pickImageLauncher.launch("image/*")
    }

    private fun updateImagePreview() {
        imagePreviewAdapter.setImages(selectedImageUris)
        
        if (selectedImageUris.isNotEmpty()) {
            binding.imagePreviewContainer.visibility = android.view.View.VISIBLE
        } else {
            binding.imagePreviewContainer.visibility = android.view.View.GONE
        }
    }

    private fun sendMessage() {
        val messageText = binding.etMessage.text.toString().trim()
        val hasImages = selectedImageUris.isNotEmpty()
        val hasText = messageText.isNotEmpty()
        
        if (!hasText && !hasImages) {
            Toast.makeText(this, "Please enter a message or select images", Toast.LENGTH_SHORT).show()
            return
        }
        
        val imageUriStrings = selectedImageUris.map { it.toString() }
        viewModel.sendMessage(messageText, imageUriStrings as MutableList<String>)
        
        // Clear inputs
        binding.etMessage.text.clear()
        selectedImageUris.clear()
    }

    private fun openFullscreenImageView(imageUrls: List<String>, initialPosition: Int) {
        val intent = FullscreenImageActivity.createIntent(this, imageUrls, initialPosition)
        startActivity(intent)
    }
}