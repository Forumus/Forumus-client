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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.data.model.TopicItem
import com.hcmus.forumus_client.databinding.FragmentCreatePostBinding
import java.io.File
import kotlin.math.abs

class CreatePostFragment : Fragment() {
    private lateinit var binding: FragmentCreatePostBinding
    private val viewModel: CreatePostViewModel by viewModels()
    private lateinit var imageAdapter: SelectedImageAdapter
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    private val MAX_TOPIC_LIMIT = 5
    private var tempImageUri: Uri? = null

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

    private val selectedTopicsList = ArrayList<String>()

    // --- LAUNCHERS (Giữ nguyên) ---
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
        val onCameraClick = View.OnClickListener { showCameraModeSelection() }
        val onPhotoClick = View.OnClickListener { checkPermissionAndPickImage() }
        val onTopicClick = View.OnClickListener { showTopicSelectionDialog() }

        binding.btnCamera.setOnClickListener(onCameraClick)
        binding.btnAttachImage.setOnClickListener(onPhotoClick)
        val layoutFullActions = binding.layoutFullActions
        if (layoutFullActions.childCount > 3) {
            layoutFullActions.getChildAt(3).setOnClickListener(onTopicClick)
        }
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
                    findNavController().popBackStack()
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
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.tvAuthorName.text = user.fullName.ifEmpty { "User Name" }
                binding.tvAuthorEmail.text = user.email
                val avatarToLoad = if (!user.profilePictureUrl.isNullOrEmpty()) user.profilePictureUrl
                else "https://ui-avatars.com/api/?name=${user.fullName}&background=2196F3&color=fff&size=128"
                Glide.with(requireContext()).load(avatarToLoad).placeholder(R.drawable.default_avatar).error(R.drawable.default_avatar).circleCrop().into(binding.ivAuthorAvatar)
            }
        }
        // Cập nhật chip khi màu đã tải về
        viewModel.topicColors.observe(viewLifecycleOwner) {
            if (selectedTopicsList.isNotEmpty()) updateTopicChips()
        }
    }

    // --- CẬP NHẬT CHIP VỚI ALPHA ---
    private fun updateTopicChips() {
        binding.chipGroupTopics.removeAllViews()
        val colorMap = viewModel.topicColors.value ?: emptyMap()

        selectedTopicsList.forEach { topicName ->
            val chip = Chip(requireContext())

            // 1. Chỉ hiện tên, bỏ icon
            chip.text = topicName
            chip.isCheckable = false
            chip.isCloseIconVisible = true
            chip.setOnCloseIconClickListener {
                selectedTopicsList.remove(topicName)
                updateTopicChips()
            }

            // 2. Xử lý màu sắc với Alpha
            val appearance = colorMap[topicName]
            if (appearance != null) {
                try {
                    val baseColor = Color.parseColor(appearance.colorHex)

                    // Tính toán màu nền với Alpha (VD: 0.125 * 255 = ~32)
                    val alphaInt = (appearance.alpha * 255).toInt().coerceIn(0, 255)
                    val backgroundColor = Color.argb(
                        alphaInt,
                        Color.red(baseColor),
                        Color.green(baseColor),
                        Color.blue(baseColor)
                    )

                    // Set Background nhạt
                    chip.chipBackgroundColor = ColorStateList.valueOf(backgroundColor)

                    // Set Text và Icon màu đậm (100% opacity)
                    chip.setTextColor(baseColor)
                    chip.closeIconTint = ColorStateList.valueOf(baseColor)
                } catch (e: Exception) {
                    setRandomPastelColor(chip, topicName)
                }
            } else {
                setRandomPastelColor(chip, topicName)
            }
            binding.chipGroupTopics.addView(chip)
        }
    }

    private fun setRandomPastelColor(chip: Chip, topicName: String) {
        val color = getColorForTopic(topicName)
        chip.chipBackgroundColor = ColorStateList.valueOf(color)
        chip.setTextColor(Color.BLACK)
        chip.closeIconTint = ColorStateList.valueOf(Color.BLACK)
    }

    private fun getColorForTopic(topic: String): Int {
        val hash = abs(topic.hashCode())
        val colors = listOf(0xFFE3F2FD.toInt(), 0xFFE8F5E9.toInt(), 0xFFFFF3E0.toInt(), 0xFFFFEBEE.toInt(), 0xFFF3E5F5.toInt(), 0xFFE0F7FA.toInt(), 0xFFFFF8E1.toInt(), 0xFFF1F8E9.toInt())
        return colors[hash % colors.size]
    }

    // --- CÁC HÀM UI KHÁC GIỮ NGUYÊN ---
    private fun showTopicSelectionDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.layout_dialog_topic_selection, null)
        dialog.setContentView(dialogView)
        val rvTopics = dialogView.findViewById<RecyclerView>(R.id.rv_topics)
        val btnDone = dialogView.findViewById<Button>(R.id.btn_done_selection)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btn_close_dialog)

        val currentItems = fullTopicData.map { it.copy(isSelected = selectedTopicsList.contains(it.name)) }
        val topicAdapter = TopicAdapter(currentItems, MAX_TOPIC_LIMIT)
        rvTopics.adapter = topicAdapter
        rvTopics.layoutManager = GridLayoutManager(requireContext(), 2)

        btnDone.setOnClickListener {
            val newSelectedNames = topicAdapter.getSelectedTopics()
            selectedTopicsList.clear()
            selectedTopicsList.addAll(newSelectedNames)
            updateTopicChips()
            validatePostButton()
            dialog.dismiss()
        }
        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.show()
    }

    private fun showCameraModeSelection() {
        val options = arrayOf("Take Photo", "Record Video")
        AlertDialog.Builder(requireContext())
            .setTitle("Choose Action")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { if (hasPermission(Manifest.permission.CAMERA)) launchCamera(isVideo = false) else requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                    1 -> { if (hasPermission(Manifest.permission.CAMERA) && hasPermission(Manifest.permission.RECORD_AUDIO)) launchCamera(isVideo = true) else requestVideoPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) }
                }
            }
            .show()
    }

    private fun launchCamera(isVideo: Boolean) {
        try {
            val fileName = "captured_${System.currentTimeMillis()}"
            val suffix = if (isVideo) ".mp4" else ".jpg"
            val file = File.createTempFile(fileName, suffix, requireContext().cacheDir)
            tempImageUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
            if (isVideo) takeVideoLauncher.launch(tempImageUri!!) else takePhotoLauncher.launch(tempImageUri!!)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun previewMedia(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
        val mimeType = requireContext().contentResolver.getType(uri) ?: "video/mp4"
        intent.setDataAndType(uri, mimeType)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try { startActivity(intent) } catch (e: Exception) { Toast.makeText(requireContext(), "No app found", Toast.LENGTH_SHORT).show() }
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