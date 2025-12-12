package com.hcmus.forumus_client.ui.main

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.databinding.ActivityMainBinding
import com.hcmus.forumus_client.ui.common.ProfileMenuAction
import com.hcmus.forumus_client.ui.navigation.AppNavigator
import com.hcmus.forumus_client.NavGraphDirections
import com.hcmus.forumus_client.ui.common.BottomNavigationBar

/**
 * Main Activity that serves as the container for all Fragments.
 * Manages:
 * - Navigation between Home, Profile, and Post Detail fragments using Navigation Component
 * - TopAppBar with profile menu, search, and home navigation
 * - BottomNavigationBar for switching between Home and Profile (hidden during Post Detail)
 * - Shared ViewModel for current user data across fragments
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val mainSharedViewModel: MainSharedViewModel by viewModels()
    private val navigator by lazy { AppNavigator(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle system window insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupWindowInsetsHandling()
        setupTopAppBar()
        setupBottomNavigation()
        setupNavigation()
        loadInitialData()
    }

    /**
     * Handle system window insets by applying padding to avoid overlap with status bar,
     * navigation bar and keyboard (IME).
     */
    private fun setupWindowInsetsHandling() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /**
     * Setup top app bar callbacks for menu, search, and profile actions.
     * Observes MainSharedViewModel for current user data.
     */
    private fun setupTopAppBar() {
        binding.topAppBar.apply {
            onMenuClick = {
                Toast.makeText(this@MainActivity, "Menu clicked", Toast.LENGTH_SHORT).show()
            }
            onHomeClick = {
                navController.navigate(R.id.homeFragment)
            }
            onSearchClick = {
                navigator.openSearch()
            }
            onProfileMenuAction = onProfileMenuAction@{ action ->
                when (action) {
                    ProfileMenuAction.VIEW_PROFILE -> {
                        val currentUser =
                            mainSharedViewModel.currentUser.value ?: return@onProfileMenuAction

                        val navAction = NavGraphDirections
                            .actionGlobalProfileFragment(currentUser.uid)

                        navController.navigate(navAction)
                    }
                    ProfileMenuAction.EDIT_PROFILE -> {
                        // TODO: Implement edit profile navigation
                    }
                    ProfileMenuAction.TOGGLE_DARK_MODE -> {
                        // TODO: Implement theme toggle
                    }
                    ProfileMenuAction.SETTINGS -> {
                        val navAction = NavGraphDirections
                            .actionGlobalSettingsFragment()

                        navController.navigate(navAction)
                    }
                }
            }
        }

        // Observe current user and update TopAppBar avatar
        mainSharedViewModel.currentUser.observe(this) { user ->
            if (user != null) {
                binding.topAppBar.setProfileImage(user.profilePictureUrl)
            }
        }
    }

    /**
     * Setup bottom navigation bar for fragment switching.
     */
    private fun setupBottomNavigation() {
        binding.bottomBar.apply {
            setActiveTab(BottomNavigationBar.Tab.HOME)
            onHomeClick = { navController.navigate(R.id.homeFragment) }
            onExploreClick = { Toast.makeText(this@MainActivity, "Explore", Toast.LENGTH_SHORT).show() }
            onCreatePostClick = { navigator.openCreatePost() }
            onAlertsClick = { navigator.openAlerts() }
            onChatClick = { navController.navigate(R.id.chatsFragment) }
        }
    }

    /**
     * Initialize the Navigation Component with NavHostFragment and set up destination changed listener.
     * Hides BottomNavigationBar when navigating to PostDetailFragment.
     */
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as? NavHostFragment

        navController = navHostFragment?.navController
            ?: throw IllegalStateException("NavHostFragment not found")

        // Listen to destination changes to control BottomNavigationBar visibility
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomBar.visibility = android.view.View.VISIBLE
            binding.topAppBar.visibility = android.view.View.VISIBLE

            when (destination.id) {
                R.id.postDetailFragment -> {
                    // Hide BottomNavigationBar for Post Detail view
                    binding.bottomBar.visibility = android.view.View.GONE
                }
                R.id.chatsFragment -> {
                    binding.topAppBar.visibility = android.view.View.GONE
                }
                R.id.conversationFragment, R.id.settingsFragment ->{
                    binding.topAppBar.visibility = android.view.View.GONE
                    binding.bottomBar.visibility = android.view.View.GONE
                }
            }
        }
    }

    /**
     * Load initial data on app startup.
     */
    private fun loadInitialData() {
        mainSharedViewModel.loadCurrentUser()
    }
}
