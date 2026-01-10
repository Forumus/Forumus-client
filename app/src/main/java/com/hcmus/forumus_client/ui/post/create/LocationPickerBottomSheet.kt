package com.hcmus.forumus_client.ui.post.create

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
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
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
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
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hcmus.forumus_client.R

class LocationPickerBottomSheet(
    private val userAvatarUrl: String?, // Truyền avatar vào để làm marker
    private val onLocationSelected: (Place) -> Unit,
    private val onSearchClick: () -> Unit // Callback để mở Autocomplete Activity
) : BottomSheetDialogFragment() {

    private lateinit var placesClient: PlacesClient
    private lateinit var rvNearbyPlaces: RecyclerView
    private lateinit var btnAddLocation: Button
    private lateinit var tvPreviewMap: TextView
    private var currentSelectedPlace: Place? = null

    // Permission Launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            fetchNearbyPlaces()
        } else {
            Toast.makeText(context, "Location permission needed to find nearby places", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_bottom_sheet_location_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        placesClient = Places.createClient(requireContext())

        rvNearbyPlaces = view.findViewById(R.id.rvNearbyPlaces)
        rvNearbyPlaces.layoutManager = LinearLayoutManager(context)

        btnAddLocation = view.findViewById(R.id.btnAddLocation)
        tvPreviewMap = view.findViewById(R.id.tvPreviewMap)
        val btnSearch = view.findViewById<LinearLayout>(R.id.btnSearchPlace)

        // 1. Kiểm tra quyền và lấy địa điểm
        checkPermissionsAndFetch()

        // 2. Xử lý Search
        btnSearch.setOnClickListener {
            dismiss() // Đóng bottom sheet
            onSearchClick() // Mở Autocomplete của Google
        }

        // 3. Xử lý nút Add
        btnAddLocation.setOnClickListener {
            currentSelectedPlace?.let {
                onLocationSelected(it)
                dismiss()
            }
        }

        // 4. Xử lý nút Preview Map
        tvPreviewMap.setOnClickListener {
            currentSelectedPlace?.let { place ->
                showMapPreviewDialog(place)
            }
        }
    }

    private fun checkPermissionsAndFetch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchNearbyPlaces()
        } else {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    private fun fetchNearbyPlaces() {
        // Lấy các trường thông tin cần thiết
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        val request = FindCurrentPlaceRequest.newInstance(placeFields)

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            placesClient.findCurrentPlace(request).addOnSuccessListener { response ->
                // Convert response to list of Places
                val places = response.placeLikelihoods.map { it.place }

                rvNearbyPlaces.adapter = NearbyPlacesAdapter(places) { selectedPlace ->
                    currentSelectedPlace = selectedPlace
                    // Enable buttons
                    btnAddLocation.isEnabled = true
                    tvPreviewMap.isEnabled = true
                    tvPreviewMap.alpha = 1.0f
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(context, "Error finding places: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showMapPreviewDialog(place: Place) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.layout_dialog_map_preview)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val mapView = dialog.findViewById<MapView>(R.id.mapView)
        val tvTitle = dialog.findViewById<TextView>(R.id.tvSelectedPlaceName)
        val btnClose = dialog.findViewById<Button>(R.id.btnCloseMap)

        tvTitle.text = place.name

        // Init Map
        MapsInitializer.initialize(requireContext())
        mapView.onCreate(dialog.onSaveInstanceState())
        mapView.onResume()

        mapView.getMapAsync { googleMap ->
            val latLng = place.latLng ?: LatLng(0.0, 0.0)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

            // Custom Marker từ Avatar
            loadAvatarMarker(googleMap, latLng)
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun loadAvatarMarker(googleMap: com.google.android.gms.maps.GoogleMap, latLng: LatLng) {
        val url = userAvatarUrl ?: "https://ui-avatars.com/api/?name=User"

        Glide.with(this)
            .asBitmap()
            .load(url)
            .circleCrop() // Bo tròn avatar
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    try {
                        // Resize bitmap nhỏ lại chút cho vừa map
                        val scaledBitmap = Bitmap.createScaledBitmap(resource, 120, 120, false)
                        googleMap.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .icon(BitmapDescriptorFactory.fromBitmap(scaledBitmap))
                        )
                    } catch (e: Exception) { e.printStackTrace() }
                }
                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }
}