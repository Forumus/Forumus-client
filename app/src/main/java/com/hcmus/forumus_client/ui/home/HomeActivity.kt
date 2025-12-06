package com.hcmus.forumus_client.ui.home

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.hcmus.forumus_client.data.model.PostAction
import com.hcmus.forumus_client.databinding.ActivityHomeBinding
import com.hcmus.forumus_client.ui.common.BottomNavigationBar
import com.hcmus.forumus_client.ui.navigation.AppNavigator
import com.hcmus.forumus_client.ui.common.ProfileMenuAction
import androidx.core.view.WindowInsetsCompat

/**
 * Home activity displaying a feed of posts with voting and interaction features.
 * Uses RecyclerView with PostViewHolder for efficient list rendering.
 */
class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var postAdapter: HomeAdapter
    private val viewModel: HomeViewModel by viewModels()
    private val navigator by lazy { AppNavigator(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsetsHandling()
        setupTopAppBar()
        setupRecyclerView()
        setupBottomNavigationBar()
        observeViewModel()

        viewModel.loadCurrentUser()
        viewModel.loadPosts()
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
                        // TODO: Implement settings navigation
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
        postAdapter = HomeAdapter(emptyList()) { post, action ->
            when (action) {
                PostAction.OPEN -> navigator.onDetailPost(post.id)
                PostAction.UPVOTE -> viewModel.onPostAction(post, PostAction.UPVOTE)
                PostAction.DOWNVOTE -> viewModel.onPostAction(post, PostAction.DOWNVOTE)
                PostAction.REPLY -> navigator.onDetailPost(post.id)
                PostAction.SHARE -> Toast.makeText(this, "Share feature coming soon", Toast.LENGTH_SHORT).show()
                PostAction.AUTHOR_PROFILE -> navigator.openProfile(post.authorId)
            }
        }

        binding.postRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = postAdapter
        }
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
            postAdapter.submitList(it)
        }

        viewModel.currentUser.observe(this) { user ->
            binding.topAppBar.setProfileImage(user?.profilePictureUrl)
        }
    }
}
