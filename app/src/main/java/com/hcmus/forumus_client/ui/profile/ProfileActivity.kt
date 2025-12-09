package com.hcmus.forumus_client.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.databinding.ActivityProfileBinding
import com.hcmus.forumus_client.ui.navigation.AppNavigator
import com.hcmus.forumus_client.data.model.PostAction
import com.hcmus.forumus_client.data.model.CommentAction
import com.hcmus.forumus_client.ui.common.ProfileMenuAction
import com.hcmus.forumus_client.ui.common.BottomNavigationBar
import kotlin.getValue

/**
 * Activity for displaying a user's profile information and content (posts and comments).
 *
 * This activity shows:
 * - User profile details (avatar, name, email, stats)
 * - User's posts and comments filtered by mode (GENERAL/POSTS/REPLIES)
 * - Filter buttons to switch between different content views
 * - Bottom navigation bar for app-wide navigation
 *
 * The profile can be opened in different modes based on intent extras,
 * and supports navigation to other screens through the AppNavigator.
 */
class ProfileActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_USER_ID = "extra_user_id"
        const val EXTRA_MODE = "extra_mode"
    }

    private lateinit var binding: ActivityProfileBinding
    private val viewModel: ProfileViewModel by viewModels()

    // Single adapter to handle both posts and comments in mixed view
    private lateinit var profileAdapter: ProfileAdapter

    private val navigator by lazy { AppNavigator(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Extract userId from intent, required parameter
        val userId = intent.getStringExtra(EXTRA_USER_ID) ?: run {
            Toast.makeText(this, "Missing userId", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Extract initial display mode from intent, defaults to GENERAL if not specified
        val initialMode = intent.getStringExtra(EXTRA_MODE)?.let {
            runCatching { ProfileMode.valueOf(it) }.getOrDefault(ProfileMode.GENERAL)
        } ?: ProfileMode.GENERAL

        // Setup UI components and observe view model
        setupWindowInsetsHandling()
        setupTopAppBar()
        setupFilterButtons()
        setupRecyclerView()
        setupBottomNavigationBar()
        observeViewModel()

        // Initialize view model with user ID and mode
        viewModel.init(userId, initialMode)
        viewModel.loadCurrentUser()
    }

    override fun onResume() {
        super.onResume()

        // Re-extract and reinitialize on resume to handle intent changes
        val userId = intent.getStringExtra(EXTRA_USER_ID) ?: run {
            Toast.makeText(this, "Missing userId", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val initialMode = intent.getStringExtra(EXTRA_MODE)?.let {
            runCatching { ProfileMode.valueOf(it) }.getOrDefault(ProfileMode.GENERAL)
        } ?: ProfileMode.GENERAL

        viewModel.init(userId, initialMode)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Handle new intent with updated user ID and mode
        val userId = intent.getStringExtra(EXTRA_USER_ID) ?: return
        val mode = intent.getStringExtra(EXTRA_MODE)?.let {
            runCatching { ProfileMode.valueOf(it) }.getOrDefault(ProfileMode.GENERAL)
        } ?: ProfileMode.GENERAL

        viewModel.init(userId, mode)
    }

    /**
     * Handles system window insets by applying padding to avoid overlap with status bar,
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
     * Initializes the top app bar component with menu and navigation callbacks.
     *
     * Handles profile menu actions (view profile, edit, dark mode, settings)
     * and navigation through the AppNavigator.
     */
    private fun setupTopAppBar() {
        binding.topAppBar.apply {
            onMenuClick = {
                Toast.makeText(this@ProfileActivity, "Menu clicked", Toast.LENGTH_SHORT).show()
            }
            onHomeClick = { navigator.openHome() }
            onSearchClick = { navigator.openSearch() }
            onProfileMenuAction = onProfileMenuAction@{ action ->
                when (action) {
                    ProfileMenuAction.VIEW_PROFILE -> {
                        // Navigate to current user's own profile
                        val currentUser = viewModel.currentUser.value ?: return@onProfileMenuAction
                        navigator.openProfile(currentUser.uid)
                    }

                    ProfileMenuAction.EDIT_PROFILE -> {
                        // TODO: Implement edit profile navigation
                        // navigator.openEditProfile()
                    }

                    ProfileMenuAction.TOGGLE_DARK_MODE -> {
                        // TODO: Implement theme toggle functionality
                        // toggle theme
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
     * Sets up the RecyclerView with the ProfileAdapter to display posts and comments.
     *
     * Configures callbacks for:
     * - Post actions (upvote, downvote, open details, reply, share)
     * - Comment actions (upvote, downvote, open post details, reply to user profile)
     */
    private fun setupRecyclerView() {
        binding.contentRecyclerView.layoutManager = LinearLayoutManager(this)

        profileAdapter = ProfileAdapter(
            onPostAction = { post, action, view ->
                when (action) {
                    PostAction.UPVOTE, PostAction.DOWNVOTE -> viewModel.onPostAction(post, action)
                    PostAction.OPEN -> {
                        navigator.onDetailPost(post.id)
                    }
                    PostAction.REPLY -> {
                        navigator.onDetailPost(post.id)
                    }
                    PostAction.SHARE -> {
                        Toast.makeText(this, "Share post from profile", Toast.LENGTH_SHORT).show()
                    }
                    PostAction.AUTHOR_PROFILE -> {
                        // Already on profile, ignore
                    }
                    PostAction.MENU -> {
                        // TODO: Implement post menu functionality
                    }
                }
            },
            onCommentAction = { comment, action ->
                when (action) {
                    CommentAction.UPVOTE, CommentAction.DOWNVOTE -> viewModel.onCommentAction(comment, action)
                    CommentAction.OPEN -> {
                        navigator.onDetailPost(comment.postId)
                    }
                    CommentAction.REPLY -> {
                        navigator.onDetailPost(comment.postId)
                    }
                    CommentAction.AUTHOR_PROFILE -> {
                        // Already on profile, ignore
                    }
                    CommentAction.REPLIED_USER_PROFILE -> {
                        comment.replyToUserId?.let {
                            if (it != viewModel.user.value?.uid)
                                navigator.openProfile(it)
                        }
                    }
                }
            }
        )

        binding.contentRecyclerView.adapter = profileAdapter
    }

    /**
     * Sets up filter buttons to switch between content display modes.
     *
     * Three modes available:
     * - GENERAL: Shows all posts and comments mixed
     * - POSTS: Shows only user's posts
     * - REPLIES: Shows only user's comments
     */
    private fun setupFilterButtons() {
        binding.generalButton.setOnClickListener { viewModel.setMode(ProfileMode.GENERAL) }
        binding.postButton.setOnClickListener { viewModel.setMode(ProfileMode.POSTS) }
        binding.replyButton.setOnClickListener { viewModel.setMode(ProfileMode.REPLIES) }
    }

    /**
     * Sets up the bottom navigation bar with navigation callbacks.
     *
     * Provides access to Home, Explore, Create Post, Alerts, and Chat screens.
     */
    private fun setupBottomNavigationBar() {
        binding.bottomBar.apply {
            onHomeClick = { navigator.openHome() }
            onExploreClick = { navigator.openSearch() }
            onCreatePostClick = { navigator.openCreatePost() }
            onAlertsClick = { navigator.openAlerts() }
            onChatClick = { navigator.openChat() }
            setActiveTab(BottomNavigationBar.Tab.NONE)
        }
    }

    /**
     * Observes all LiveData from the view model and updates UI accordingly.
     *
     * Observes:
     * - User profile information (name, email, avatar)
     * - Post count, comment count, and upvote count statistics
     * - Current display mode for filter UI update
     * - Visible items list for RecyclerView adapter
     * - Current user information for top app bar avatar
     * - Error messages and loading state
     */
    private fun observeViewModel() {
        // Update profile header with user information
        viewModel.user.observe(this) { user ->
            binding.username.text = user.fullName.ifBlank { "Anonymous" }
            binding.userEmail.text = user.email

            binding.userAvatar.load(user.profilePictureUrl) {
                crossfade(true)
                placeholder(R.drawable.default_avatar)
                error(R.drawable.default_avatar)
            }
        }

        // Update statistics display
        viewModel.postsCount.observe(this) { count ->
            binding.postsCount.text = count.toString()
        }
        viewModel.repliesCount.observe(this) { count ->
            binding.repliesCount.text = count.toString()
        }
        viewModel.upvotesCount.observe(this) { count ->
            binding.upvotesCount.text = count.toString()
        }

        // Update filter buttons UI when mode changes
        viewModel.mode.observe(this) { mode ->
            // RecyclerView continues using same adapter, only data source changes
            updateFilterUI(mode)
            binding.contentRecyclerView.scrollToPosition(0)
        }

        // Update adapter with filtered visible items
        viewModel.visibleItems.observe(this) { items ->
            profileAdapter.submitList(items)
        }

        // Update top app bar with current user's avatar
        viewModel.currentUser.observe(this) { user ->
            binding.topAppBar.setProfileImage(user?.profilePictureUrl)
        }

        // Display error messages to user
        viewModel.error.observe(this) { msg ->
            if (!msg.isNullOrBlank()) {
                Toast.makeText(this, "Error: $msg", Toast.LENGTH_SHORT).show()
            }
        }

        // Monitor loading state (can show/hide ProgressBar if needed)
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading == true) {
                binding.progressBar.visibility = View.VISIBLE
                binding.contentRecyclerView.visibility = View.GONE
            } else {
                binding.progressBar.visibility = View.GONE
                binding.contentRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Updates the visual state of filter buttons based on the current mode.
     *
     * Applies active/inactive background resources to highlight the selected filter.
     *
     * @param mode The current ProfileMode (GENERAL, POSTS, or REPLIES)
     */
    private fun updateFilterUI(mode: ProfileMode) {
        with(binding) {
            when (mode) {
                ProfileMode.GENERAL -> {
                    generalButton.setBackgroundResource(R.drawable.filter_button_active)
                    postButton.setBackgroundResource(R.drawable.filter_button_inactive)
                    replyButton.setBackgroundResource(R.drawable.filter_button_inactive)
                }
                ProfileMode.POSTS -> {
                    generalButton.setBackgroundResource(R.drawable.filter_button_inactive)
                    postButton.setBackgroundResource(R.drawable.filter_button_active)
                    replyButton.setBackgroundResource(R.drawable.filter_button_inactive)
                }
                ProfileMode.REPLIES -> {
                    generalButton.setBackgroundResource(R.drawable.filter_button_inactive)
                    postButton.setBackgroundResource(R.drawable.filter_button_inactive)
                    replyButton.setBackgroundResource(R.drawable.filter_button_active)
                }
            }
        }
    }
}