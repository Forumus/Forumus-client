package com.hcmus.forumus_client.ui.post.create

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.databinding.ActivityCreatePostBinding
import java.io.File
import kotlin.math.abs

class CreatePostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatePostBinding
    private val viewModel: CreatePostViewModel by viewModels()
    private lateinit var imageAdapter: SelectedImageAdapter
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    private val MAX_TOPIC_LIMIT = 5
    private var tempImageUri: Uri? = null

    // --- LAUNCHERS ---
    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) tempImageUri?.let { viewModel.addImages(listOf(it)); setBottomSheetState(false) }
    }

    private val takeVideoLauncher = registerForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        if (success) tempImageUri?.let { viewModel.addImages(listOf(it)); setBottomSheetState(false) }
    }

    private val requestVideoPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (cameraGranted && audioGranted) launchCamera(isVideo = true)
        else Toast.makeText(this, "Camera & Audio required", Toast.LENGTH_SHORT).show()
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) launchCamera(isVideo = false) else Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    private val requestStoragePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.entries.any { it.value }) launchPhotoPicker() else Toast.makeText(this, "Storage permission required", Toast.LENGTH_SHORT).show()
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isNotEmpty()) { viewModel.addImages(uris); setBottomSheetState(false) }
    }

    // --- DATA ---
    private val availableTopics = arrayOf(
        "geology", "materials_science", "mathematics", "nanotechnology", "physics",
        "quantum_computing", "robotics", "statistics_&_data_science", "theoretical_physics", "xai",
        "analytical_chemistry", "artificial_intelligence", "astrobiology", "astronomy", "biology",
        "biotechnology", "computer_science", "genetics"
    )
    private val selectedTopicBooleans = BooleanArray(availableTopics.size)
    private val selectedTopicsList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomSheet()
        setupRecyclerView()
        setupListeners()
        setupObservers()

        onBackPressedDispatcher.addCallback(this) { handleExit() }
        validatePostButton()
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener { handleExit() }

        val onCameraClick = View.OnClickListener { showCameraModeSelection() }
        val onPhotoClick = View.OnClickListener { checkPermissionAndPickImage() }
        val onTopicClick = View.OnClickListener { showTopicSelectionDialog() }

        binding.btnCamera.setOnClickListener(onCameraClick)
        binding.btnAttachImage.setOnClickListener(onPhotoClick)

        // Nút Topic ở List to
        val layoutFullActions = binding.layoutFullActions
        if (layoutFullActions.childCount > 3) layoutFullActions.getChildAt(3).setOnClickListener(onTopicClick)

        // Toolbar nhỏ
        binding.btnQuickPhoto.setOnClickListener(onPhotoClick)
        binding.btnQuickCamera.setOnClickListener(onCameraClick)
        binding.btnQuickTopic.setOnClickListener(onTopicClick)

        binding.btnMoreOptions.setOnClickListener { setBottomSheetState(true) }

        val textWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { validatePostButton() }
            override fun afterTextChanged(s: android.text.Editable?) {}
        }
        binding.edtTitle.addTextChangedListener(textWatcher)
        binding.edtContent.addTextChangedListener(textWatcher)

        binding.btnSubmitPost.setOnClickListener {
            val title = binding.edtTitle.text.toString().trim()
            val content = binding.edtContent.text.toString().trim()
            viewModel.createPost(title, content, selectedTopicsList)
        }
    }

    private fun setupObservers() {
        viewModel.postState.observe(this) { state ->
            when (state) {
                is PostState.Loading -> { binding.btnSubmitPost.isEnabled = false; binding.btnSubmitPost.text = "Posting..." }
                is PostState.Success -> { Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show(); finish() }
                is PostState.Error -> { binding.btnSubmitPost.isEnabled = true; binding.btnSubmitPost.text = "POST"; Toast.makeText(this, state.msg, Toast.LENGTH_LONG).show() }
            }
        }
        viewModel.selectedImages.observe(this) { images ->
            if (images.isNullOrEmpty()) { binding.rvSelectedImages.visibility = View.GONE; setBottomSheetState(true) }
            else { binding.rvSelectedImages.visibility = View.VISIBLE; imageAdapter.submitList(images.toList()); setBottomSheetState(false) }
            validatePostButton()
        }
    }

    private fun showCameraModeSelection() {
        val options = arrayOf("Take Photo", "Record Video")
        AlertDialog.Builder(this).setTitle("Choose").setItems(options) { _, which ->
            when (which) {
                0 -> if (hasPermission(Manifest.permission.CAMERA)) launchCamera(false) else requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                1 -> if (hasPermission(Manifest.permission.CAMERA) && hasPermission(Manifest.permission.RECORD_AUDIO)) launchCamera(true) else requestVideoPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
            }
        }.show()
    }

    private fun launchCamera(isVideo: Boolean) {
        try {
            val fileName = "captured_${System.currentTimeMillis()}"
            val suffix = if (isVideo) ".mp4" else ".jpg"
            val file = File.createTempFile(fileName, suffix, cacheDir)
            tempImageUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            if (isVideo) takeVideoLauncher.launch(tempImageUri!!) else takePhotoLauncher.launch(tempImageUri!!)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun previewMedia(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
        val mimeType = contentResolver.getType(uri) ?: "video/mp4"
        intent.setDataAndType(uri, mimeType)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try { startActivity(intent) } catch (e: Exception) { Toast.makeText(this, "No app found", Toast.LENGTH_SHORT).show() }
    }

    private fun hasPermission(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    private fun checkPermissionAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (hasPermission(Manifest.permission.READ_MEDIA_IMAGES)) launchPhotoPicker() else requestStoragePermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO))
        } else {
            if (hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) launchPhotoPicker() else requestStoragePermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }

    private fun launchPhotoPicker() = pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    private fun showTopicSelectionDialog() {
        AlertDialog.Builder(this).setTitle("Topics (Max $MAX_TOPIC_LIMIT)").setMultiChoiceItems(availableTopics, selectedTopicBooleans) { dialog, which, isChecked ->
            if (isChecked) {
                if (selectedTopicBooleans.count { it } >= MAX_TOPIC_LIMIT) {
                    Toast.makeText(this, "Max $MAX_TOPIC_LIMIT", Toast.LENGTH_SHORT).show()
                    (dialog as AlertDialog).listView.setItemChecked(which, false)
                    selectedTopicBooleans[which] = false
                } else selectedTopicBooleans[which] = true
            } else selectedTopicBooleans[which] = false
        }.setPositiveButton("Done") { _, _ ->
            selectedTopicsList.clear()
            for (i in availableTopics.indices) if (selectedTopicBooleans[i]) selectedTopicsList.add(availableTopics[i])
            updateTopicChips()
            validatePostButton()
        }.setNegativeButton("Cancel", null).show()
    }

    private fun updateTopicChips() {
        binding.chipGroupTopics.removeAllViews()
        selectedTopicsList.forEach { topic ->
            val chip = Chip(this)
            chip.text = topic.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            chip.isCheckable = false; chip.isCloseIconVisible = true
            chip.chipBackgroundColor = ColorStateList.valueOf(getColorForTopic(topic)); chip.setTextColor(Color.BLACK)
            chip.setOnCloseIconClickListener { removeTopic(topic) }
            binding.chipGroupTopics.addView(chip)
        }
    }

    private fun removeTopic(topic: String) {
        val index = availableTopics.indexOf(topic)
        if (index != -1) { selectedTopicBooleans[index] = false; selectedTopicsList.remove(topic); updateTopicChips() }
    }

    private fun getColorForTopic(topic: String): Int {
        val hash = abs(topic.hashCode())
        val colors = listOf(0xFFE3F2FD.toInt(), 0xFFE8F5E9.toInt(), 0xFFFFF3E0.toInt(), 0xFFFFEBEE.toInt(), 0xFFF3E5F5.toInt(), 0xFFE0F7FA.toInt(), 0xFFFFF8E1.toInt(), 0xFFF1F8E9.toInt())
        return colors[hash % colors.size]
    }

    private fun setupBottomSheet() {
        val bottomSheet = binding.bottomSheetLayout
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        binding.layoutBottomToolbar.visibility = View.GONE
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) binding.layoutBottomToolbar.visibility = View.VISIBLE
                else if (newState == BottomSheetBehavior.STATE_EXPANDED) binding.layoutBottomToolbar.visibility = View.GONE
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (slideOffset < 0) { binding.layoutBottomToolbar.visibility = View.VISIBLE; binding.layoutBottomToolbar.alpha = 1f - (slideOffset + 1) }
            }
        })
    }

    private fun setupRecyclerView() {
        imageAdapter = SelectedImageAdapter(onDelete = { viewModel.removeImage(it) }, onItemClick = { previewMedia(it) })
        binding.rvSelectedImages.apply { adapter = imageAdapter; layoutManager = LinearLayoutManager(this@CreatePostActivity, LinearLayoutManager.HORIZONTAL, false) }
    }

    private fun handleExit() {
        val title = binding.edtTitle.text.toString().trim()
        val content = binding.edtContent.text.toString().trim()
        val hasImages = !viewModel.selectedImages.value.isNullOrEmpty()
        if (title.isNotEmpty() || content.isNotEmpty() || hasImages) showExitConfirmationDialog() else finish()
    }

    private fun showExitConfirmationDialog() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_exit_confirmation, null)
        dialog.setContentView(view)
        view.findViewById<View>(R.id.btnSaveDraft).setOnClickListener { dialog.dismiss(); finish() }
        view.findViewById<View>(R.id.btnDiscardPost).setOnClickListener { dialog.dismiss(); finish() }
        view.findViewById<View>(R.id.btnContinueEditing).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun setBottomSheetState(isExpanded: Boolean) {
        bottomSheetBehavior.state = if (isExpanded) BottomSheetBehavior.STATE_EXPANDED else BottomSheetBehavior.STATE_HIDDEN
        binding.layoutBottomToolbar.visibility = if (isExpanded) View.GONE else View.VISIBLE
    }

    private fun validatePostButton() {
        val isValid = binding.edtTitle.text.toString().trim().isNotEmpty() && binding.edtContent.text.toString().trim().isNotEmpty()
        binding.btnSubmitPost.isEnabled = isValid
        binding.btnSubmitPost.alpha = if (isValid) 1.0f else 0.3f
    }
}