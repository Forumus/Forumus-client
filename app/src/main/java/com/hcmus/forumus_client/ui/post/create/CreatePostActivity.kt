package com.hcmus.forumus_client.ui.post.create

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import coil.load
import coil.transform.CircleCropTransformation
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.databinding.ActivityCreatePostBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Activity for creating and publishing new posts.
 * Allows users to add title, content, images, and videos before submission.
 * Handles file picker intents and camera intents for media capture.
 */
class CreatePostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatePostBinding
    private val viewModel: CreatePostViewModel by viewModels()

    // Activity result launchers for file picking and camera
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var takePhotoLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickVideoLauncher: ActivityResultLauncher<Intent>
    private lateinit var recordVideoLauncher: ActivityResultLauncher<Intent>

    // Current camera image URI (used for camera intent)
    private var cameraImageUri: Uri? = null
    private var cameraVideoUri: Uri? = null

    private companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
        private const val REQUEST_READ_EXTERNAL_STORAGE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModels()
        setupUI()
        setupListeners()
        setupActivityResultLaunchers()
        setupObservers()
        // User will be loaded via userId input field (Dev feature)
    }

    /**
     * Setup ViewModel observers for LiveData updates.
     */
    private fun setupViewModels() {
        // ViewModel is already initialized via viewModels() delegate
    }

    /**
     * Initialize UI with default values.
     */
    private fun setupUI() {
        // UI is populated from XML layout
    }

    /**
     * Setup click listeners for all interactive elements.
     */
    private fun setupListeners() {
        // Close button
        binding.btnClose.setOnClickListener {
            finish()
        }

        // Submit post button
        binding.btnSubmitPost.setOnClickListener {
            submitPost()
        }

        // Media selection buttons
        binding.btnPickImage.setOnClickListener {
            pickImageFromGallery()
        }

        binding.btnTakePhoto.setOnClickListener {
            takePhotoWithCamera()
        }

        binding.btnPickVideo.setOnClickListener {
            pickVideoFromGallery()
        }

        binding.btnRecordVideo.setOnClickListener {
            recordVideoWithCamera()
        }

        // User ID input listener (Dev feature)
        binding.edtUserId.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val userId = s?.toString()?.trim() ?: ""
                if (userId.isNotEmpty()) {
                    viewModel.loadUser(userId)
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Text input listeners
        binding.edtTitle.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setPostTitle(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        binding.edtContent.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setPostContent(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    /**
     * Setup activity result launchers for file/camera operations.
     */
    private fun setupActivityResultLaunchers() {
        // Pick image from gallery
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri = result.data?.data
                if (imageUri != null) {
                    val imagePath = imageUri.toString()
                    viewModel.addImageUri(imagePath)
                    Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Take photo with camera
        takePhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (cameraImageUri != null) {
                    val imagePath = cameraImageUri.toString()
                    viewModel.addImageUri(imagePath)
                    Toast.makeText(this, "Photo captured", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Pick video from gallery
        pickVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val videoUri = result.data?.data
                if (videoUri != null) {
                    val videoPath = videoUri.toString()
                    viewModel.addVideoUri(videoPath)
                    Toast.makeText(this, "Video selected", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Record video with camera
        recordVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (cameraVideoUri != null) {
                    val videoPath = cameraVideoUri.toString()
                    viewModel.addVideoUri(videoPath)
                    Toast.makeText(this, "Video recorded", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Setup observers for ViewModel LiveData.
     */
    private fun setupObservers() {
        // Observe current user
        viewModel.currentUser.observe(this) { user ->
            if (user != null) {
                binding.tvAuthorName.text = user.fullName
                binding.tvAuthorEmail.text = user.email
                
                // Load avatar with fallback
                binding.ivAuthorAvatar.load(user.profilePictureUrl) {
                    crossfade(true)
                    placeholder(R.drawable.default_avatar)
                    error(R.drawable.default_avatar)
                    transformations(CircleCropTransformation())
                }
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnSubmitPost.isEnabled = !isLoading
            binding.btnSubmitPost.text = if (isLoading) "POSTING..." else "POST"
        }

        // Observe error messages
        viewModel.error.observe(this) { errorMessage ->
            if (!errorMessage.isNullOrBlank()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        }

        // Observe success messages
        viewModel.successMessage.observe(this) { successMessage ->
            if (!successMessage.isNullOrBlank()) {
                Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
                // Close activity after successful post creation
                finish()
            }
        }

        // Observe selected images
        viewModel.selectedImageUris.observe(this) { uris ->
            // Update UI to show selected media count
            if (uris.isNotEmpty()) {
                Toast.makeText(this, "${uris.size} image(s) selected", Toast.LENGTH_SHORT).show()
            }
        }

        // Observe selected videos
        viewModel.selectedVideoUris.observe(this) { uris ->
            // Update UI to show selected media count
            if (uris.isNotEmpty()) {
                Toast.makeText(this, "${uris.size} video(s) selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Open gallery to pick image.
     */
    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    /**
     * Open camera to take photo.
     */
    private fun takePhotoWithCamera() {
        if (!hasCameraPermission()) {
            requestCameraPermission()
            return
        }

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        
        // Create file for saving photo
        val photoFile = createImageFile()
        if (photoFile != null) {
            cameraImageUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
            takePhotoLauncher.launch(intent)
        }
    }

    /**
     * Open gallery to pick video.
     */
    private fun pickVideoFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        intent.type = "video/*"
        pickVideoLauncher.launch(intent)
    }

    /**
     * Open camera to record video.
     */
    private fun recordVideoWithCamera() {
        if (!hasCameraPermission()) {
            requestCameraPermission()
            return
        }

        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        
        // Create file for saving video
        val videoFile = createVideoFile()
        if (videoFile != null) {
            cameraVideoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                videoFile
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraVideoUri)
            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1) // High quality
            recordVideoLauncher.launch(intent)
        }
    }

    /**
     * Create a new image file in the app's cache directory.
     *
     * @return File object for the image, or null if creation failed
     */
    private fun createImageFile(): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "IMG_${timestamp}_"
            val storageDir = cacheDir
            File.createTempFile(imageFileName, ".jpg", storageDir)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Create a new video file in the app's cache directory.
     *
     * @return File object for the video, or null if creation failed
     */
    private fun createVideoFile(): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val videoFileName = "VID_${timestamp}_"
            val storageDir = cacheDir
            File.createTempFile(videoFileName, ".mp4", storageDir)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if camera permission is granted.
     *
     * @return True if permission is granted, false otherwise
     */
    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request camera permission from user.
     */
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            REQUEST_CAMERA_PERMISSION
        )
    }

    /**
     * Handle permission request result.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Submit the post with current content and media.
     * Validates input and calls ViewModel to submit post.
     */
    private fun submitPost() {
        val title = binding.edtTitle.text.toString().trim()
        val content = binding.edtContent.text.toString().trim()

        if (content.isEmpty()) {
            Toast.makeText(this, "Post content cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        // Submit via ViewModel
        viewModel.submitPost(this)
    }
}
