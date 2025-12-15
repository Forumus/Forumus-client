package com.hcmus.forumus_client.ui.main

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.databinding.ActivityMainBinding
import com.hcmus.forumus_client.ui.navigation.AppNavigator
import com.hcmus.forumus_client.ui.common.BottomNavigationBar
import androidx.core.view.GravityCompat
import coil.load
import com.hcmus.forumus_client.data.model.Topic
import com.hcmus.forumus_client.ui.home.HomeViewModel
import kotlin.text.ifEmpty
import kotlin.text.lowercase
import kotlin.text.startsWith

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

        setupWindowInsetsHandling()
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
     * Initialize the Navigation Component with NavHostFragment and set up destination changed listener.
     * Hides BottomNavigationBar when navigating to PostDetailFragment.
     */
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as? NavHostFragment

        navController = navHostFragment?.navController
            ?: throw IllegalStateException("NavHostFragment not found")
    }

    /**
     * Load initial data on app startup.
     */
    private fun loadInitialData() {
        mainSharedViewModel.loadCurrentUser()
    }
}
