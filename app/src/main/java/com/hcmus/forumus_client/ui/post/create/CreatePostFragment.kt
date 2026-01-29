package com.hcmus.forumus_client.ui.post.create

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.data.model.TopicItem
import com.hcmus.forumus_client.databinding.FragmentCreatePostBinding
import java.io.File
import kotlin.math.abs
import com.hcmus.forumus_client.BuildConfig

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

    // Cờ kiểm soát trạng thái để tránh lưu nháp khi đã post xong hoặc discard
    private var isPostSubmittedOrDiscarded = false

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) tempImageUri?.let { viewModel.addImages(listOf(it)); setBottomSheetState(false) }
    }

    private val takeVideoLauncher = registerForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        if (success) tempImageUri?.let { viewModel.addImages(listOf(it)); setBottomSheetState(false) }
    }

    private val requestVideoPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if ((permissions[Manifest.permission.CAMERA] == true) && (permissions[Manifest.permission.RECORD_AUDIO] == true)) launchCamera(isVideo = true)
        else Toast.makeText(requireContext(), getString(R.string.error_permission_denied), Toast.LENGTH_SHORT).show()
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) launchCamera(isVideo = false)
        else Toast.makeText(requireContext(), getString(R.string.error_permission_denied), Toast.LENGTH_SHORT).show()
    }

    private val requestStoragePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.entries.any { it.value }) launchPhotoPicker()
        else Toast.makeText(requireContext(), getString(R.string.error_permission_denied), Toast.LENGTH_SHORT).show()
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isNotEmpty()) { viewModel.addImages(uris); setBottomSheetState(false) }
    }

    private val startAutocomplete = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { intent ->
                val place = Autocomplete.getPlaceFromIntent(intent)
                updateLocationUI(place.name, place.latLng?.latitude, place.latLng?.longitude)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), BuildConfig.PLACES_API_KEY)
        }
        placesClient = Places.createClient(requireContext())

        setupBottomSheet()
        setupRecyclerView()
        setupListeners()
        setupObservers()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { handleExit() }
        validatePostButton()
        viewModel.getAllTopics()

        // --- KHÔI PHỤC DỮ LIỆU NHÁP (RESTORE) ---
        restoreDraftData()
    }

    // Hàm khôi phục dữ liệu từ ViewModel lên UI
    private fun restoreDraftData() {
        val draft = viewModel.restoreDraft(requireContext())
        if (draft != null) {
            // Khôi phục Text
            binding.edtTitle.setText(draft["title"] as? String ?: "")
            binding.edtContent.setText(draft["content"] as? String ?: "")

            // Khôi phục Location
            val locName = draft["locationName"] as? String
            val lat = draft["lat"] as? Double
            val lng = draft["lng"] as? Double

            if (locName != null && lat != null && lng != null) {
                updateLocationUI(locName, lat, lng)
            }

            // Khôi phục Topics
            val topics = draft["topics"] as? List<String>
            if (!topics.isNullOrEmpty()) {
                selectedTopicsList.clear()
                selectedTopicsList.addAll(topics)
                // Đợi observer allTopics load xong sẽ update màu, nhưng gọi tạm update để hiện text
                updateTopicChips()
            }
            // Ảnh đã được viewModel restore vào LiveData -> setupObservers sẽ tự update UI
            Toast.makeText(context, getString(R.string.msg_draft_restored), Toast.LENGTH_SHORT).show()
        }
    }

    // --- AUTO SAVE (TỰ ĐỘNG LƯU KHI THOÁT/ẨN APP) ---
    override fun onPause() {
        super.onPause()
        if (!isPostSubmittedOrDiscarded) {
            performSaveDraft()
        }
    }

    // Hàm thực hiện lưu nháp
    private fun performSaveDraft() {
        val title = binding.edtTitle.text.toString()
        val content = binding.edtContent.text.toString()

        // Dù rỗng cũng lưu để đảm bảo trạng thái nhất quán
        viewModel.saveDraft(
            requireContext(),
            title,
            content,
            selectedLocationName,
            selectedLat,
            selectedLng,
            selectedTopicsList
        )
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener { handleExit() }

        val onCameraClick = View.OnClickListener { showCameraModeSelection() }
        val onPhotoClick = View.OnClickListener { checkPermissionAndPickImage() }
        val onTopicClick = View.OnClickListener { showTopicSelectionDialog() }

        val onLocationClick = View.OnClickListener {
            val currentUser = viewModel.currentUser.value
            val locationSheet = LocationPickerBottomSheet(
                userAvatarUrl = currentUser?.profilePictureUrl,
                onLocationSelected = { place ->
                    updateLocationUI(place.name, place.latLng?.latitude, place.latLng?.longitude)
                },
                onSearchClick = {
                    val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
                    val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(requireContext())
                    startAutocomplete.launch(intent)
                }
            )
            locationSheet.show(parentFragmentManager, "LocationPicker")
        }

        binding.btnCamera.setOnClickListener(onCameraClick)
        binding.btnAttachImage.setOnClickListener(onPhotoClick)
        binding.btnAddTopic.setOnClickListener(onTopicClick)
        binding.btnCheckIn.setOnClickListener(onLocationClick)

        binding.btnQuickPhoto.setOnClickListener(onPhotoClick)
        binding.btnQuickCamera.setOnClickListener(onCameraClick)
        binding.btnQuickTopic.setOnClickListener(onTopicClick)
        binding.btnQuickLocation.setOnClickListener(onLocationClick)

        binding.btnMoreOptions.setOnClickListener { setBottomSheetState(true) }

        binding.layoutLocation.setOnClickListener {
            if (selectedLat != null && selectedLng != null) {
                showMapPreviewDialog(selectedLocationName ?: "Location", selectedLat!!, selectedLng!!)
            }
        }

        binding.btnRemoveLocation.setOnClickListener {
            selectedLocationName = null
            selectedLat = null
            selectedLng = null
            binding.layoutLocation.visibility = View.GONE
        }

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
            viewModel.createPost(
                title, content, selectedTopicsList, requireContext(),
                locationName = selectedLocationName, lat = selectedLat, lng = selectedLng
            )
        }
    }

    private fun handleExit() {
        // Luôn hiện dialog confirm khi thoát, cho phép người dùng chọn Save/Discard
        showExitConfirmationDialog()
    }

    private fun showExitConfirmationDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.layout_exit_confirmation, null)
        dialog.setContentView(view)

        // Nút Save as draft
        view.findViewById<View>(R.id.btnSaveDraft).setOnClickListener {
            performSaveDraft()
            isPostSubmittedOrDiscarded = false // Để onPause không cần lưu lại lần nữa (hoặc cứ để nó lưu cũng ko sao)
            Toast.makeText(context, getString(R.string.msg_draft_saved), Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            findNavController().popBackStack()
        }

        // Nút Discard post (Xóa nháp)
        view.findViewById<View>(R.id.btnDiscardPost).setOnClickListener {
            isPostSubmittedOrDiscarded = true // Đánh dấu để onPause KHÔNG lưu lại
            viewModel.clearDraft(requireContext())
            dialog.dismiss()
            findNavController().popBackStack()
        }

        view.findViewById<View>(R.id.btnContinueEditing).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun updateLocationUI(name: String?, lat: Double?, lng: Double?) {
        selectedLocationName = name
        selectedLat = lat
        selectedLng = lng

        if (name != null) {
            binding.tvLocationName.text = name
            binding.layoutLocation.visibility = View.VISIBLE
        }
    }

    private fun showMapPreviewDialog(name: String, lat: Double, lng: Double) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.layout_dialog_map_preview)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val mapView = dialog.findViewById<MapView>(R.id.mapView)
        val tvTitle = dialog.findViewById<TextView>(R.id.tvSelectedPlaceName)
        val btnClose = dialog.findViewById<Button>(R.id.btnCloseMap)

        tvTitle.text = name
        btnClose.text = getString(R.string.action_close)

        MapsInitializer.initialize(requireContext())
        mapView.onCreate(dialog.onSaveInstanceState())
        mapView.onResume()

        mapView.getMapAsync { googleMap ->
            val latLng = LatLng(lat, lng)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
            loadAvatarMarker(googleMap, latLng)
        }
        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun loadAvatarMarker(googleMap: com.google.android.gms.maps.GoogleMap, latLng: LatLng) {
        val currentUser = viewModel.currentUser.value
        val url = currentUser?.profilePictureUrl ?: "https://ui-avatars.com/api/?name=User"

        Glide.with(this).asBitmap().load(url).circleCrop().into(object : CustomTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                try {
                    val customMarker = createCustomMarker(resource)
                    googleMap.addMarker(MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.fromBitmap(customMarker)).anchor(0.5f, 1.0f))
                } catch (e: Exception) {}
            }
            override fun onLoadCleared(placeholder: Drawable?) {}
        })
    }

    private fun createCustomMarker(avatarBitmap: Bitmap): Bitmap {
        val context = requireContext()
        val pinDrawable = ContextCompat.getDrawable(context, R.drawable.ic_map_pin_frame) ?: return avatarBitmap
        val w = pinDrawable.intrinsicWidth; val h = pinDrawable.intrinsicHeight
        val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bm)
        pinDrawable.setBounds(0, 0, w, h); pinDrawable.draw(cv)
        val pad = (w * 0.12f).toInt(); val size = w - pad * 2
        val avt = Bitmap.createScaledBitmap(avatarBitmap, size, size, false)
        cv.drawBitmap(avt, (w - size) / 2f, (w - size) / 2f, null)
        return bm
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

    private fun setupObservers() {
        viewModel.postState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is PostState.Loading -> { binding.btnSubmitPost.isEnabled = false; binding.btnSubmitPost.text = getString(R.string.status_posting) }
                is PostState.Success -> {
                    // Post thành công -> Đánh dấu xong để onPause không lưu lại -> Xóa draft
                    isPostSubmittedOrDiscarded = true
                    // ViewModel đã tự gọi clearDraft trong hàm createPost khi success rồi
                    Toast.makeText(context, getString(R.string.msg_post_success), Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                is PostState.Error -> { binding.btnSubmitPost.isEnabled = true; binding.btnSubmitPost.text = getString(R.string.action_post); Toast.makeText(context, state.msg, Toast.LENGTH_SHORT).show() }
            }
        }
        viewModel.selectedImages.observe(viewLifecycleOwner) { images ->
            if (images.isEmpty()) { binding.rvSelectedImages.visibility = View.GONE; setBottomSheetState(true) }
            else { binding.rvSelectedImages.visibility = View.VISIBLE; imageAdapter.submitList(images.toList()); setBottomSheetState(false) }
            validatePostButton()
        }
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.tvAuthorName.text = it.fullName
                binding.tvAuthorEmail.text = it.email
                if (!it.profilePictureUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(it.profilePictureUrl)
                        .circleCrop()
                        .placeholder(R.drawable.default_avatar)
                        .error(R.drawable.default_avatar)
                        .into(binding.ivAuthorAvatar)
                } else {
                    binding.ivAuthorAvatar.setImageResource(R.drawable.default_avatar)
                }
            }
        }
        viewModel.topicColors.observe(viewLifecycleOwner) { if (selectedTopicsList.isNotEmpty()) updateTopicChips() }
        viewModel.suggestedTopics.observe(viewLifecycleOwner) { /* AI Logic */ }
    }

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
                Toast.makeText(requireContext(), getString(R.string.error_enter_content), Toast.LENGTH_SHORT).show()
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
            tvAiText.text = getString(R.string.ai_topic_suggestion_title)
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
                Toast.makeText(requireContext(), getString(R.string.msg_found_topics), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), getString(R.string.msg_no_suggestions), Toast.LENGTH_SHORT).show()
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

    private fun getColorForTopic(topic: String): Int {
        val hash = abs(topic.hashCode())
        val colors = listOf(0xFFE3F2FD.toInt(), 0xFFE8F5E9.toInt(), 0xFFFFF3E0.toInt(), 0xFFFFEBEE.toInt(), 0xFFF3E5F5.toInt(), 0xFFE0F7FA.toInt(), 0xFFFFF8E1.toInt(), 0xFFF1F8E9.toInt())
        return colors[hash % colors.size]
    }

    private fun showCameraModeSelection() {
        val options = arrayOf(getString(R.string.action_take_photo), getString(R.string.action_record_video))
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_choose_action))
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

    private fun launchPhotoPicker() = pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))

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