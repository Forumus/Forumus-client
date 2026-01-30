package com.hcmus.forumus_client.ui.common

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import com.hcmus.forumus_client.R

class PopupProfileMenu (
    private val activity: AppCompatActivity
) {
    var onViewProfileClick: (() -> Unit)? = null
    var onEditProfileClick: (() -> Unit)? = null
    var onDarkModeClick: (() -> Unit)? = null
    var onSettingsClick: (() -> Unit)? = null

    fun show(anchor: View) {
        val inflater = LayoutInflater.from(activity)
        val popupView = inflater.inflate(R.layout.popup_profile_menu, null)

        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(activity.getDrawable(R.drawable.popup_menu_background))
            elevation = 8f
        }

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

        popupWindow.showAsDropDown(anchor, 0, 8)
    }
}