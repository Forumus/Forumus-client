package com.hcmus.forumus_client.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.load
import com.hcmus.forumus_client.databinding.FragmentSettingsBinding
import com.hcmus.forumus_client.ui.home.HomeFragmentDirections
import kotlin.getValue

class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    private val viewModel: SettingsViewModel by viewModels() {
        SettingsViewModelFactory(requireActivity().application)
    }
    private val navController by lazy { findNavController() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup UI components and observe view model
        setupHeaderActions()
        setupProfileSection()
        setupToggleSwitches()
        setupMenuActions()
        observeViewModel()

        // Load current user and saved preferences
        viewModel.loadCurrentUser()
        viewModel.loadSavedPreferences()
    }

    /**
     * Setup back button and header interactions
     */
    private fun setupHeaderActions() {
        binding.ibBack.setOnClickListener {
            navController.popBackStack()
        }
    }

    /**
     * Setup user profile header card display
     */
    private fun setupProfileSection() {
        binding.llUserProfile.setOnClickListener {
            // Profile card is read-only, no action needed
        }
    }

    /**
     * Setup preference toggle switches with saved state persistence
     */
    private fun setupToggleSwitches() {
        // Dark mode toggle
        binding.swDarkMode.setOnCheckedChangeListener { _, isChecked ->
            viewModel.saveDarkModePreference(isChecked)
        }

        // Push notifications toggle
        binding.swPushNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewModel.savePushNotificationsPreference(isChecked)
        }

        // Email notifications toggle
        binding.swEmailNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewModel.saveEmailNotificationsPreference(isChecked)
        }
    }

    /**
     * Setup menu action click listeners
     */
    private fun setupMenuActions() {
        // View profile - navigate to profile screen
        binding.llViewProfile.setOnClickListener {
            viewModel.user.value?.let { user ->
                val action = HomeFragmentDirections
                    .actionGlobalProfileFragment(user.uid)
                navController.navigate(action)
            }
        }

        // Edit profile - mock toast
        binding.llEditProfile.setOnClickListener {
            Toast.makeText(requireContext(), "Edit Profile - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        // Saved posts - mock toast
        binding.llSavedPosts.setOnClickListener {
            Toast.makeText(requireContext(), "Saved Posts - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        // Help center - mock toast
        binding.llHelpCenter.setOnClickListener {
            Toast.makeText(requireContext(), "Help Center - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        // Community guidelines - mock toast
        binding.llCommunityGuidelines.setOnClickListener {
            Toast.makeText(requireContext(), "Community Guidelines - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        // About forumus - mock toast
        binding.llAboutForumus.setOnClickListener {
            Toast.makeText(requireContext(), "About Forumus - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        // Logout - mock toast
        binding.llLogout.setOnClickListener {
            Toast.makeText(requireContext(), "Logout - Coming Soon", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Observe ViewModel LiveData for UI updates
     */
    private fun observeViewModel() {
        // Observe current user for profile display
        viewModel.user.observe(viewLifecycleOwner) { user ->
            binding.ivUserAvatar.load(user.profilePictureUrl) {
                crossfade(true)
            }
            binding.tvUserName.text = user.fullName
            binding.tvUserEmail.text = user.email
            binding.tvUserStatus.text = user.role.toString()
        }

        // Observe dark mode preference
        viewModel.isDarkModeEnabled.observe(viewLifecycleOwner) { isDarkMode ->
            binding.swDarkMode.isChecked = isDarkMode
        }

        // Observe push notifications preference
        viewModel.isPushNotificationsEnabled.observe(viewLifecycleOwner) { isEnabled ->
            binding.swPushNotifications.isChecked = isEnabled
        }

        // Observe email notifications preference
        viewModel.isEmailNotificationsEnabled.observe(viewLifecycleOwner) { isEnabled ->
            binding.swEmailNotifications.isChecked = isEnabled
        }
    }
}