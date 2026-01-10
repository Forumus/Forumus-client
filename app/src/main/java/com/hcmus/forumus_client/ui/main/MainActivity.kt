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
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.messaging.FirebaseMessaging
import com.hcmus.forumus_client.NavGraphDirections
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.databinding.ActivityMainBinding

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsetsHandling()
        
        // Force light status bar (dark icons)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        
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

    /**
     * Handle notification intent and navigate to the conversation fragment if chatId is present.
     */
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

    /**
     * Activity Result Launcher to request notification permission on Android 13+.
     */
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

    /**
     * Initialize FCM and retrieve the token
     */
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

    /**
     * Manually save FCM token to Firestore
     */
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
}
