package com.hcmus.forumus_client.ui.fragments

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.databinding.FragmentNavbarBinding

class NavbarFragment : Fragment() {

    interface OnNavigationItemSelectedListener {
        fun onNavigationItemSelected(item: String)
    }

    private var _binding: FragmentNavbarBinding? = null
    private val binding get() = _binding!!
    
    private var currentActiveTab: View? = null
    private var navigationListener: OnNavigationItemSelectedListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNavbarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        navigationListener = activity as? OnNavigationItemSelectedListener
        setupClickListeners()
        // Set home as default active tab
        setActiveTab(binding.navHome)
    }

    private fun setupClickListeners() {
        binding.navHome.setOnClickListener { 
            setActiveTab(binding.navHome)
            navigationListener?.onNavigationItemSelected("home")
        }
        binding.navExplore.setOnClickListener { 
            setActiveTab(binding.navExplore)
            navigationListener?.onNavigationItemSelected("explore")
        }
        binding.navAlerts.setOnClickListener { 
            setActiveTab(binding.navAlerts)
            navigationListener?.onNavigationItemSelected("alerts")
        }
        binding.navChat.setOnClickListener { 
            setActiveTab(binding.navChat)
            navigationListener?.onNavigationItemSelected("chat")
        }
        binding.btnCreatePost.setOnClickListener {
            // TODO: Handle create post action
        }
    }

    private fun setActiveTab(selectedTab: View) {
        // Reset all tabs to inactive
        listOf(binding.navHome, binding.navExplore, binding.navAlerts, binding.navChat)
            .forEach { applyInactiveStyle(it) }
        
        // Set selected tab to active
        applyActiveStyle(selectedTab)
        currentActiveTab = selectedTab
    }

    private fun applyInactiveStyle(container: View) {
        container.setBackgroundResource(0)
        if (container is ViewGroup && container.childCount >= 2) {
            val icon = container.getChildAt(0) as? ImageView
            val label = container.getChildAt(1) as? TextView
            // Clear any special backgrounds from home icon
            icon?.background = null
            val inactiveColor = ContextCompat.getColor(requireContext(), R.color.text_tertiary)
            when (container.id) {
                R.id.nav_home -> icon?.setImageResource(R.drawable.ic_home)
                R.id.nav_explore -> icon?.setImageResource(R.drawable.ic_explore)
                R.id.nav_alerts -> icon?.setImageResource(R.drawable.ic_notification)
                R.id.nav_chat -> icon?.setImageResource(R.drawable.ic_chat)
            }
            icon?.imageTintList = ColorStateList.valueOf(inactiveColor)
            label?.setTextColor(inactiveColor)
        }
    }

    private fun applyActiveStyle(container: View) {
        val blue = ContextCompat.getColor(requireContext(), R.color.link_color)
        // All tabs (including Home): slightly darken container; icon and label turn blue
        container.setBackgroundResource(R.drawable.nav_item_active_dim)
        if (container is ViewGroup && container.childCount >= 2) {
            val icon = container.getChildAt(0) as? ImageView
            val label = container.getChildAt(1) as? TextView
            icon?.background = null
            when (container.id) {
                R.id.nav_home -> {
                    icon?.setImageResource(R.drawable.ic_home_filled)
                    icon?.imageTintList = null
                }
                R.id.nav_explore -> {
                    icon?.setImageResource(R.drawable.ic_explore_filled)
                    icon?.imageTintList = null
                }
                R.id.nav_alerts -> {
                    icon?.setImageResource(R.drawable.ic_notification_filled)
                    icon?.imageTintList = null
                }
                R.id.nav_chat -> {
                    icon?.setImageResource(R.drawable.ic_chat_filled)
                    icon?.imageTintList = null
                }
            }
            // Other non-icon changes
            label?.setTextColor(blue)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}