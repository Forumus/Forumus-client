package com.hcmus.forumus_client.ui.home

import android.os.Bundle
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

        setupWindowInsetsHandling()
        setupTopAppBar()
        setupSwipeRefresh()
        setupRecyclerView()
        setupBottomNavigationBar()
        observeViewModel()

        viewModel.loadCurrentUser()
        viewModel.loadPosts()
    }

    override fun onResume() {
        super.onResume()
        // No session validity check - users can access app regardless of Remember Me choice
        // Session validation only happens at app startup in SplashActivity
    }

    /**
     * Handle system window insets by applying padding to avoid overlap with status bar,
     * navigation bar and keyboard (IME).
    */
    /**
     * Handle system window insets by applying padding to avoid overlap with status bar,
     * navigation bar and keyboard (IME).
     */
    private fun setupWindowInsetsHandling() {
        // Enable edge-to-edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            // Apply padding to root to avoid notch/system bars
            // Note: If we want transparency behind bars, we would set padding on content only
            // but for simplicity and existing behavior, padding root works.
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /**
     * Configures the top app bar with callbacks for menu, home, search, and profile actions.
     */
    private fun setupTopAppBar() {
        binding.topAppBar.apply {
            // Hide search button in home screen
            setSearchVisibility(false)
            
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
     * Sets up SwipeRefreshLayout to reload posts on pull-to-refresh.
     */
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadPosts()
        }
        
        // Ensure the refresh indicator doesn't overlap with system bars if necessary, 
        // though standard behavior usually handles this well.
        // Check if we need to adjust progress view end target or offset based on top inset.
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

            // Show bars when scrolling up
            addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                    if (dy < 0) { // Scrolling up
                        binding.appBarLayout.setExpanded(true, true)
                    }
                }
            })
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

            // Sync with AppBarLayout for swiping animation
            syncWithAppBar(binding.appBarLayout) { isHidden ->
                 // Optionally handle hidden state
            }
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
        
        viewModel.isLoading.observe(this) { isLoading ->
             binding.swipeRefresh.isRefreshing = isLoading
        }

        viewModel.currentUser.observe(this) { user ->
            binding.topAppBar.setProfileImage(user?.profilePictureUrl)
        }
    }
}
