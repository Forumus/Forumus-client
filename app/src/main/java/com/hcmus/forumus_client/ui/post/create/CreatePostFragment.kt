package com.hcmus.forumus_client.ui.post.create

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.databinding.FragmentCreatePostBinding
import com.hcmus.forumus_client.data.model.TopicItem
import java.io.File
import kotlin.compareTo
import kotlin.getValue
import kotlin.math.abs

class CreatePostFragment : Fragment() {
    private lateinit var binding: FragmentCreatePostBinding
    private val viewModel: CreatePostViewModel by viewModels()
    private lateinit var imageAdapter: SelectedImageAdapter
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    private val MAX_TOPIC_LIMIT = 5
    private var tempImageUri: Uri? = null

    // --- DANH SÁCH 23 CHỦ ĐỀ KÈM ICON ---
    private val fullTopicData = listOf(
        TopicItem("Analytical Chemistry", "🧪"),
        TopicItem("Artificial Intelligence", "🤖"),
        TopicItem("Astrobiology", "🔭"),
        TopicItem("Astronomy", "🌟"),
        TopicItem("Biology", "🧬"),
        TopicItem("Biophysics", "⚡"),
        TopicItem("Biotechnology", "🧫"),
        TopicItem("Chemistry", "⚗️"),
        TopicItem("Computational Science", "💻"),
        TopicItem("Computer Science", "💾"),
        TopicItem("Earth & Atmospheric Science", "🌍"),
        TopicItem("Environmental Science", "🌿"),
        TopicItem("Genetics", "🧬"),
        TopicItem("Geology", "🪨"),
        TopicItem("Materials Science", "⚗️"),
        TopicItem("Mathematics", "📐"),
        TopicItem("Nanotechnology", "🔬"),
        TopicItem("Physics", "⚛️"),
        TopicItem("Quantum Computing", "💻"),
        TopicItem("Robotics", "🤖"),
        TopicItem("Statistics & Data Science", "📊"),
        TopicItem("Theoretical Physics", "🌌"),
        TopicItem("XAI", "🧠")
    )

    // List String để lưu tên topic đã chọn gửi lên Firebase
    private val selectedTopicsList = ArrayList<String>()

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
        else Toast.makeText(requireContext(), "Camera and Audio permissions are required to record video", Toast.LENGTH_SHORT).show()
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) launchCamera(isVideo = false)
        else Toast.makeText(requireContext(), "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
    }

    private val requestStoragePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.entries.any { it.value }) launchPhotoPicker()
        else Toast.makeText(requireContext(), "Photo access permission is required", Toast.LENGTH_SHORT).show()
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImages(uris)
            setBottomSheetState(false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBottomSheet()
        setupRecyclerView()
        setupListeners()
        setupObservers()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { handleExit() }
        validatePostButton()
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener { handleExit() }

        // Định nghĩa các sự kiện click
        val onCameraClick = View.OnClickListener { showCameraModeSelection() }
        val onPhotoClick = View.OnClickListener { checkPermissionAndPickImage() }
        val onTopicClick = View.OnClickListener { showTopicSelectionDialog() }

        // Gán sự kiện cho BottomSheet (List to)
        binding.btnCamera.setOnClickListener(onCameraClick)
        binding.btnAttachImage.setOnClickListener(onPhotoClick)

        // Tìm nút Topic trong layout dynamic (List to)
        val layoutFullActions = binding.layoutFullActions
        if (layoutFullActions.childCount > 3) {
            layoutFullActions.getChildAt(3).setOnClickListener(onTopicClick)
        }

        // Gán sự kiện cho Toolbar nhỏ
        binding.btnQuickPhoto.setOnClickListener(onPhotoClick)
        binding.btnQuickCamera.setOnClickListener(onCameraClick)
        binding.btnQuickTopic.setOnClickListener(onTopicClick)

        binding.btnMoreOptions.setOnClickListener { setBottomSheetState(true) }

        // Text Watcher
        val textWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { validatePostButton() }
            override fun afterTextChanged(s: android.text.Editable?) {}
        }
        binding.edtTitle.addTextChangedListener(textWatcher)
        binding.edtContent.addTextChangedListener(textWatcher)

        // Nút Đăng bài
        binding.btnSubmitPost.setOnClickListener {
            val title = binding.edtTitle.text.toString().trim()
            val content = binding.edtContent.text.toString().trim()
            // Gọi ViewModel lưu Firebase
            viewModel.createPost(title, content, selectedTopicsList, requireContext())
        }
    }

    private fun setupObservers() {
        viewModel.postState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is PostState.Loading -> {
                    binding.btnSubmitPost.isEnabled = false
                    binding.btnSubmitPost.text = "Posting..."
                }
                is PostState.Success -> {
                    Toast.makeText(requireContext(), "Post created successfully!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack() // Đóng màn hình quay về Home
                }
                is PostState.Error -> {
                    binding.btnSubmitPost.isEnabled = true
                    binding.btnSubmitPost.text = "POST"
                    Toast.makeText(requireContext(), state.msg, Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.selectedImages.observe(viewLifecycleOwner) { images ->
            if (images.isNullOrEmpty()) {
                binding.rvSelectedImages.visibility = View.GONE
                setBottomSheetState(true)
            } else {
                binding.rvSelectedImages.visibility = View.VISIBLE
                imageAdapter.submitList(images.toList())
                setBottomSheetState(false)
            }
            validatePostButton()
        }
    }

    // --- MENU TOPIC MỚI (GRID + EMOJI) ---
    private fun showTopicSelectionDialog() {
        val dialog = BottomSheetDialog(requireContext())
        // Inflate layout dialog mới (chứa RecyclerView)
        val dialogView = layoutInflater.inflate(R.layout.layout_dialog_topic_selection, null)
        dialog.setContentView(dialogView)

        val rvTopics = dialogView.findViewById<RecyclerView>(R.id.rv_topics)
        val btnDone = dialogView.findViewById<Button>(R.id.btn_done_selection)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btn_close_dialog)
        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tv_subtitle)

        // Chuẩn bị dữ liệu: Clone list gốc và đánh dấu những item đang được chọn
        val currentItems = fullTopicData.map { it.copy(isSelected = selectedTopicsList.contains(it.name)) }

        // Setup Adapter (TopicAdapter)
        val topicAdapter = TopicAdapter(currentItems, MAX_TOPIC_LIMIT)
        rvTopics.adapter = topicAdapter
        // Setup Layout Manager: Grid 2 cột
        rvTopics.layoutManager = GridLayoutManager(requireContext(), 2)

        // Xử lý nút Done
        btnDone.setOnClickListener {
            val newSelectedNames = topicAdapter.getSelectedTopics()
            selectedTopicsList.clear()
            selectedTopicsList.addAll(newSelectedNames)

            updateTopicChips() // Cập nhật chip trên màn hình chính
            validatePostButton()
            dialog.dismiss()
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.show()
    }

    // --- CẬP NHẬT CHIP TRÊN UI ---
    private fun updateTopicChips() {
        binding.chipGroupTopics.removeAllViews()
        selectedTopicsList.forEach { topicName ->
            val chip = Chip(requireContext())
            // Tìm icon tương ứng để hiển thị trên chip (nếu muốn)
            val icon = fullTopicData.find { it.name == topicName }?.icon ?: ""
            chip.text = "$icon $topicName"

            chip.isCheckable = false
            chip.isCloseIconVisible = true

            // Màu nền: Random pastel theo tên
            chip.chipBackgroundColor = ColorStateList.valueOf(getColorForTopic(topicName))
            chip.setTextColor(Color.BLACK)

            chip.setOnCloseIconClickListener {
                selectedTopicsList.remove(topicName)
                updateTopicChips() // Vẽ lại
            }
            binding.chipGroupTopics.addView(chip)
        }
    }

    // --- CAMERA & PERMISSIONS ---
    private fun showCameraModeSelection() {
        val options = arrayOf("Take Photo", "Record Video")
        AlertDialog.Builder(requireContext())
            .setTitle("Choose Action")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        Log.d("CreatePost", "Take Photo clicked")
                        val hasCameraPermission = hasPermission(Manifest.permission.CAMERA)
                        Log.d("CreatePost", "Camera permission: $hasCameraPermission")
                        if (hasCameraPermission) {
                            Log.d("CreatePost", "Launching camera directly")
                            launchCamera(isVideo = false)
                        } else {
                            Log.d("CreatePost", "Requesting camera permission")
                            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                    1 -> {
                        Log.d("CreatePost", "Record Video clicked")
                        val hasCameraPermission = hasPermission(Manifest.permission.CAMERA)
                        val hasAudioPermission = hasPermission(Manifest.permission.RECORD_AUDIO)
                        Log.d("CreatePost", "Camera: $hasCameraPermission, Audio: $hasAudioPermission")
                        if (hasCameraPermission && hasAudioPermission) {
                            Log.d("CreatePost", "Launching camera directly for video")
                            launchCamera(isVideo = true)
                        } else {
                            Log.d("CreatePost", "Requesting camera and audio permissions")
                            requestVideoPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                        }
                    }
                }
            }
            .show()
    }

    private fun launchCamera(isVideo: Boolean) {
        try {
            Log.d("CreatePost", "launchCamera called with isVideo=$isVideo")
            val fileName = "captured_${System.currentTimeMillis()}"
            val suffix = if (isVideo) ".mp4" else ".jpg"
            val file = File.createTempFile(fileName, suffix, requireContext().cacheDir)
            tempImageUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
            Log.d("CreatePost", "Created temp file: ${file.absolutePath}")
            Log.d("CreatePost", "FileProvider URI: $tempImageUri")
            if (isVideo) {
                Log.d("CreatePost", "Launching video launcher")
                takeVideoLauncher.launch(tempImageUri!!)
            } else {
                Log.d("CreatePost", "Launching photo launcher")
                takePhotoLauncher.launch(tempImageUri!!)
            }
        } catch (e: Exception) {
            Log.e("CreatePost", "Error in launchCamera", e)
            e.printStackTrace()
        }
    }

    private fun previewMedia(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
        val mimeType = requireContext().contentResolver.getType(uri) ?: "video/mp4"
        intent.setDataAndType(uri, mimeType)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try { startActivity(intent) } catch (e: Exception) { Toast.makeText(requireContext(), "No app found to open this file", Toast.LENGTH_SHORT).show() }
    }

    private fun hasPermission(permission: String) = ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED

    private fun checkPermissionAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (hasPermission(Manifest.permission.READ_MEDIA_IMAGES)) launchPhotoPicker() else requestStoragePermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO))
        } else {
            if (hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) launchPhotoPicker() else requestStoragePermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }

    private fun launchPhotoPicker() = pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    private fun getColorForTopic(topic: String): Int {
        val hash = abs(topic.hashCode())
        // Bảng màu pastel nhẹ nhàng
        val colors = listOf(
            0xFFE3F2FD.toInt(), // Blue
            0xFFE8F5E9.toInt(), // Green
            0xFFFFF3E0.toInt(), // Orange
            0xFFFFEBEE.toInt(), // Red
            0xFFF3E5F5.toInt(), // Purple
            0xFFE0F7FA.toInt(), // Cyan
            0xFFFFF8E1.toInt(), // Amber
            0xFFF1F8E9.toInt()  // Light Green
        )
        return colors[hash % colors.size]
    }

    // --- UI HELPERS ---
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
                if (slideOffset < 0) {
                    binding.layoutBottomToolbar.visibility = View.VISIBLE
                    binding.layoutBottomToolbar.alpha = 1f - (slideOffset + 1)
                }
            }
        })
    }

    private fun setupRecyclerView() {
        // Setup Adapter với đầy đủ 2 tham số: Xóa và Xem
        imageAdapter = SelectedImageAdapter(
            onDelete = { uriToRemove -> viewModel.removeImage(uriToRemove) },
            onItemClick = { uriToView -> previewMedia(uriToView) }
        )
        binding.rvSelectedImages.apply {
            adapter = imageAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun handleExit() {
        val title = binding.edtTitle.text.toString().trim()
        val content = binding.edtContent.text.toString().trim()
        val hasImages = !viewModel.selectedImages.value.isNullOrEmpty()
        if (title.isNotEmpty() || content.isNotEmpty() || hasImages) showExitConfirmationDialog() else findNavController().popBackStack()
    }

    private fun showExitConfirmationDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.layout_exit_confirmation, null)
        dialog.setContentView(view)
        view.findViewById<View>(R.id.btnSaveDraft).setOnClickListener { dialog.dismiss(); findNavController().popBackStack() }
        view.findViewById<View>(R.id.btnDiscardPost).setOnClickListener { dialog.dismiss(); findNavController().popBackStack() }
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