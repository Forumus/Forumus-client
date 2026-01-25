package com.hcmus.forumus_client.ui.home

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.hcmus.forumus_client.NavGraphDirections
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.model.PostAction
import com.hcmus.forumus_client.data.model.Topic
import com.hcmus.forumus_client.databinding.FragmentHomeBinding
import com.hcmus.forumus_client.ui.common.BottomNavigationBar
import com.hcmus.forumus_client.ui.common.PopupPostMenu
import com.hcmus.forumus_client.ui.common.ProfileMenuAction
import com.hcmus.forumus_client.ui.main.MainSharedViewModel
import com.hcmus.forumus_client.ui.common.SharePostDialog
import com.hcmus.forumus_client.data.repository.SavePostResult
import kotlin.text.ifEmpty
import kotlin.text.lowercase
import kotlin.text.startsWith

/**
 * Home Fragment displaying a feed of posts with voting and interaction features. Uses NavController
 * for navigation and Safe Args for passing data. Shares MainSharedViewModel for current user data.
 */
class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var homeAdapter: HomeAdapter
    private val viewModel: HomeViewModel by viewModels()
    private val mainSharedViewModel: MainSharedViewModel by activityViewModels()
    private val notificationViewModel: com.hcmus.forumus_client.ui.notification.NotificationViewModel by activityViewModels()
    private val chatsViewModel: com.hcmus.forumus_client.ui.chats.ChatsViewModel by activityViewModels()
    private val navController by lazy { findNavController() }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTopAppBar()
        setupSwipeRefresh()
        setupRecyclerView()
        setupBottomNavigation()
        setupDrawer()
        observeViewModel()

        viewModel.loadPosts()
        viewModel.loadTopics()

    }

    /**
     * Setup top app bar callbacks for menu, search, and profile actions. Observes
     * MainSharedViewModel for current user data.
     */
    private fun setupTopAppBar() {
        binding.topAppBar.apply {
            onFuncClick = { binding.drawerLayout.openDrawer(GravityCompat.START) }
            onHomeClick = { navController.navigate(R.id.homeFragment) }
            onProfileMenuAction =
                    onProfileMenuAction@{ action ->
                        when (action) {
                            ProfileMenuAction.VIEW_PROFILE -> {
                                val currentUser =
                                        mainSharedViewModel.currentUser.value
                                                ?: return@onProfileMenuAction

                                val navAction =
                                        NavGraphDirections.actionGlobalProfileFragment(
                                                currentUser.uid
                                        )

                                navController.navigate(navAction)
                            }
                            ProfileMenuAction.EDIT_PROFILE -> {
                                navController.navigate(R.id.editProfileFragment)
                            }
                            ProfileMenuAction.TOGGLE_DARK_MODE -> {
                                // Toggle dark mode preference
                                val preferencesManager = com.hcmus.forumus_client.data.local.PreferencesManager(requireContext())
                                val currentMode = preferencesManager.isDarkModeEnabled
                                preferencesManager.isDarkModeEnabled = !currentMode
                                
                                // Apply new theme
                                if (!currentMode) {
                                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                                        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                                    )
                                } else {
                                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                                        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                                    )
                                }
                                
                                // Update system status bar and navigation bar colors
                                (activity as? com.hcmus.forumus_client.ui.main.MainActivity)?.updateStatusBarAppearance()
                            }
                            ProfileMenuAction.SETTINGS -> {
                                navController.navigate(R.id.settingsFragment)
                            }
                        }
                    }

            setIconFuncButton(R.drawable.ic_hamburger_button)
        }
    }

    /**
     * Sets up the RecyclerView with HomeAdapter to display posts. Configures post action callbacks
     * for upvote, downvote, and navigation. Adds scroll listener for infinite scrolling.
     */
    private fun setupRecyclerView() {
        homeAdapter =
                HomeAdapter(emptyList()) { post, action, view ->
                    when (action) {
                        PostAction.OPEN -> {
                            // Navigate to PostDetailFragment using Safe Args
                            val action =
                                    HomeFragmentDirections.actionGlobalPostDetailFragment(post.id)
                            navController.navigate(action)
                        }
                        PostAction.UPVOTE -> viewModel.onPostAction(post, PostAction.UPVOTE)
                        PostAction.DOWNVOTE -> viewModel.onPostAction(post, PostAction.DOWNVOTE)
                        PostAction.REPLY -> {
                            // Navigate to PostDetailFragment using Safe Args
                            val action =
                                    HomeFragmentDirections.actionGlobalPostDetailFragment(post.id)
                            navController.navigate(action)
                        }
                        PostAction.SHARE -> {
                            val shareDialog = SharePostDialog.newInstance(post.id)
                            shareDialog.show(childFragmentManager, "SharePostDialog")
                        }
                        PostAction.AUTHOR_PROFILE -> {
                            // Navigate to ProfileFragment using Safe Args
                            val action =
                                    HomeFragmentDirections.actionGlobalProfileFragment(
                                            post.authorId
                                    )
                            navController.navigate(action)
                        }
                        PostAction.MENU -> showPostMenu(post, view)
                    }
                }

        val layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        binding.postRecyclerView.apply {
            adapter = homeAdapter
            this.layoutManager = layoutManager

            // Add scroll listener for infinite scrolling
            addOnScrollListener(
                    object : RecyclerView.OnScrollListener() {
                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            super.onScrolled(recyclerView, dx, dy)

                            // Check if user scrolled down
                            if (dy > 0) {
                                val visibleItemCount = layoutManager.childCount
                                val totalItemCount = layoutManager.itemCount
                                val firstVisibleItemPosition =
                                        layoutManager.findFirstVisibleItemPosition()

                                // Load more when reaching the last 3 items
                                if ((visibleItemCount + firstVisibleItemPosition) >=
                                                totalItemCount - 3
                                ) {
                                    viewModel.loadMorePosts()
                                }
                            }
                        }
                    }
            )
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener { viewModel.loadPosts() }
    }

    /** Setup bottom navigation bar for fragment switching. */
    private fun setupBottomNavigation() {
        binding.bottomBar.apply {
            setActiveTab(BottomNavigationBar.Tab.HOME)
            onHomeClick = { navController.navigate(R.id.homeFragment) }
            onExploreClick = { navController.navigate(R.id.searchFragment) }
            onCreatePostClick = { navController.navigate(R.id.createPostFragment) }
            onAlertsClick = { navController.navigate(NavGraphDirections.actionGlobalNotificationFragment()) }
            onChatClick = { navController.navigate(R.id.chatsFragment) }
        }
    }

    /** Initializes the navigation drawer callbacks. */
    private fun setupDrawer() {

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

    /** Populates the topics linear layout with dynamic views. */
    private fun populateTopics(topics: List<Topic>) {
        val container = binding.navView.findViewById<LinearLayout>(R.id.topics_container) ?: return
        container.removeAllViews()

        val density = resources.displayMetrics.density

        // Resolve selectableItemBackground
        val typedValue = TypedValue()
        requireActivity()
                .theme
                .resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
        val selectableBackground = typedValue.resourceId

        for (topic in topics) {
            val itemView =
                    LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams =
                                LinearLayout.LayoutParams(
                                                LinearLayout.LayoutParams.MATCH_PARENT,
                                                (48 * density).toInt()
                                        )
                                        .apply { bottomMargin = (12 * density).toInt() }
                        gravity = Gravity.CENTER_VERTICAL
                        if (selectableBackground != 0) {
                            setBackgroundResource(selectableBackground)
                        }
                        isClickable = true
                        isFocusable = true
                        tag = topic.id // Set tag for identification
                    }

            val iconView =
                    ImageView(requireContext()).apply {
                        layoutParams =
                                LinearLayout.LayoutParams(
                                        (24 * density).toInt(),
                                        (24 * density).toInt()
                                )

                        // improved icon lookup logic
                        val iconName = topic.icon.ifEmpty { topic.name }
                        val normalizedName =
                                iconName.lowercase()
                                        .replace(" ", "_")
                                        .replace("&", "")
                                        .replace("__", "_")

                        // Try multiple patterns
                        // 1. Exact match (e.g. "ic_biology")
                        var resId =
                                resources.getIdentifier(
                                        iconName,
                                        "drawable",
                                        requireContext().packageName
                                )

                        // 2. Try adding ic_ prefix if not present (e.g. "biology" -> "ic_biology")
                        if (resId == 0) {
                            if (!iconName.startsWith("ic_")) {
                                resId =
                                        resources.getIdentifier(
                                                "ic_$iconName",
                                                "drawable",
                                                requireContext().packageName
                                        )
                            }
                        }

                        // 3. Try normalized name with ic_ prefix (e.g. "Computer Science" ->
                        // "ic_computer_science")
                        if (resId == 0) {
                            resId =
                                    resources.getIdentifier(
                                            "ic_$normalizedName",
                                            "drawable",
                                            requireContext().packageName
                                    )
                        }

                        // 4. Try just normalized name (e.g. "computer_science")
                        if (resId == 0) {
                            resId =
                                    resources.getIdentifier(
                                            normalizedName,
                                            "drawable",
                                            requireContext().packageName
                                    )
                        }

                        if (resId != 0) {
                            setImageResource(resId)
                        } else {
                            // Fallback to URL or default
                            if (topic.icon.isNotEmpty() &&
                                            (topic.icon.startsWith("http") ||
                                                    topic.icon.startsWith("content"))
                            ) {
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

            val textView =
                    TextView(requireContext()).apply {
                        text = topic.name
                        textSize = 14f
                        setTextColor(requireContext().getColor(R.color.text_primary))
                        layoutParams =
                                LinearLayout.LayoutParams(
                                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                                ViewGroup.LayoutParams.WRAP_CONTENT
                                        )
                                        .apply { marginStart = (16 * density).toInt() }
                    }

            itemView.setOnClickListener { viewModel.toggleTopicSelection(topic.id) }

            itemView.addView(iconView)
            itemView.addView(textView)
            container.addView(itemView)
        }
    }

    // Updates the visual state of topic items in the drawer based on selection
    private fun updateTopicSelectionUI(selectedTopics: Set<String>) {
        val topicListContainer =
                binding.navView.findViewById<LinearLayout>(R.id.topics_container) ?: return

        // Iterate through all child views (topic items)
        for (i in 0 until topicListContainer.childCount) {
            val itemView = topicListContainer.getChildAt(i) as? LinearLayout ?: continue

            // We need to associate the view with the topic ID.
            // Ideally we set it as a tag in populateTopics.
            val topicId = itemView.tag as? String ?: continue

            if (selectedTopics.contains(topicId)) {
                itemView.setBackgroundColor(requireContext().getColor(R.color.filter_selected_bg))
            } else {
                val typedValue = TypedValue()
                requireActivity()
                        .theme
                        .resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
                if (typedValue.resourceId != 0) {
                    itemView.setBackgroundResource(typedValue.resourceId)
                } else {
                    itemView.setBackgroundResource(0)
                }
            }
        }
    }

    /** Observe all ViewModel LiveData streams and update UI accordingly. */
    private fun observeViewModel() {
        viewModel.posts.observe(viewLifecycleOwner) { posts -> homeAdapter.submitList(posts) }

        viewModel.topics.observe(viewLifecycleOwner) { topics ->
            populateTopics(topics)
            homeAdapter.setTopics(topics)
        }

        notificationViewModel.unreadCount.observe(viewLifecycleOwner) { count ->
             android.util.Log.d("HomeFragment", "Notification badge update: $count")
             binding.bottomBar.setNotificationBadge(count)
        }

        chatsViewModel.unreadChatCount.observe(viewLifecycleOwner) { count ->
             android.util.Log.d("HomeFragment", "Chat badge update: $count")
             binding.bottomBar.setChatBadge(count)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.swipeRefresh.isRefreshing = loading
        }

        viewModel.isLoadingMore.observe(viewLifecycleOwner) { isLoadingMore ->
            homeAdapter.setLoadingMore(isLoadingMore)
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrBlank()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.savePostResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                when (it) {
                    is SavePostResult.Success -> {
                        Toast.makeText(requireContext(), "Post saved successfully", Toast.LENGTH_SHORT).show()
                    }
                    is SavePostResult.AlreadySaved -> {
                        Toast.makeText(requireContext(), "Post is already saved", Toast.LENGTH_SHORT).show()
                    }
                    is SavePostResult.Error -> {
                        Toast.makeText(requireContext(), "Failed to save post: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                viewModel.clearSavePostResult()
            }
        }

        mainSharedViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            binding.topAppBar.setProfileImage(user?.profilePictureUrl)
        }

        viewModel.selectedTopics.observe(viewLifecycleOwner) { selected ->
            updateTopicSelectionUI(selected)
        }

        viewModel.sortOption.observe(viewLifecycleOwner) { sortOption ->
            val newItem = binding.navView.findViewById<LinearLayout>(R.id.item_new)
            val trendingItem = binding.navView.findViewById<LinearLayout>(R.id.item_trending)

            // Resolve selectableItemBackground once
            val typedValue = TypedValue()
            requireActivity()
                    .theme
                    .resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
            val selectableBackground = if (typedValue.resourceId != 0) typedValue.resourceId else 0

            // Update New item
            if (sortOption == HomeViewModel.SortOption.NEW) {
                newItem?.setBackgroundColor(requireContext().getColor(R.color.filter_selected_bg))
            } else {
                newItem?.setBackgroundResource(selectableBackground)
            }

            // Update Trending item
            if (sortOption == HomeViewModel.SortOption.TRENDING) {
                trendingItem?.setBackgroundColor(requireContext().getColor(R.color.filter_selected_bg))
            } else {
                trendingItem?.setBackgroundResource(selectableBackground)
            }
        }
    }

    /**
     * Display the post action menu popup when user taps the menu icon on a post. Allows users to
     * save or report the post.
     */
    private fun showPostMenu(post: Post, menuButton: View) {
        val popupMenu = PopupPostMenu(requireActivity() as androidx.appcompat.app.AppCompatActivity)

        // Handle save button click
        popupMenu.onSaveClick = {
            viewModel.savePost(post)
        }

        // Handle violation selection from report menu
        popupMenu.onReportClick = { violation -> viewModel.saveReport(post, violation) }

        // Show popup at menu button
        popupMenu.show(menuButton)
    }
}
