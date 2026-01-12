package com.hcmus.forumus_client.ui.post.create

import android.Manifest
import android.app.Activity
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
// --- CÁC IMPORT CỦA GOOGLE PLACES ---
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
// ------------------------------------
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
    private lateinit var placesClient: PlacesClient

    // Biến lưu địa điểm đã chọn
    private var selectedLocationName: String? = null
    private var selectedLat: Double? = null
    private var selectedLng: Double? = null

    private val MAX_TOPIC_LIMIT = 5
    private var tempImageUri: Uri? = null
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
        else Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) launchCamera(isVideo = false)
        else Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
    }

    private val requestStoragePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.entries.any { it.value }) launchPhotoPicker()
        else Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImages(uris)
            setBottomSheetState(false)
        }
    }

    // Launcher cho Google Map Autocomplete
    private val startAutocomplete = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            if (intent != null) {
                val place = Autocomplete.getPlaceFromIntent(intent)
                handlePlaceSelection(place)

                // Lưu dữ liệu
                selectedLocationName = place.name
                selectedLat = place.latLng?.latitude
                selectedLng = place.latLng?.longitude

                // Hiển thị lên TextView (Đã thêm ID trong XML)
                binding.tvLocation.text = place.name
                binding.tvLocation.visibility = View.VISIBLE

                // Đổi màu nút Location để báo hiệu
                binding.btnQuickLocation.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary))
            }
        } else if (result.resultCode == AutocompleteActivity.RESULT_ERROR) {
            Toast.makeText(requireContext(), "Error picking location", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Init Google Places (Thay API Key thật của bạn)
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), "AIzaSyBSvLkWXEj9agyzUv2bzi4AA1ihj7pnxmY")
        }
        placesClient = Places.createClient(requireContext())

        setupBottomSheet()
        setupRecyclerView()
        setupListeners()
        setupObservers()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { handleExit() }
        validatePostButton()
        viewModel.getAllTopics()
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener { handleExit() }

        // --- ĐỊNH NGHĨA CÁC HÀM CLICK ---
        val onCameraClick = View.OnClickListener { showCameraModeSelection() }
        val onPhotoClick = View.OnClickListener { checkPermissionAndPickImage() }
        val onTopicClick = View.OnClickListener { showTopicSelectionDialog() }

        // Hàm mở Google Map
        val onLocationClick = View.OnClickListener {
            // Lấy URL avatar của user hiện tại từ ViewModel
            val currentUser = viewModel.currentUser.value
            val avatarUrl = currentUser?.profilePictureUrl

            val locationSheet = LocationPickerBottomSheet(
                userAvatarUrl = avatarUrl,
                onLocationSelected = { place ->
                    // Xử lý khi user chọn từ List và bấm Add
                    handlePlaceSelection(place)
                },
                onSearchClick = {
                    // Mở Google Autocomplete nếu user muốn tìm kiếm
                    val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
                    val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                        .build(requireContext())
                    startAutocomplete.launch(intent)
                }
            )
            locationSheet.show(parentFragmentManager, "LocationPicker")
        }

        // --- GÁN SỰ KIỆN CHO BOTTOM SHEET (Đã có ID từ XML mới) ---
        binding.btnCamera.setOnClickListener(onCameraClick)
        binding.btnAttachImage.setOnClickListener(onPhotoClick)
        binding.btnAddTopic.setOnClickListener(onTopicClick) // Đã sửa XML có ID này
        binding.btnCheckIn.setOnClickListener(onLocationClick) // Đã sửa XML có ID này

        // --- GÁN SỰ KIỆN CHO QUICK TOOLBAR ---
        binding.btnQuickPhoto.setOnClickListener(onPhotoClick)
        binding.btnQuickCamera.setOnClickListener(onCameraClick)
        binding.btnQuickTopic.setOnClickListener(onTopicClick)
        binding.btnQuickLocation.setOnClickListener(onLocationClick) // Đã sửa XML có ID này

        binding.btnMoreOptions.setOnClickListener { setBottomSheetState(true) }

        // Xóa địa điểm đã chọn
        binding.tvLocation.setOnClickListener {
            selectedLocationName = null
            selectedLat = null
            selectedLng = null
            binding.tvLocation.visibility = View.GONE
            binding.tvLocation.text = ""
            binding.btnQuickLocation.clearColorFilter()
        }

        // Validate Input
        val textWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { validatePostButton() }
            override fun afterTextChanged(s: android.text.Editable?) {}
        }
        binding.edtTitle.addTextChangedListener(textWatcher)
        binding.edtContent.addTextChangedListener(textWatcher)

        // Submit Post
        binding.btnSubmitPost.setOnClickListener {
            val title = binding.edtTitle.text.toString().trim()
            val content = binding.edtContent.text.toString().trim()

            viewModel.createPost(
                title = title,
                content = content,
                selectedTopics = selectedTopicsList,
                context = requireContext(),
                locationName = selectedLocationName,
                lat = selectedLat,
                lng = selectedLng
            )
        }
    }

    private fun handlePlaceSelection(place: Place) {
        selectedLocationName = place.name
        selectedLat = place.latLng?.latitude
        selectedLng = place.latLng?.longitude

        binding.tvLocation.text = place.name
        binding.tvLocation.visibility = View.VISIBLE
        binding.btnQuickLocation.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary))
    }

    private fun setupObservers() {
        viewModel.postState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is PostState.Loading -> {
                    binding.btnSubmitPost.isEnabled = false
                    binding.btnSubmitPost.text = "Posting..."
                }
                is PostState.Success -> {
                    Toast.makeText(requireContext(), "Post submitted for review", Toast.LENGTH_SHORT).show()
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

        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.tvAuthorName.text = user.fullName.ifEmpty { "User" }
                binding.tvAuthorEmail.text = user.email
                val url = if (!user.profilePictureUrl.isNullOrEmpty()) user.profilePictureUrl else "https://ui-avatars.com/api/?name=${user.fullName}"
                Glide.with(this).load(url).circleCrop().into(binding.ivAuthorAvatar)
            }
        }

        viewModel.topicColors.observe(viewLifecycleOwner) {
            if (selectedTopicsList.isNotEmpty()) updateTopicChips()
        }
        viewModel.suggestedTopics.observe(viewLifecycleOwner) { /* AI Logic */ }
    }

    // --- CÁC HÀM UI HELPERS KHÁC (Topic Dialog, Camera...) GIỮ NGUYÊN ---
    // (Phần này code cũ của bạn đã ổn, mình giữ nguyên cấu trúc)

    private fun showTopicSelectionDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.layout_dialog_topic_selection, null)
        dialog.setContentView(dialogView)

        val rvTopics = dialogView.findViewById<RecyclerView>(R.id.rv_topics)
        val btnDone = dialogView.findViewById<Button>(R.id.btn_done_selection)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btn_close_dialog)
        val btnAiSuggestion = dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.btn_ai_suggestion)
        val progressAi = dialogView.findViewById<android.widget.ProgressBar>(R.id.progress_ai)
        val tvAiEmoji = dialogView.findViewById<TextView>(R.id.tv_ai_emoji)
        val tvAiText = dialogView.findViewById<TextView>(R.id.tv_ai_text)

        val currentItems = ArrayList<TopicItem>()
        val fullTopicData = viewModel.allTopics.value ?: emptyList()
        fullTopicData.forEach { topic ->
            val isSelected = selectedTopicsList.contains(topic.name)
            currentItems.add(TopicItem(topic.name, isSelected))
        }

        val topicAdapter = TopicAdapter(currentItems, MAX_TOPIC_LIMIT)
        rvTopics.adapter = topicAdapter
        rvTopics.layoutManager = GridLayoutManager(requireContext(), 2)

        btnAiSuggestion.setOnClickListener {
            val title = binding.edtTitle.text.toString().trim()
            val content = binding.edtContent.text.toString().trim()
            if (title.isEmpty() && content.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter content first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            progressAi.visibility = View.VISIBLE
            tvAiEmoji.visibility = View.GONE
            tvAiText.text = "Loading..."
            btnAiSuggestion.isClickable = false
            viewModel.getSuggestedTopics(title, content)
        }

        viewModel.suggestedTopics.observe(viewLifecycleOwner) { suggestedTopics ->
            progressAi.visibility = View.GONE
            tvAiEmoji.visibility = View.VISIBLE
            tvAiText.text = "AI Topic Suggestion"
            btnAiSuggestion.isClickable = true

            if (!suggestedTopics.isNullOrEmpty()) {
                currentItems.forEach { it.isSelected = false }
                var selectedCount = 0
                suggestedTopics.forEach { suggestedTopic ->
                    if (selectedCount >= MAX_TOPIC_LIMIT) return@forEach
                    val matchingItem = currentItems.find { it.name == suggestedTopic.name }
                    if (matchingItem != null) {
                        matchingItem.isSelected = true
                        selectedCount++
                    }
                }
                topicAdapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), "Found suggested topics!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "No suggestions", Toast.LENGTH_SHORT).show()
            }
        }

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

    private fun updateTopicChips() {
        binding.chipGroupTopics.removeAllViews()
        selectedTopicsList.forEach { topicName ->
            val chip = Chip(requireContext())
            chip.text = topicName
            chip.isCheckable = false
            chip.isCloseIconVisible = true
            chip.chipBackgroundColor = ColorStateList.valueOf(getColorForTopic(topicName))
            chip.setTextColor(Color.BLACK)
            chip.setOnCloseIconClickListener {
                selectedTopicsList.remove(topicName)
                updateTopicChips()
            }
            binding.chipGroupTopics.addView(chip)
        }
    }

    private fun getColorForTopic(topic: String): Int {
        val hash = abs(topic.hashCode())
        val colors = listOf(0xFFE3F2FD.toInt(), 0xFFE8F5E9.toInt(), 0xFFFFF3E0.toInt(), 0xFFFFEBEE.toInt(), 0xFFF3E5F5.toInt(), 0xFFE0F7FA.toInt(), 0xFFFFF8E1.toInt(), 0xFFF1F8E9.toInt())
        return colors[hash % colors.size]
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