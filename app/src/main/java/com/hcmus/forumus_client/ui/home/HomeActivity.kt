package com.hcmus.forumus_client.ui.home

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.hcmus.forumus_client.data.model.PostAction
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.databinding.ActivityHomeBinding
import com.hcmus.forumus_client.ui.common.BottomNavigationBar
import com.hcmus.forumus_client.ui.navigation.AppNavigator
import com.hcmus.forumus_client.ui.common.ProfileMenuAction
import com.hcmus.forumus_client.ui.common.PopupPostMenu
import androidx.core.view.WindowInsetsCompat
import android.view.View
import com.google.firebase.messaging.FirebaseMessaging
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Home activity displaying a feed of posts with voting and interaction features.
 * Uses RecyclerView with PostViewHolder for efficient list rendering.
 */
class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var homeAdapter: HomeAdapter
    private val viewModel: HomeViewModel by viewModels()
    private val navigator by lazy { AppNavigator(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        setupWindowInsetsHandling()
        setupTopAppBar()
        setupRecyclerView()
        setupBottomNavigationBar()
        observeViewModel()

        viewModel.loadCurrentUser()
        viewModel.loadPosts()
        
        // Initialize FCM and get token
        initializeFCM()
    }

    override fun onResume() {
        super.onResume()
        // Refresh posts when returning to home screen
        viewModel.loadPosts()
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

    /**
     * Configures the top app bar with callbacks for menu, home, search, and profile actions.
     */
    private fun setupTopAppBar() {
        binding.topAppBar.apply {
            // Menu button - placeholder for navigation drawer
            onMenuClick = { Toast.makeText(this@HomeActivity, "Menu clicked", Toast.LENGTH_SHORT).show() }
            // Logo navigates to home
            onHomeClick = { navigator.openHome() }
            // Search icon opens search functionality
            onSearchClick = { navigator.openSearch() }
            // Profile menu actions
            onProfileMenuAction = onProfileMenuAction@{ action ->
                when (action) {
                    ProfileMenuAction.VIEW_PROFILE -> {
                        val currentUser = viewModel.currentUser.value ?: return@onProfileMenuAction
                        navigator.openProfile(currentUser.uid)
                    }
                    ProfileMenuAction.EDIT_PROFILE -> {
                        // TODO: Implement edit profile navigation
                    }
                    ProfileMenuAction.TOGGLE_DARK_MODE -> {
                        // TODO: Implement dark mode toggle
                    }
                    ProfileMenuAction.SETTINGS -> {
                        navigator.openSettings()
                    }
                }
            }
            setProfileImage(null)
        }
    }

    /**
     * Sets up the RecyclerView with HomeAdapter to display posts.
     * Configures post action callbacks for upvote, downvote, and navigation.
     */
    private fun setupRecyclerView() {
        homeAdapter = HomeAdapter(emptyList()) { post, action, view ->
            when (action) {
                PostAction.OPEN -> navigator.onDetailPost(post.id)
                PostAction.UPVOTE -> viewModel.onPostAction(post, PostAction.UPVOTE)
                PostAction.DOWNVOTE -> viewModel.onPostAction(post, PostAction.DOWNVOTE)
                PostAction.REPLY -> navigator.onDetailPost(post.id)
                PostAction.SHARE -> Toast.makeText(this, "Share feature coming soon", Toast.LENGTH_SHORT).show()
                PostAction.AUTHOR_PROFILE -> navigator.openProfile(post.authorId)
                PostAction.MENU -> showPostMenu(post, view)
            }
        }

        binding.postRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = homeAdapter
        }
    }

    /**
     * Shows the post popup menu for the given post.
     * Allows user to save or report the post.
     *
     * @param post The post to show menu for
     */
    private fun showPostMenu(post: Post, menuButton: View) {
        val popupMenu = PopupPostMenu(this)
        
        // Handle save button click
        popupMenu.onSaveClick = {
            Toast.makeText(this, "Post saved", Toast.LENGTH_SHORT).show()
        }
        
        // Handle violation selection from report menu
        popupMenu.onReportClick = { violation ->
            viewModel.saveReport(post, violation)
        }

        // Show popup at RecyclerView center
        popupMenu.show(menuButton)
    }

    /**
     * Configures the bottom navigation bar with tab callbacks.
     */
    private fun setupBottomNavigationBar() {
        binding.bottomBar.apply {
            onHomeClick = { navigator.openHome() }
            onExploreClick = { navigator.openSearch() }
            onCreatePostClick = { navigator.openCreatePost() }
            onAlertsClick = { navigator.openAlerts() }
            onChatClick = { navigator.openChat() }
            setActiveTab(BottomNavigationBar.Tab.HOME)
        }
    }

    /**
     * Observes ViewModel data and updates UI accordingly.
     * - Posts: Updates adapter with new post list
     * - Current user: Updates top app bar profile image
     */
    private fun observeViewModel() {
        viewModel.posts.observe(this) {
            homeAdapter.submitList(it)
        }

        viewModel.currentUser.observe(this) { user ->
            binding.topAppBar.setProfileImage(user?.profilePictureUrl)
        }
    }
}