package com.hcmus.forumus_client.ui.common

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import com.hcmus.forumus_client.R

/**
 * A popup menu for profile-related actions.
 * Provides click callbacks for view profile, edit profile, dark mode toggle, and settings.
 */
class PopupProfileMenu (
    private val activity: AppCompatActivity
) {
    // Callback for when user taps "View Profile" option
    var onViewProfileClick: (() -> Unit)? = null
    // Callback for when user taps "Edit Profile" option
    var onEditProfileClick: (() -> Unit)? = null
    // Callback for when user taps "Dark Mode" toggle option
    var onDarkModeClick: (() -> Unit)? = null
    // Callback for when user taps "Settings" option
    var onSettingsClick: (() -> Unit)? = null

    /**
     * Displays the popup menu at the specified anchor view position.
     *
     * @param anchor The view to anchor the popup menu to (typically the profile icon)
     */
    fun show(anchor: View) {
        // Inflate the popup menu layout from XML
        val inflater = LayoutInflater.from(activity)
        val popupView = inflater.inflate(R.layout.popup_profile_menu, null)

        // Create the popup window with wrap content dimensions and focusable enabled
        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            // Apply popup styling: background drawable and elevation shadow
            setBackgroundDrawable(activity.getDrawable(R.drawable.popup_menu_background))
            elevation = 8f
        }

        // Setup click listeners for all menu options
        // Each listener invokes the corresponding callback and closes the popup
        popupView.findViewById<LinearLayout>(R.id.btnViewProfile).setOnClickListener {
            onViewProfileClick?.invoke()
            popupWindow.dismiss()
        }

        popupView.findViewById<LinearLayout>(R.id.btnEditProfile).setOnClickListener {
            onEditProfileClick?.invoke()
            popupWindow.dismiss()
        }

        popupView.findViewById<LinearLayout>(R.id.btnDarkMode).setOnClickListener {
            onDarkModeClick?.invoke()
            popupWindow.dismiss()
        }

        popupView.findViewById<LinearLayout>(R.id.btnSettings).setOnClickListener {
            onSettingsClick?.invoke()
            popupWindow.dismiss()
        }

        // Display popup as dropdown below the anchor view with 8dp vertical offset
        popupWindow.showAsDropDown(anchor, 0, 8)
    }
}