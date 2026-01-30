package com.hcmus.forumus_client.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.databinding.LayoutBottomNavigationBinding

class BottomNavigationBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: LayoutBottomNavigationBinding =
        LayoutBottomNavigationBinding.inflate(LayoutInflater.from(context), this, true)

    var onHomeClick: (() -> Unit)? = null
    var onExploreClick: (() -> Unit)? = null
    var onCreatePostClick: (() -> Unit)? = null
    var onAlertsClick: (() -> Unit)? = null
    var onChatClick: (() -> Unit)? = null

    init {
        orientation = HORIZONTAL

        binding.navHome.setOnClickListener {
            setActiveTab(Tab.HOME)
            onHomeClick?.invoke() }
        binding.navExplore.setOnClickListener {
            setActiveTab(Tab.EXPLORE)
            onExploreClick?.invoke() }
        binding.btnCreatePost.setOnClickListener {
            setActiveTab(Tab.NONE)
            onCreatePostClick?.invoke() }
        binding.navAlerts.setOnClickListener {
            setActiveTab(Tab.ALERTS)
            onAlertsClick?.invoke() }
        binding.navChat.setOnClickListener {
            setActiveTab(Tab.CHAT)
            onChatClick?.invoke() }
    }

    fun setActiveTab(tab: Tab) {
        resetTabStyles()
        val activeColor = ContextCompat.getColor(context, R.color.link_color)

        when (tab) {
            Tab.HOME -> {
                binding.iconHome.setImageResource(R.drawable.ic_home_filled)
                binding.textHome.setTextColor(activeColor)
                binding.iconHome.setColorFilter(activeColor)
            }

            Tab.EXPLORE -> {
                binding.iconExplore.setImageResource(R.drawable.ic_explore_filled)
                binding.textExplore.setTextColor(activeColor)
                binding.iconExplore.setColorFilter(activeColor)
            }

            Tab.ALERTS -> {
                binding.iconAlerts.setImageResource(R.drawable.ic_notification_filled)
                binding.textAlerts.setTextColor(activeColor)
                binding.iconAlerts.setColorFilter(activeColor)
            }

            Tab.CHAT -> {
                binding.iconChat.setImageResource(R.drawable.ic_chat_filled)
                binding.textChat.setTextColor(activeColor)
                binding.iconChat.setColorFilter(activeColor)
            }

            else -> Unit
        }
    }

    private fun resetTabStyles() {
        val gray = ContextCompat.getColor(context, R.color.text_tertiary)

        binding.iconHome.setImageResource(R.drawable.ic_home)
        binding.textHome.setTextColor(gray)
        binding.iconHome.setColorFilter(gray)

        binding.iconExplore.setImageResource(R.drawable.ic_explore)
        binding.textExplore.setTextColor(gray)
        binding.iconExplore.setColorFilter(gray)

        binding.iconAlerts.setImageResource(R.drawable.ic_notification)
        binding.textAlerts.setTextColor(gray)
        binding.iconAlerts.setColorFilter(gray)

        binding.iconChat.setImageResource(R.drawable.ic_chat)
        binding.textChat.setTextColor(gray)
        binding.iconChat.setColorFilter(gray)
    }

    enum class Tab {
        HOME, EXPLORE, ALERTS, CHAT, NONE
    }

    fun setNotificationBadge(count: Int) {
        android.util.Log.d("BottomNavigationBar", "Setting badge: $count")
        if (count > 0) {
            binding.badgeAlerts.visibility = VISIBLE
            binding.badgeAlerts.text = if (count > 99) "99+" else count.toString()
        } else {
            binding.badgeAlerts.visibility = GONE
        }
    }

    fun setChatBadge(count: Int) {
        android.util.Log.d("BottomNavigationBar", "Setting chat badge: $count")
        if (count > 0) {
            binding.badgeChat.visibility = VISIBLE
            binding.badgeChat.text = if (count > 99) "99+" else count.toString()
        } else {
            binding.badgeChat.visibility = GONE
        }
    }
}
