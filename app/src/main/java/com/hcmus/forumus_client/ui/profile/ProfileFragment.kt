package com.hcmus.forumus_client.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.data.model.CommentAction
import com.hcmus.forumus_client.data.model.PostAction
import com.hcmus.forumus_client.databinding.FragmentProfileBinding
import com.hcmus.forumus_client.ui.main.MainSharedViewModel
import android.util.Log
import com.hcmus.forumus_client.NavGraphDirections
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.ui.common.BottomNavigationBar
import com.hcmus.forumus_client.ui.common.PopupPostMenu
import com.hcmus.forumus_client.ui.common.ProfileMenuAction
import com.hcmus.forumus_client.ui.common.SharePostDialog

/**
 * Profile Fragment displaying a user's profile information and content (posts and comments).
 * Receives userId and mode via Safe Args from navigation.
 * Uses ProfileViewModel for profile-specific data and MainSharedViewModel for shared user data.
 */
class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private val viewModel: ProfileViewModel by viewModels()
    private val mainSharedViewModel: MainSharedViewModel by activityViewModels()
    private val navController by lazy { findNavController() }
    private lateinit var profileAdapter: ProfileAdapter

    // Receive userId via Safe Args
    private val args: ProfileFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = args.userId
        Log.d("ProfileFragment", "Received userId: $userId")

        if (userId.isEmpty()) {
            Toast.makeText(requireContext(), "Missing userId", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
            return
        }

        setupTopAppBar()
        setupSwipeRefresh()
        setupFilterButtons()
        setupRecyclerView()
        setupBottomNavigation()
        observeViewModel()

        // Initialize view model with user ID and mode
        viewModel.loadUserInfo(userId)
        mainSharedViewModel.loadCurrentUser()
    }

    /**
     * Setup top app bar callbacks for menu, search, and profile actions.
     * Observes MainSharedViewModel for current user data.
     */
    private fun setupTopAppBar() {
        binding.topAppBar.apply {
            onFuncClick = {
                navController.popBackStack()
            }
            onHomeClick = {
                navController.navigate(R.id.homeFragment)
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
                        navController.navigate(R.id.createPostFragment)
                    }
                }
            }

            setIconFuncButton(R.drawable.ic_back)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            val userId = args.userId

            if (userId.isNotEmpty()) {
                viewModel.loadUserInfo(userId)
            }
        }
    }
    /**
     * Setup bottom navigation bar for fragment switching.
     */
    private fun setupBottomNavigation() {
        binding.bottomBar.apply {
            setActiveTab(BottomNavigationBar.Tab.NONE)
            onHomeClick = { navController.navigate(R.id.homeFragment) }
            onExploreClick = { navController.navigate(R.id.searchFragment) }
            onCreatePostClick = { navController.navigate(R.id.createPostFragment) }
            onAlertsClick = { }
            onChatClick = { navController.navigate(R.id.chatsFragment) }
        }
    }

    /**
     * Setup filter buttons for different content views (GENERAL, POSTS, REPLIES).
     * Clicking a button switches the profile display mode.
     */
    private fun setupFilterButtons() {
        binding.generalButton.setOnClickListener { viewModel.setMode(ProfileMode.GENERAL) }
        binding.postButton.setOnClickListener { viewModel.setMode(ProfileMode.POSTS) }
        binding.replyButton.setOnClickListener { viewModel.setMode(ProfileMode.REPLIES) }
    }

    /**
     * Set up the RecyclerView with ProfileAdapter to display profile content.
     * Handles both posts and comments, with separate callbacks for each type.
     */
    private fun setupRecyclerView() {
        binding.contentRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        profileAdapter = ProfileAdapter(
            onPostAction = { post, action, view ->
                when (action) {
                    PostAction.UPVOTE, PostAction.DOWNVOTE -> viewModel.onPostAction(post, action)
                    PostAction.OPEN -> {
                        val navAction = ProfileFragmentDirections
                            .actionGlobalPostDetailFragment(post.id)
                        navController.navigate(navAction)
                    }
                    PostAction.REPLY -> {
                        val navAction = ProfileFragmentDirections
                            .actionGlobalPostDetailFragment(post.id)
                        navController.navigate(navAction)
                    }
                    PostAction.SHARE -> {
                        val shareDialog = SharePostDialog.newInstance(post.id)
                        shareDialog.show(childFragmentManager, "SharePostDialog")
                    }
                    PostAction.AUTHOR_PROFILE -> {
                        // Already on profile, ignore
                    }
                    PostAction.MENU -> {
                        showPostMenu(post, view)
                    }
                }
            },
            onCommentAction = { comment, action ->
                when (action) {
                    CommentAction.UPVOTE, CommentAction.DOWNVOTE -> viewModel.onCommentAction(comment, action)
                    CommentAction.OPEN -> {
                        val navAction = ProfileFragmentDirections
                            .actionGlobalPostDetailFragment(comment.postId)
                        navController.navigate(navAction) }
                    CommentAction.REPLY -> {
                        val navAction = ProfileFragmentDirections
                            .actionGlobalPostDetailFragment(comment.postId)
                        navController.navigate(navAction)
                    }
                    CommentAction.AUTHOR_PROFILE -> {
                        // Already on profile, ignore
                    }
                    CommentAction.REPLIED_USER_PROFILE -> {
                        comment.replyToUserId?.let {
                            val navAction = ProfileFragmentDirections
                                .actionGlobalProfileFragment(it)
                            navController.navigate(navAction)
                        }
                    }

                    CommentAction.VIEW_REPLIES -> TODO()
                }
            }
        )

        binding.contentRecyclerView.adapter = profileAdapter
    }

    /**
     * Observe all ViewModel LiveData streams and update UI accordingly.
     */
    private fun observeViewModel() {
        // Update profile header with user information
        viewModel.user.observe(viewLifecycleOwner) { user ->
            binding.username.text = user.fullName.ifBlank { "Anonymous" }
            binding.userEmail.text = user.email

            binding.userAvatar.load(user.profilePictureUrl) {
                crossfade(true)
                placeholder(R.drawable.default_avatar)
                error(R.drawable.default_avatar)
            }
            binding.topAppBar.setProfileImage(user.profilePictureUrl)
        }

        // Update statistics display
        viewModel.postsCount.observe(viewLifecycleOwner) { count ->
            binding.postsCount.text = count.toString()
        }
        viewModel.repliesCount.observe(viewLifecycleOwner) { count ->
            binding.repliesCount.text = count.toString()
        }
        viewModel.upvotesCount.observe(viewLifecycleOwner) { count ->
            binding.upvotesCount.text = count.toString()
        }

        // Update filter buttons UI when mode changes
        viewModel.mode.observe(viewLifecycleOwner) { mode ->
            // RecyclerView continues using same adapter, only data source changes
            updateFilterUI(mode)
            binding.contentRecyclerView.scrollToPosition(0)
        }

        // Update adapter with filtered visible items
        viewModel.visibleItems.observe(viewLifecycleOwner) { items ->
            profileAdapter.submitList(items)
        }

        // Display error messages to user
        viewModel.error.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Error: $msg", Toast.LENGTH_SHORT).show()
            }
        }

        // Monitor loading state (can show/hide ProgressBar if needed)
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
        }
    }

    /**
     * Update the visual appearance of filter buttons based on current mode.
     */
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

    /**
     * Display the post action menu popup when user taps the menu icon on a post. Allows users to
     * save or report the post.
     */
    private fun showPostMenu(post: Post, menuButton: View) {
        val popupMenu = PopupPostMenu(requireActivity() as androidx.appcompat.app.AppCompatActivity)

        // Handle save button click
        popupMenu.onSaveClick = {
            Toast.makeText(requireContext(), "Post saved", Toast.LENGTH_SHORT).show()
        }

        // Handle violation selection from report menu
        popupMenu.onReportClick = { violation -> viewModel.saveReport(post, violation) }

        // Show popup at menu button
        popupMenu.show(menuButton)
    }
}
