package com.hcmus.forumus_client.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.load
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.databinding.FragmentSettingsBinding
import com.hcmus.forumus_client.ui.auth.login.LoginActivity
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
        // Use localized strings for display
        val languages = listOf(getString(R.string.english), getString(R.string.vietnamese))
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        binding.spinnerLanguage.adapter = adapter
        
        // set selection based on current viewmodel value to avoid infinite loop or wrong selection on init
        viewModel.language.value?.let { currentLang ->
            // Map saved "English"/"Vietnamese" or code to display string
            // This logic might need to be robust if viewModel stores codes vs display names
             val index = if (currentLang.equals("Vietnamese", ignoreCase = true) || currentLang == "vi") {
                 1
             } else {
                 0
             }
             binding.spinnerLanguage.setSelection(index, false)
        }

        binding.spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Determine language code
                val selectedLangCode = if (position == 1) "vi" else "en"
                val selectedLangName = if (position == 1) "Vietnamese" else "English"
                
                // Only proceed if the selected language is different from current
                // We compare against the LiveData value
                 val currentStoredLang = viewModel.language.value
                 if (currentStoredLang != selectedLangName) {
                     // 1. Save to ViewModel/Preferences
                     viewModel.saveLanguagePreference(selectedLangName)
                     
                     // 2. Apply Locale using AppCompatDelegate
                     val appLocale = LocaleListCompat.forLanguageTags(selectedLangCode)
                     AppCompatDelegate.setApplicationLocales(appLocale)
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

        // About forumus
        binding.llAboutForumus.setOnClickListener {
            navController.navigate(R.id.action_settingsFragment_to_aboutFragment)
        }

        // Logout - show confirmation dialog
        binding.llLogout.setOnClickListener {
            showLogoutConfirmationDialog()
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
           // Update spinner selection if needed
           val isVietnamese = language.equals("Vietnamese", ignoreCase = true) || language == "vi"
           val index = if (isVietnamese) 1 else 0
           
           if (binding.spinnerLanguage.selectedItemPosition != index) {
               binding.spinnerLanguage.setSelection(index)
           }
        }
        // Observe logout completion to navigate to login screen
        viewModel.logoutCompleted.observe(viewLifecycleOwner) { isLoggedOut ->
            if (isLoggedOut) {
                navigateToLogin()
            }
        }
    }

    /**
     * Show confirmation dialog before logging out
     */
    private fun showLogoutConfirmationDialog() {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.logout_title)
            .setMessage(R.string.logout_confirmation_message)
            .setPositiveButton(R.string.logout_confirm) { _, _ ->
                viewModel.logout()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
        
        // Set button text colors for visibility
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            ?.setTextColor(ContextCompat.getColor(requireContext(), R.color.logout_text))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            ?.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
    }

    /**
     * Navigate to login screen and clear back stack
     */
    private fun navigateToLogin() {
        // Clear session first - Firestore.terminate() cancels all active listeners
        // before Firebase signout to prevent PERMISSION_DENIED errors
        viewModel.clearSession()
        
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}