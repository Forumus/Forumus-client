package com.hcmus.forumus_client.ui.conversation

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.data.model.Message
import com.hcmus.forumus_client.data.model.MessageType
import com.hcmus.forumus_client.databinding.FragmentConversationBinding
import com.hcmus.forumus_client.ui.image.FullscreenImageActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import androidx.fragment.app.Fragment
import kotlin.getValue
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs

class ConversationFragment : Fragment() {
    private lateinit var binding: FragmentConversationBinding
    private lateinit var messageAdapter: ConversationAdapter
    private lateinit var imagePreviewAdapter: ImagePreviewAdapter
    private val viewModel: ConversationViewModel by viewModels()
    private val navController by lazy { findNavController() }
    private var chatId: String? = null
    private var selectedImageUris: MutableList<Uri> = mutableListOf()
    private var currentPhotoPath: String? = null
    private var photoUri: Uri = Uri.EMPTY
    private val args: ConversationFragmentArgs by navArgs()

    companion object {
        private const val MAX_IMAGES = 5
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentConversationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatId = args.id

        if (chatId == "" || chatId == null) {
            Toast.makeText(requireContext(), "Invalid chat", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressedDispatcher.onBackPressed()
            return
        }

        setupUI()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        // Load messages
        chatId?.let { viewModel.loadMessages(it) }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            if (uris.size + selectedImageUris.size > MAX_IMAGES) {
                Toast.makeText(
                    requireContext(),
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
                    requireContext(),
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
            Toast.makeText(requireContext(), "Storage permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupUI() {
        // Get user info from intent
        val userName = args.contactName
        val userEmail = args.email
        val userPictureUrl = args.profilePictureUrl

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
        messageAdapter = ConversationAdapter(currentUserId, viewPool, { imageUrls, initialPosition ->
            openFullscreenImageView(imageUrls, initialPosition)
        }, { message ->
            // Handle message long-click for deletion
            showDeleteMessageDialog(message)
        })

        binding.rvMessages.apply {
            adapter = messageAdapter
            // Optimization: Cache more view holders offscreen to prevent re-binding on small scrolls
            setItemViewCacheSize(20)
            layoutManager = LinearLayoutManager(requireContext()).apply {
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
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            // repeatOnLifecycle pauses the collection when the app is in the background
            // This saves battery and prevents crashes when the view is destroyed
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {

                launch {
                    viewModel.isUploading.collectLatest { isUploading ->
                        if (isUploading) {
                            binding.etMessage.hint = "Uploading images..."
                        } else {
                            binding.etMessage.hint = "Message..."
                        }
                    }
                }

                launch {
                    viewModel.isLoading.collectLatest { isLoading ->
                        binding.btnSend.visibility = if (isLoading) android.view.View.GONE else android.view.View.VISIBLE
                        binding.pbSending.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
                    }
                }

                launch {
                    viewModel.sendMessageResult.collectLatest { success ->
                        if (success) {
                            // Clear inputs on main thread - this is lightweight
                            binding.etMessage.text.clear()
                            binding.btnSend.isEnabled = true
                            clearSelectedImages()
                        }
                        viewModel.clearSendResult()
                    }
                }

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
                        }
                        binding.etMessage.text.clear()

                    }
                }
            }
        }

        viewModel.isLoadingMore.observe(viewLifecycleOwner, Observer { isLoadingMore ->
            // Show/hide loading indicator when fetching previous messages
            if (isLoadingMore) {
                Log.d("ConversationActivity", "Loading more messages...")
            }
        })

        viewModel.error.observe(viewLifecycleOwner, Observer { errorMessage ->
            if (errorMessage != null) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        })
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
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
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED -> {
                openImagePicker()
            }
            else -> {
                requestStoragePermissionLauncher.launch(permission)
            }
        }
    }

    private fun checkPermissionAndOpenCamera() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
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
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                imageFile
            )

            // Launch camera
            takePictureLauncher.launch(photoUri)
        } catch (ex: Exception) {
            Toast.makeText(requireContext(), "Error opening camera: ${ex.message}", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(java.io.IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)

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
            Toast.makeText(requireContext(), "Please enter a message or select images", Toast.LENGTH_SHORT).show()
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
        val intent = FullscreenImageActivity.createIntent(requireContext(), imageUrls, initialPosition)
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
        val dialog = AlertDialog.Builder(requireContext())
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

    private fun showDeleteMessageDialog(message: Message) {
        // Only allow deletion of own messages that are not already deleted
        if (message.type == MessageType.DELETED) {
            Toast.makeText(requireContext(), "This message has already been deleted", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUserId = viewModel.getCurrentUserId()
        if (message.senderId != currentUserId) {
            Toast.makeText(requireContext(), "You can only delete your own messages", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_message, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Set custom background
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        dialogView.findViewById<android.widget.Button>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<android.widget.Button>(R.id.btn_delete).setOnClickListener {
            dialog.dismiss()
            viewModel.deleteMessage(message.id)
            Toast.makeText(requireContext(), "Message deleted", Toast.LENGTH_SHORT).show()
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