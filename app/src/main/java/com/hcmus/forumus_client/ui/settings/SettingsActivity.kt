package com.hcmus.forumus_client.ui.settings

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.hcmus.forumus_client.databinding.ActivitySettingsBinding
import com.hcmus.forumus_client.ui.navigation.AppNavigator

/**
 * Activity for managing application settings and user preferences.
 *
 * Responsibilities:
 * - Display user profile information in header section
 * - Handle preference toggles (dark mode, push notifications, email notifications)
 * - Persist user settings using PreferencesManager
 * - Navigate to profile screen when view profile is clicked
 * - Show mock toast messages for other menu items
 * - Load and restore saved preferences on activity creation
 *
 * The activity uses SettingsViewModel to manage state and preferences,
 * with all preference changes automatically persisted to SharedPreferences.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels() {
        SettingsViewModelFactory(application)
    }
    
    private val navigator by lazy { AppNavigator(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            finish()
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
                navigator.openProfile(user.uid)
            }
        }

        // Edit profile - mock toast
        binding.llEditProfile.setOnClickListener {
            Toast.makeText(this, "Edit Profile - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        // Saved posts - mock toast
        binding.llSavedPosts.setOnClickListener {
            Toast.makeText(this, "Saved Posts - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        // Help center - mock toast
        binding.llHelpCenter.setOnClickListener {
            Toast.makeText(this, "Help Center - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        // Community guidelines - mock toast
        binding.llCommunityGuidelines.setOnClickListener {
            Toast.makeText(this, "Community Guidelines - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        // About forumus - mock toast
        binding.llAboutForumus.setOnClickListener {
            Toast.makeText(this, "About Forumus - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        // Logout - mock toast
        binding.llLogout.setOnClickListener {
            Toast.makeText(this, "Logout - Coming Soon", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Observe ViewModel LiveData for UI updates
     */
    private fun observeViewModel() {
        // Observe current user for profile display
        viewModel.user.observe(this) { user ->
            binding.ivUserAvatar.load(user.profilePictureUrl) {
                crossfade(true)
            }
            binding.tvUserName.text = user.fullName
            binding.tvUserEmail.text = user.email
            binding.tvUserStatus.text = user.role.toString()
        }

        // Observe dark mode preference
        viewModel.isDarkModeEnabled.observe(this) { isDarkMode ->
            binding.swDarkMode.isChecked = isDarkMode
        }

        // Observe push notifications preference
        viewModel.isPushNotificationsEnabled.observe(this) { isEnabled ->
            binding.swPushNotifications.isChecked = isEnabled
        }

        // Observe email notifications preference
        viewModel.isEmailNotificationsEnabled.observe(this) { isEnabled ->
            binding.swEmailNotifications.isChecked = isEnabled
        }
    }
}
