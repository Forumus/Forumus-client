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

class TopAppBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    private val binding: LayoutTopAppBarBinding =
        LayoutTopAppBarBinding.inflate(LayoutInflater.from(context), this, true)

    var onProfileMenuAction: ((ProfileMenuAction) -> Unit)? = null
    var onFuncClick: (() -> Unit)? = null
    var onHomeClick: (() -> Unit)? = null

    init {
        orientation = HORIZONTAL

        with(binding) {
            funcButton.setOnClickListener { onFuncClick?.invoke() }
            logoIcon.setOnClickListener { onHomeClick?.invoke() }
            logoText.setOnClickListener { onHomeClick?.invoke() }
            profileImage.setOnClickListener { showProfilePopup() }
        }
    }

    fun setProfileImage(url: String?) {
        binding.profileImage.load(url) {
            crossfade(true)
            placeholder(R.drawable.default_avatar)
            error(R.drawable.default_avatar)
        }
    }

    fun setIconFuncButton(icon: Int) {
        binding.funcButton.setImageResource(icon)
    }

    private fun showProfilePopup() {
        val popupBinding = PopupProfileMenuBinding.inflate(LayoutInflater.from(context))

        val preferencesManager = com.hcmus.forumus_client.data.local.PreferencesManager(context)
        val isDarkMode = preferencesManager.isDarkModeEnabled
        
        val darkModeContainer = popupBinding.btnDarkMode
        val darkModeIcon = darkModeContainer.getChildAt(0) as android.widget.ImageView
        val darkModeTextView = darkModeContainer.getChildAt(1) as android.widget.TextView
        
        if (isDarkMode) {
            darkModeIcon.setImageResource(R.drawable.ic_sun)
            darkModeTextView.text = context.getString(R.string.light_mode)
        } else {
            darkModeIcon.setImageResource(R.drawable.ic_theme)
            darkModeTextView.text = context.getString(R.string.dark_mode)
        }

        val popupWindow = PopupWindow(
            popupBinding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            elevation = 8f
        }

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

        popupWindow.showAsDropDown(binding.profileImage, -20, 0)
    }
}
