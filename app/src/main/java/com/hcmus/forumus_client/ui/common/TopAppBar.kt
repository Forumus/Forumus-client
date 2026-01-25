package com.hcmus.forumus_client.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.view.ViewGroup
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.databinding.LayoutTopAppBarBinding
import com.hcmus.forumus_client.databinding.PopupProfileMenuBinding
import android.graphics.Color
import coil.load
import androidx.core.graphics.drawable.toDrawable
import android.view.View

/**
 * A custom top app bar component with menu, logo, search, and profile functionalities.
 * Provides callbacks for user interactions with various UI elements.
 */
class TopAppBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    // View binding for the top app bar layout
    private val binding: LayoutTopAppBarBinding =
        LayoutTopAppBarBinding.inflate(LayoutInflater.from(context), this, true)

    // Unified callback for all profile menu actions
    var onProfileMenuAction: ((ProfileMenuAction) -> Unit)? = null
    var onFuncClick: (() -> Unit)? = null         // Triggered when hamburger menu is tapped
    var onHomeClick: (() -> Unit)? = null         // Triggered when logo is tapped

    init {
        // Set horizontal layout orientation for menu, logo, search, and profile elements
        orientation = HORIZONTAL

        with(binding) {
            // Menu button triggers navigation drawer or menu
            funcButton.setOnClickListener { onFuncClick?.invoke() }
            // Logo icon and text both navigate to home screen
            logoIcon.setOnClickListener { onHomeClick?.invoke() }

            logoText.setOnClickListener { onHomeClick?.invoke() }
            // Profile image shows the profile popup menu
            profileImage.setOnClickListener {
                showProfilePopup()
            }
        }
    }

    /**
     * Loads and displays a profile image from the given URL.
     * Uses crossfade transition and default avatar as placeholder/error fallback.
     *
     * @param url The URL of the profile image to load
     */
    fun setProfileImage(url: String?) {
        binding.profileImage.load(url) {
            crossfade(true)                              // Smooth transition when image loads
            placeholder(R.drawable.default_avatar)       // Show while loading
            error(R.drawable.default_avatar)             // Show if loading fails
        }
    }

    fun setIconFuncButton(icon: Int) {
        binding.funcButton.setImageResource(icon)
    }

    /**
     * Creates and displays a dropdown menu for profile-related actions.
     * The menu appears below the profile image with offset positioning.
     */
    private fun showProfilePopup() {
        // Inflate the popup menu layout using view binding
        val popupBinding = PopupProfileMenuBinding.inflate(LayoutInflater.from(context))

        // Check current theme mode
        val preferencesManager = com.hcmus.forumus_client.data.local.PreferencesManager(context)
        val isDarkMode = preferencesManager.isDarkModeEnabled
        
        // Update dark mode button text and icon based on current theme
        // The btnDarkMode is a LinearLayout containing an ImageView and TextView
        val darkModeContainer = popupBinding.btnDarkMode
        val darkModeIcon = darkModeContainer.getChildAt(0) as android.widget.ImageView
        val darkModeTextView = darkModeContainer.getChildAt(1) as android.widget.TextView
        
        if (isDarkMode) {
            // Currently in dark mode, show "Light Mode" to switch to light
            darkModeIcon.setImageResource(R.drawable.ic_sun)
            darkModeTextView.text = context.getString(R.string.light_mode)
        } else {
            // Currently in light mode, show "Dark Mode" to switch to dark
            darkModeIcon.setImageResource(R.drawable.ic_theme)
            darkModeTextView.text = context.getString(R.string.dark_mode)
        }

        // Create popup window with wrap content dimensions and focus enabled
        val popupWindow = PopupWindow(
            popupBinding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true                           // Dismiss when tapping outside
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable()) // Transparent background
            elevation = 8f                                      // Add shadow effect
        }

        // Attach click listeners to all four menu options
        // Each triggers the appropriate action callback and closes the popup
        popupBinding.btnViewProfile.setOnClickListener {
            onProfileMenuAction?.invoke(ProfileMenuAction.VIEW_PROFILE)
            popupWindow.dismiss()
        }

        popupBinding.btnEditProfile.setOnClickListener {
            onProfileMenuAction?.invoke(ProfileMenuAction.EDIT_PROFILE)
            popupWindow.dismiss()
        }

        popupBinding.btnDarkMode.setOnClickListener {
            onProfileMenuAction?.invoke(ProfileMenuAction.TOGGLE_DARK_MODE)
            popupWindow.dismiss()
        }

        popupBinding.btnSettings.setOnClickListener {
            onProfileMenuAction?.invoke(ProfileMenuAction.SETTINGS)
            popupWindow.dismiss()
        }

        // Display popup below the profile image with -20dp horizontal offset (left alignment adjustment)
        popupWindow.showAsDropDown(binding.profileImage, -20, 0)
    }
}
