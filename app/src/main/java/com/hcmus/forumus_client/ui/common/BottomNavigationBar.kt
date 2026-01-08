package com.hcmus.forumus_client.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.databinding.LayoutBottomNavigationBinding

/**
 * A custom bottom navigation bar component for the main app navigation.
 * Provides navigation tabs for Home, Explore, Create Post, Alerts, and Chat.
 * Supports visual feedback for the active tab with color and icon changes.
 */
class BottomNavigationBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    // View binding for the bottom navigation layout
    private val binding: LayoutBottomNavigationBinding =
        LayoutBottomNavigationBinding.inflate(LayoutInflater.from(context), this, true)

    // Callbacks for navigation actions
    var onHomeClick: (() -> Unit)? = null            // Triggered when Home tab is tapped
    var onExploreClick: (() -> Unit)? = null         // Triggered when Explore tab is tapped
    var onCreatePostClick: (() -> Unit)? = null      // Triggered when Create Post button is tapped
    var onAlertsClick: (() -> Unit)? = null          // Triggered when Alerts tab is tapped
    var onChatClick: (() -> Unit)? = null            // Triggered when Chat tab is tapped

    init {
        // Set horizontal layout orientation for navigation tabs
        orientation = HORIZONTAL

        // Attach click listeners to all navigation items
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

    /**
     * Highlights the active tab by applying color and filled icon to the specified tab.
     * All other tabs are reset to their default gray appearance.
     *
     * @param tab The tab to highlight. Use the Tab enum for type-safe selection.
     */
    fun setActiveTab(tab: Tab) {
        // Reset all tabs to default styling first
        resetTabStyles()

        // Get the link color for active state
        val activeColor = ContextCompat.getColor(context, R.color.link_color)

        // Apply active styling to the selected tab with filled icon and colored text
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

    /**
     * Resets all tabs to their default inactive appearance.
     * Sets icons to outline style and text color to tertiary (gray).
     */
    private fun resetTabStyles() {
        // Get the tertiary gray color for inactive tabs
        val gray = ContextCompat.getColor(context, R.color.text_tertiary)

        // Reset Home tab to default outline icon and gray text
        binding.iconHome.setImageResource(R.drawable.ic_home)
        binding.textHome.setTextColor(gray)
        binding.iconHome.setColorFilter(gray)

        // Reset Explore tab to default outline icon and gray text
        binding.iconExplore.setImageResource(R.drawable.ic_explore)
        binding.textExplore.setTextColor(gray)
        binding.iconExplore.setColorFilter(gray)

        // Reset Alerts tab to default outline icon and gray text
        binding.iconAlerts.setImageResource(R.drawable.ic_notification)
        binding.textAlerts.setTextColor(gray)
        binding.iconAlerts.setColorFilter(gray)

        // Reset Chat tab to default outline icon and gray text
        binding.iconChat.setImageResource(R.drawable.ic_chat)
        binding.textChat.setTextColor(gray)
        binding.iconChat.setColorFilter(gray)
    }

    enum class Tab {
        HOME, EXPLORE, ALERTS, CHAT, NONE
    }

    /**
     * Sets the notification badge count.
     * @param count number of unread notifications. 0 or less hides the badge.
     */
    fun setNotificationBadge(count: Int) {
        android.util.Log.d("BottomNavigationBar", "Setting badge: $count")
        if (count > 0) {
            binding.badgeAlerts.visibility = VISIBLE
            binding.badgeAlerts.text = if (count > 99) "99+" else count.toString()
        } else {
            binding.badgeAlerts.visibility = GONE
        }
    }
}
