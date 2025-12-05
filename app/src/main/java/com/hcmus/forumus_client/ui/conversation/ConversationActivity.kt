package com.hcmus.forumus_client.ui.conversation

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.databinding.ActivityConversationBinding
import com.hcmus.forumus_client.ui.image.FullscreenImageActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

class ConversationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConversationBinding
    private lateinit var messageAdapter: ConversationAdapter
    private lateinit var imagePreviewAdapter: ImagePreviewAdapter
    private val viewModel: ConversationViewModel by viewModels()
    private var chatId: String? = null
    private var selectedImageUris: MutableList<Uri> = mutableListOf()
    private var currentPhotoPath: String? = null
    private var photoUri: Uri = Uri.EMPTY
    
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

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            if (selectedImageUris.size < MAX_IMAGES) {
                selectedImageUris.add(photoUri)
                updateImagePreview()
            } else {
                Toast.makeText(
                    this,
                    "Maximum $MAX_IMAGES images allowed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            // Delete the file if capture failed
            currentPhotoPath?.let { path ->
                File(path).delete()
            }
        }
    }

    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
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

    private val viewPool = RecyclerView.RecycledViewPool()

    private fun setupRecyclerView() {
        val currentUserId = viewModel.getCurrentUserId() ?: ""

        // PASS THE POOL HERE
        messageAdapter = ConversationAdapter(currentUserId, viewPool) { imageUrls, initialPosition ->
            openFullscreenImageView(imageUrls, initialPosition)
        }

        binding.rvMessages.apply {
            adapter = messageAdapter
            // Optimization: Cache more view holders offscreen to prevent re-binding on small scrolls
            setItemViewCacheSize(20)
            layoutManager = LinearLayoutManager(this@ConversationActivity).apply {
                stackFromEnd = true // Keeps view at bottom
            }
            
            // Add scroll listener to detect when user scrolls to top
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    // Check if we are at the top (index 0) and not currently loading
                    if (firstVisibleItemPosition == 0 && dy < 0) { // dy < 0 means scrolling up
                        viewModel.loadPreviousMessages()
                    }
                }
            })
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
        lifecycleScope.launch {
            // repeatOnLifecycle pauses the collection when the app is in the background
            // This saves battery and prevents crashes when the view is destroyed
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {

                // Listen to the flow
                viewModel.messages.collectLatest { messages ->
                    val layoutManager = binding.rvMessages.layoutManager as LinearLayoutManager
                    val firstVisibleItemIndex = layoutManager.findFirstVisibleItemPosition()
                    val oldItemCount = messageAdapter.itemCount

                    Log.d("ConversationActivity", "Received $messages")
                    messageAdapter.submitList(messages) {
                        if (messages.size > oldItemCount && oldItemCount > 0) {
                            val newItemsCount = messages.size - oldItemCount
                            // Scroll to the item that WAS at the top, so the view doesn't jump
                            val targetPosition = firstVisibleItemIndex + newItemsCount
                            // scrollToPositionWithOffset is smoother than scrollToPosition
                            layoutManager.scrollToPositionWithOffset(targetPosition, 0)
                        }
                        // If it's the INITIAL load (oldItemCount == 0), scroll to bottom
                        else if (oldItemCount == 0 && messages.isNotEmpty()) {
                            binding.rvMessages.scrollToPosition(messages.size - 1)
                            binding.etMessage.text.clear()
                        }
                    }
                }
            }
        }

        viewModel.isLoadingMore.observe(this, Observer { isLoadingMore ->
            // Show/hide loading indicator when fetching previous messages
            if (isLoadingMore) {
                Log.d("ConversationActivity", "Loading more messages...")
            }
        })

        viewModel.isUploading.observe(this, Observer { isUploading ->
            // Additional feedback for image uploads
            if (isUploading) {
                binding.etMessage.hint = "Uploading images..."
            } else {
                binding.etMessage.hint = "Message..."
            }
        })

        viewModel.error.observe(this, Observer { errorMessage ->
            if (errorMessage != null) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        })

        viewModel.sendMessageResult.observe(this, Observer { success ->
            if (success == true) {
                // Clear inputs on main thread - this is lightweight
                runOnUiThread {
                    binding.etMessage.text.clear()
                    clearSelectedImages()
                }
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
            showImageSelectionDialog()
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
                requestStoragePermissionLauncher.launch(permission)
            }
        }
    }

    private fun checkPermissionAndOpenCamera() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openImagePicker() {
        pickImageLauncher.launch("image/*")
    }

    private fun openCamera() {
        try {
            // Create image file
            val imageFile = createImageFile()
            photoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                imageFile
            )
            
            // Launch camera
            takePictureLauncher.launch(photoUri)
        } catch (ex: Exception) {
            Toast.makeText(this, "Error opening camera: ${ex.message}", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(java.io.IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs()
        }
        
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
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
        
        // Disable send button immediately to prevent multiple sends
        binding.btnSend.isEnabled = false
        
        // Convert URIs to strings on main thread (lightweight operation)
        val imageUriStrings = selectedImageUris.map { it.toString() }.toMutableList()
        
        // Send message (heavy operations will be done in background)
        viewModel.sendMessage(messageText, imageUriStrings)
    }

    private fun openFullscreenImageView(imageUrls: List<String>, initialPosition: Int) {
        val intent = FullscreenImageActivity.createIntent(this, imageUrls, initialPosition)
        startActivity(intent)
    }
    
    private fun clearSelectedImages() {
        selectedImageUris.clear()
        currentPhotoPath = null
        photoUri = Uri.EMPTY
        updateImagePreview()
    }

    private fun showImageSelectionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_image_selection, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Set custom background
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        dialogView.findViewById<android.view.View>(R.id.ll_camera).setOnClickListener {
            dialog.dismiss()
            checkPermissionAndOpenCamera()
        }

        dialogView.findViewById<android.view.View>(R.id.ll_gallery).setOnClickListener {
            dialog.dismiss()
            checkPermissionAndPickImages()
        }

        dialog.show()
    }

    override fun onDestroy() {
        // MEMORY FIX: Clean up all resources to prevent leaks
        selectedImageUris.clear()
        currentPhotoPath = null
        photoUri = Uri.EMPTY

        super.onDestroy()
    }
}
