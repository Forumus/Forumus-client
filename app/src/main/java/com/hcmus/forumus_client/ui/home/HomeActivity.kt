package com.hcmus.forumus_client.ui.home

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.model.PostAction
import com.hcmus.forumus_client.data.model.Topic
import com.hcmus.forumus_client.databinding.ActivityHomeBinding
import com.hcmus.forumus_client.ui.common.BottomNavigationBar
import com.hcmus.forumus_client.ui.common.PopupPostMenu
import com.hcmus.forumus_client.ui.common.ProfileMenuAction
import com.hcmus.forumus_client.ui.navigation.AppNavigator

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
        setupDrawer()
        observeViewModel()

        viewModel.loadCurrentUser()
        viewModel.loadPosts()
        viewModel.loadTopics()
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
    private fun setupWindowInsetsHandling() {
        // Enable edge-to-edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        androidx.core.view.WindowInsetsControllerCompat(window, binding.root).isAppearanceLightStatusBars = true
        
        // Apply padding to CoordinatorLayout (content) to avoid system bars
        // We do NOT apply to binding.root (DrawerLayout) so that the drawer can slide under the status bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.coordinatorRoot) { v, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Apply padding to Navigation Drawer to avoid notch overlap
        ViewCompat.setOnApplyWindowInsetsListener(binding.navView) { v, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(0, systemBars.top, 0, systemBars.bottom)
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
            
            // Menu button - toggles navigation drawer
            onMenuClick = { 
                binding.drawerLayout.openDrawer(GravityCompat.START)
            }
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
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
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
     * Initializes the navigation drawer callbacks.
     */
    private fun setupDrawer() {
        val drawerContent = binding.navView.findViewById<ImageButton>(R.id.btn_close_drawer)
        drawerContent?.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        val newItem = binding.navView.findViewById<LinearLayout>(R.id.item_new)
        newItem?.setOnClickListener {
            viewModel.toggleSort(HomeViewModel.SortOption.NEW)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        val trendingItem = binding.navView.findViewById<LinearLayout>(R.id.item_trending)
        trendingItem?.setOnClickListener {
            viewModel.toggleSort(HomeViewModel.SortOption.TRENDING)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    /**
     * Populates the topics linear layout with dynamic views.
     */
    private fun populateTopics(topics: List<Topic>) {
        val container = binding.navView.findViewById<LinearLayout>(R.id.topics_container) ?: return
        container.removeAllViews()

        val density = resources.displayMetrics.density

        // Resolve selectableItemBackground
        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
        val selectableBackground = typedValue.resourceId

        for (topic in topics) {
            val itemView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (48 * density).toInt()
                ).apply {
                    bottomMargin = (12 * density).toInt()
                }
                gravity = Gravity.CENTER_VERTICAL
                if (selectableBackground != 0) {
                    setBackgroundResource(selectableBackground)
                }
                isClickable = true
                isFocusable = true
                tag = topic.id // Set tag for identification
            }

            val iconView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (24 * density).toInt(),
                    (24 * density).toInt()
                )
                
                // improved icon lookup logic
                val iconName = topic.icon.ifEmpty { topic.name }
                val normalizedName = iconName.lowercase().replace(" ", "_").replace("&", "").replace("__", "_")
                
                // Try multiple patterns
                // 1. Exact match (e.g. "ic_biology")
                var resId = resources.getIdentifier(iconName, "drawable", packageName)
                
                // 2. Try adding ic_ prefix if not present (e.g. "biology" -> "ic_biology")
                if (resId == 0) {
                     if (!iconName.startsWith("ic_")) {
                         resId = resources.getIdentifier("ic_$iconName", "drawable", packageName)
                     }
                }

                // 3. Try normalized name with ic_ prefix (e.g. "Computer Science" -> "ic_computer_science")
                if (resId == 0) {
                    resId = resources.getIdentifier("ic_$normalizedName", "drawable", packageName)
                }

                // 4. Try just normalized name (e.g. "computer_science")
                if (resId == 0) {
                    resId = resources.getIdentifier(normalizedName, "drawable", packageName)
                }

                if (resId != 0) {
                    setImageResource(resId)
                } else {
                    // Fallback to URL or default
                    if (topic.icon.isNotEmpty() && (topic.icon.startsWith("http") || topic.icon.startsWith("content"))) {
                        this.load(topic.icon) {
                            placeholder(R.drawable.ic_study_groups)
                            error(R.drawable.ic_study_groups)
                        }
                    } else {
                         setImageResource(R.drawable.ic_study_groups)
                    }
                }
                contentDescription = topic.name
            }

            val textView = TextView(this).apply {
                text = topic.name
                textSize = 14f
                setTextColor(android.graphics.Color.parseColor("#333333"))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = (16 * density).toInt()
                }
            }

            itemView.setOnClickListener {
                viewModel.toggleTopicSelection(topic.id)
            }

            itemView.addView(iconView)
            itemView.addView(textView)
            container.addView(itemView)
        }
    }
    
    // Updates the visual state of topic items in the drawer based on selection
    private fun updateTopicSelectionUI(selectedTopics: Set<String>) {
        val topicListContainer = binding.navView.findViewById<LinearLayout>(R.id.topics_container) ?: return
        
        // Iterate through all child views (topic items)
        for (i in 0 until topicListContainer.childCount) {
             val itemView = topicListContainer.getChildAt(i) as? LinearLayout ?: continue
             
             // We need to associate the view with the topic ID. 
             // Ideally we set it as a tag in populateTopics.
             val topicId = itemView.tag as? String ?: continue
             
             if (selectedTopics.contains(topicId)) {
                 itemView.setBackgroundColor(android.graphics.Color.parseColor("#E1E1E1"))
             } else {
                 val typedValue = TypedValue()
                 theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
                 if (typedValue.resourceId != 0) {
                     itemView.setBackgroundResource(typedValue.resourceId)
                 } else {
                     itemView.setBackgroundResource(0)
                 }
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
        
        viewModel.topics.observe(this) { topics ->
            populateTopics(topics)
            homeAdapter.setTopics(topics)
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
             binding.swipeRefresh.isRefreshing = isLoading
        }

        viewModel.currentUser.observe(this) { user ->
            binding.topAppBar.setProfileImage(user?.profilePictureUrl)
        }

        viewModel.selectedTopics.observe(this) { selected ->
            updateTopicSelectionUI(selected)
        }

        viewModel.sortOption.observe(this) { sortOption ->
            val newItem = binding.navView.findViewById<LinearLayout>(R.id.item_new)
            val trendingItem = binding.navView.findViewById<LinearLayout>(R.id.item_trending)
            
            // Resolve selectableItemBackground once
            val typedValue = TypedValue()
            theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
            val selectableBackground = if (typedValue.resourceId != 0) typedValue.resourceId else 0

            // Update New item
            if (sortOption == HomeViewModel.SortOption.NEW) {
                newItem?.setBackgroundColor(android.graphics.Color.parseColor("#E1E1E1"))
            } else {
                newItem?.setBackgroundResource(selectableBackground)
            }

            // Update Trending item
            if (sortOption == HomeViewModel.SortOption.TRENDING) {
                trendingItem?.setBackgroundColor(android.graphics.Color.parseColor("#E1E1E1"))
            } else {
                trendingItem?.setBackgroundResource(selectableBackground)
            }
        }
    }
}
