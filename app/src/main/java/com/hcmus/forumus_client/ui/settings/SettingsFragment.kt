package com.hcmus.forumus_client.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.load
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.databinding.FragmentSettingsBinding
import com.hcmus.forumus_client.ui.common.ProfileMenuAction
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
        setupLanguageSpinner()
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
        // Dark mode toggle with theme application
        binding.swDarkMode.setOnCheckedChangeListener { _, isChecked ->
            viewModel.saveDarkModePreference(isChecked)
            
            // Apply theme change immediately - AppCompatDelegate handles it automatically
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            
            // Update system status bar and navigation bar colors
            (activity as? com.hcmus.forumus_client.ui.main.MainActivity)?.updateStatusBarAppearance()
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
     * Setup language selection spinner
     */
    private fun setupLanguageSpinner() {
        val languages = listOf("English", "Vietnamese")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        binding.spinnerLanguage.adapter = adapter
        
        binding.spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedLanguage = languages[position]
                if (selectedLanguage != viewModel.language.value) {
                    viewModel.saveLanguagePreference(selectedLanguage)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
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
            navController.navigate(R.id.editProfileFragment)
        }

        // Saved posts - mock toast
        binding.llSavedPosts.setOnClickListener {
            navController.navigate(R.id.action_settingsFragment_to_savedPostsFragment)
        }

        // Help center - navigate to help center screen
        binding.llHelpCenter.setOnClickListener {
            navController.navigate(R.id.action_settingsFragment_to_helpCenterFragment)
        }

        // Community guidelines - navigate to guidelines screen
        binding.llCommunityGuidelines.setOnClickListener {
            navController.navigate(R.id.action_settingsFragment_to_communityGuidelinesFragment)
        }

        // About forumus - mock toast
        binding.llAboutForumus.setOnClickListener {
            navController.navigate(R.id.action_settingsFragment_to_aboutFragment)
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
                placeholder(R.drawable.default_avatar)
                error(R.drawable.default_avatar)
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

        // Observe language preference
        viewModel.language.observe(viewLifecycleOwner) { language ->
            val languages = listOf("English", "Vietnamese")
            val index = languages.indexOf(language)
            if (index >= 0 && binding.spinnerLanguage.selectedItemPosition != index) {
                binding.spinnerLanguage.setSelection(index)
            }
        }
    }
}