package com.hcmus.forumus_client.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.messaging.FirebaseMessaging
import com.hcmus.forumus_client.NavGraphDirections
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.data.local.PreferencesManager
import com.hcmus.forumus_client.data.model.UserStatus
import com.hcmus.forumus_client.data.repository.UserRepository
import com.hcmus.forumus_client.databinding.ActivityMainBinding
import com.hcmus.forumus_client.ui.auth.banned.BannedActivity
import kotlinx.coroutines.launch

/** Main Activity container for all app fragments. */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val mainSharedViewModel: MainSharedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme before super.onCreate()
        applyTheme()
        applyLocale()
        
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsetsHandling()
        
        // Check if user is banned
        checkBanStatus()
        
        // Update status bar appearance based on theme
        updateStatusBarAppearance()
        
        setupNavigation()
        loadInitialData()
        handleNotificationIntent(intent)

        // Request permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                    Log.d("HomeActivity", "Notification permission already granted")
                    initializeFCM()
                }
                else -> {
                    // Request permission
                    Log.d("HomeActivity", "Requesting notification permission")
                    requestNotificationPermission.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                }
            }
        } else {
            // Android 12 and below - notifications allowed by default
            Log.d("HomeActivity", "Android 12 or below - initializing FCM")
            initializeFCM()
        }
    }

    /** Applies padding to avoid overlap with system bars. */
    private fun setupWindowInsetsHandling() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /** Initializes the Navigation Component. */
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as? NavHostFragment

        navController = navHostFragment?.navController
            ?: throw IllegalStateException("NavHostFragment not found")
    }

    /** Loads initial data on app startup. */
    private fun loadInitialData() {
        mainSharedViewModel.loadCurrentUser()
    }

    /** Handles notification intent navigation. */
    private fun handleNotificationIntent(intent: Intent?) {
        val chatId = intent?.getStringExtra("chatId")
        Log.d("MainActivity", "Notification intent chatId: $chatId")

        if (!chatId.isNullOrEmpty()) {
            // We found a chat ID! The user clicked a notification.
            Log.d("MainActivity", "Navigating to chat: $chatId")

            val action = NavGraphDirections.actionGlobalConversationFragment(
                id = chatId,
                contactName = intent.getStringExtra("senderName") ?: "",
                email = intent.getStringExtra("senderEmail") ?: "",
                profilePictureUrl = intent.getStringExtra("senderPictureUrl") ?: ""
            )
            navController.navigate(action)
        }

        val postId = intent?.getStringExtra("postId")
        if (!postId.isNullOrEmpty()) {
             val action = NavGraphDirections.actionGlobalPostDetailFragment(postId)
             navController.navigate(action)
        }
    }

    /** Request notification permission on Android 13+. */
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("HomeActivity", "Notification permission granted")
            initializeFCM()
        } else {
            Log.d("HomeActivity", "Notification permission denied")
            Toast.makeText(
                this,
                "You won't receive chat notifications",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /** Initializes FCM and retrieves the token. */
    private fun initializeFCM() {
        Log.d("HomeActivity", "Initializing FCM...")

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.e("HomeActivity", "Failed to get FCM token", task.exception)
                return@addOnCompleteListener
            }

            // Get the token
            val token = task.result
            Log.d("HomeActivity", "FCM Token retrieved: $token")

            // The token will be automatically saved to Firestore by ForumusFirebaseMessagingService.onNewToken()
            // But we can also manually trigger a save here to ensure it's stored
            saveFcmTokenToFirestore(token)
        }
    }

    /** Saves FCM token to Firestore. */
    private fun saveFcmTokenToFirestore(token: String) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

        if (userId == null) {
            Log.w("HomeActivity", "User not logged in, cannot save FCM token")
            return
        }

        Log.d("HomeActivity", "Saving FCM token for user: $userId")

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .update("fcmToken", token)
            .addOnSuccessListener {
                Log.d("HomeActivity", "FCM token saved to Firestore successfully")
            }
            .addOnFailureListener { e ->
                Log.e("HomeActivity", "Failed to save FCM token to Firestore", e)
            }
    }
    
    /** Checks if user is banned and redirects if necessary. */
    private fun checkBanStatus() {
        lifecycleScope.launch {
            try {
                val userRepository = UserRepository()
                val currentUser = userRepository.getCurrentUser()
                
                if (currentUser == null) {
                    Log.w("MainActivity", "No current user found")
                    return@launch
                }
                
                // Check if user is banned
                if (currentUser.status == UserStatus.BANNED && 
                    currentUser.blacklistedUntil != null && 
                    currentUser.blacklistedUntil > System.currentTimeMillis()) {
                    
                    Log.d("MainActivity", "User is banned, navigating to BannedActivity")
                    navigateToBannedActivity(currentUser.blacklistedUntil)
                } else {
                    Log.d("MainActivity", "User is not banned")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error checking ban status", e)
            }
        }
    }
    
    /** Navigates to BannedActivity. */
    private fun navigateToBannedActivity(blacklistedUntil: Long) {
        val intent = Intent(this, BannedActivity::class.java)
        intent.putExtra(BannedActivity.EXTRA_BLACKLISTED_UNTIL, blacklistedUntil)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    /** Applies saved theme preference. */
    private fun applyTheme() {
        val preferencesManager = PreferencesManager(application)
        val isDarkMode = preferencesManager.isDarkModeEnabled
        
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    /** Applies saved language preference. */
    private fun applyLocale() {
        val preferencesManager = PreferencesManager(application)
        val language = preferencesManager.language
        val code = if (language.equals("Vietnamese", ignoreCase = true) || language == "vi") "vi" else "en"
        
        // Sync with AppCompatDelegate if needed (e.g. fresh install or data clear)
        val currentAppLocales = AppCompatDelegate.getApplicationLocales()
        if (currentAppLocales.isEmpty) {
             val appLocale = LocaleListCompat.forLanguageTags(code)
             AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }
    
    /** Updates status bar appearance based on theme. */
    fun updateStatusBarAppearance() {
        val preferencesManager = PreferencesManager(application)
        val isDarkMode = preferencesManager.isDarkModeEnabled
        
        val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        
        // Set icon colors
        windowInsetsController.isAppearanceLightStatusBars = !isDarkMode
        windowInsetsController.isAppearanceLightNavigationBars = !isDarkMode
        
        // Set bar background colors from theme
        window.statusBarColor = if (isDarkMode) {
            getColor(com.hcmus.forumus_client.R.color.bg_app)
        } else {
            getColor(com.hcmus.forumus_client.R.color.white)
        }
        
        window.navigationBarColor = if (isDarkMode) {
            getColor(com.hcmus.forumus_client.R.color.bg_app)
        } else {
            getColor(com.hcmus.forumus_client.R.color.white)
        }
    }
}
