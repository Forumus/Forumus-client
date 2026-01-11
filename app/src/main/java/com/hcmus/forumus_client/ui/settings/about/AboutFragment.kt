package com.hcmus.forumus_client.ui.settings.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.hcmus.forumus_client.databinding.FragmentAboutBinding

/**
 * Fragment displaying information about the Forumus application.
 * 
 * This screen provides:
 * - App description and purpose
 * - Key features of the platform
 * - Development team information
 * - Legal and copyright information
 * 
 * The screen is purely informational and does not require complex logic.
 */
class AboutFragment : Fragment() {

    private lateinit var binding: FragmentAboutBinding
    private val navController by lazy { findNavController() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupBackButton()
    }

    /**
     * Setup back button to return to previous screen
     */
    private fun setupBackButton() {
        binding.ibBack.setOnClickListener {
            navController.popBackStack()
        }
    }
}
