package com.hcmus.forumus_client.ui.post.create

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
// --- IMPORT GOOGLE MAPS ---
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
// --- IMPORT GOOGLE PLACES (QUAN TRỌNG) ---
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hcmus.forumus_client.R

class LocationPickerBottomSheet(
    private val userAvatarUrl: String?,
    private val onLocationSelected: (Place) -> Unit,
    private val onSearchClick: () -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var placesClient: PlacesClient
    private lateinit var rvNearbyPlaces: RecyclerView
    private lateinit var btnAddLocation: Button
    private lateinit var tvPreviewMap: TextView
    private var currentSelectedPlace: Place? = null

    // Launcher xin quyền vị trí
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocation || coarseLocation) {
            fetchNearbyPlaces()
        } else {
            Toast.makeText(context, "Cần quyền vị trí để gợi ý địa điểm gần bạn", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_bottom_sheet_location_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Khởi tạo Places Client
        if (!Places.isInitialized()) {
            // Thay YOUR_API_KEY bằng key của bạn, hoặc để CreatePostFragment lo việc init
            Places.initialize(requireContext(), "YOUR_API_KEY_HERE")
        }
        placesClient = Places.createClient(requireContext())

        // 2. Setup View
        rvNearbyPlaces = view.findViewById(R.id.rvNearbyPlaces)
        rvNearbyPlaces.layoutManager = LinearLayoutManager(context)
        btnAddLocation = view.findViewById(R.id.btnAddLocation)
        tvPreviewMap = view.findViewById(R.id.tvPreviewMap)
        val btnSearch = view.findViewById<LinearLayout>(R.id.btnSearchPlace)

        // 3. Logic: Kiểm tra quyền và lấy địa điểm
        checkPermissionsAndFetch()

        // 4. Sự kiện Click
        btnSearch.setOnClickListener {
            dismiss()
            onSearchClick()
        }

        btnAddLocation.setOnClickListener {
            currentSelectedPlace?.let {
                onLocationSelected(it)
                dismiss()
            }
        }

        tvPreviewMap.setOnClickListener {
            currentSelectedPlace?.let { place ->
                showMapPreviewDialog(place)
            }
        }
    }

    private fun checkPermissionsAndFetch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            fetchNearbyPlaces()
        } else {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    private fun fetchNearbyPlaces() {
        // Chỉ định các trường dữ liệu cần lấy
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        val request = FindCurrentPlaceRequest.newInstance(placeFields)

        // Kiểm tra quyền lại một lần nữa (bắt buộc bởi IDE)
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            placesClient.findCurrentPlace(request).addOnSuccessListener { response ->
                // Lấy danh sách Place từ response
                val places = response.placeLikelihoods.map { it.place }

                // Đổ dữ liệu vào Adapter
                rvNearbyPlaces.adapter = NearbyPlacesAdapter(places) { selectedPlace ->
                    currentSelectedPlace = selectedPlace

                    // Khi chọn xong mới sáng nút lên
                    btnAddLocation.isEnabled = true

                    tvPreviewMap.isEnabled = true
                    tvPreviewMap.alpha = 1.0f
                }
            }.addOnFailureListener { exception ->
                // Có thể lỗi do chưa bật GPS hoặc API Key chưa enable Billing
                Toast.makeText(context, "Không thể lấy địa điểm gần đây.", Toast.LENGTH_SHORT).show()
                exception.printStackTrace()
            }
        }
    }

    // --- HIỂN THỊ MAP PREVIEW ---
    private fun showMapPreviewDialog(place: Place) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.layout_dialog_map_preview)

        // Làm nền dialog trong suốt để bo góc đẹp hơn
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val mapView = dialog.findViewById<MapView>(R.id.mapView)
        val tvTitle = dialog.findViewById<TextView>(R.id.tvSelectedPlaceName)
        val btnClose = dialog.findViewById<Button>(R.id.btnCloseMap)

        tvTitle.text = place.name

        // Khởi tạo MapView trong Dialog
        MapsInitializer.initialize(requireContext())
        mapView.onCreate(dialog.onSaveInstanceState())
        mapView.onResume()

        mapView.getMapAsync { googleMap ->
            val latLng = place.latLng ?: LatLng(10.762622, 106.660172) // Mặc định HCM nếu null
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))

            // Vẽ Marker Avatar
            loadAvatarMarker(googleMap, latLng)
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // --- LOGIC VẼ MARKER AVATAR ---
    private fun loadAvatarMarker(googleMap: com.google.android.gms.maps.GoogleMap, latLng: LatLng) {
        val url = userAvatarUrl ?: "https://ui-avatars.com/api/?name=User"

        Glide.with(this)
            .asBitmap()
            .load(url)
            .circleCrop()
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    try {
                        val customMarker = createCustomMarker(resource)
                        googleMap.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .icon(BitmapDescriptorFactory.fromBitmap(customMarker))
                                .anchor(0.5f, 1.0f) // Mũi nhọn ở giữa đáy
                        )
                    } catch (e: Exception) { e.printStackTrace() }
                }
                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun createCustomMarker(avatarBitmap: Bitmap): Bitmap {
        val context = requireContext()
        val pinDrawable = ContextCompat.getDrawable(context, R.drawable.ic_map_pin_frame) ?: return avatarBitmap

        val pinWidth = pinDrawable.intrinsicWidth
        val pinHeight = pinDrawable.intrinsicHeight
        val combinedBitmap = Bitmap.createBitmap(pinWidth, pinHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(combinedBitmap)

        pinDrawable.setBounds(0, 0, pinWidth, pinHeight)
        pinDrawable.draw(canvas)

        val padding = (pinWidth * 0.12f).toInt()
        val avatarSize = pinWidth - (padding * 2)
        val scaledAvatar = Bitmap.createScaledBitmap(avatarBitmap, avatarSize, avatarSize, false)

        val leftOffset = (pinWidth - avatarSize) / 2f
        val topOffset = (pinWidth - avatarSize) / 2f

        canvas.drawBitmap(scaledAvatar, leftOffset, topOffset, null)
        return combinedBitmap
    }
}