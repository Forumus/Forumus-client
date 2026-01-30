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
import com.hcmus.forumus_client.data.repository.SavePostResult

/**
 * Displays a user's profile with their info, posts, and comments.
 * Supports filtering between all content, posts only, or comments only.
 */
class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private val viewModel: ProfileViewModel by viewModels()
    private val mainSharedViewModel: MainSharedViewModel by activityViewModels()
    private val notificationViewModel: com.hcmus.forumus_client.ui.notification.NotificationViewModel by activityViewModels()
    private val chatsViewModel: com.hcmus.forumus_client.ui.chats.ChatsViewModel by activityViewModels()
    private val navController by lazy { findNavController() }
    private lateinit var profileAdapter: ProfileAdapter

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

        // Load profile data for the user
        viewModel.loadUserInfo(userId)
        mainSharedViewModel.loadCurrentUser()
    }

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
                        navController.navigate(R.id.editProfileFragment)
                    }

                    ProfileMenuAction.TOGGLE_DARK_MODE -> {
                        val preferencesManager = com.hcmus.forumus_client.data.local.PreferencesManager(requireContext())
                        val currentMode = preferencesManager.isDarkModeEnabled
                        preferencesManager.isDarkModeEnabled = !currentMode

                        if (!currentMode) {
                            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                            )
                        } else {
                            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                            )
                        }

                        (activity as? com.hcmus.forumus_client.ui.main.MainActivity)?.updateStatusBarAppearance()
                    }

                    ProfileMenuAction.SETTINGS -> {
                        navController.navigate(R.id.settingsFragment)
                    }
                }
            }

            setIconFuncButton(R.drawable.ic_back)
            setProfileImage(mainSharedViewModel.currentUser.value?.profilePictureUrl)
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

    private fun setupBottomNavigation() {
        binding.bottomBar.apply {
            setActiveTab(BottomNavigationBar.Tab.NONE)
            onHomeClick = { navController.navigate(R.id.homeFragment) }
            onExploreClick = { navController.navigate(R.id.searchFragment) }
            onCreatePostClick = { navController.navigate(R.id.createPostFragment) }
            onAlertsClick = { navController.navigate(NavGraphDirections.actionGlobalNotificationFragment()) }
            onChatClick = { navController.navigate(R.id.chatsFragment) }
        }
    }

    // Filter buttons switch between showing all content, posts only, or comments only
    private fun setupFilterButtons() {
        binding.generalButton.setOnClickListener { viewModel.setMode(ProfileMode.GENERAL) }
        binding.postButton.setOnClickListener { viewModel.setMode(ProfileMode.POSTS) }
        binding.replyButton.setOnClickListener { viewModel.setMode(ProfileMode.REPLIES) }
    }

    private fun setupRecyclerView() {
        binding.contentRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        profileAdapter = ProfileAdapter(
            onPostAction = { post, action, view ->
                when (action) {
                    PostAction.UPVOTE, PostAction.DOWNVOTE -> viewModel.onPostAction(post, action)
                    // Navigate to post detail for open/reply actions
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
                    // Ignore: already on profile, summary not available here
                    PostAction.AUTHOR_PROFILE, PostAction.SUMMARY -> { }
                    PostAction.MENU -> {
                        showPostMenu(post, view)
                    }
                }
            },
            onCommentAction = { comment, action ->
                when (action) {
                    CommentAction.UPVOTE, CommentAction.DOWNVOTE -> viewModel.onCommentAction(comment, action)
                    // Navigate to the parent post to see comment in context
                    CommentAction.OPEN -> {
                        val navAction = ProfileFragmentDirections
                            .actionGlobalPostDetailFragment(comment.postId)
                        navController.navigate(navAction) }
                    CommentAction.REPLY -> {
                        val navAction = ProfileFragmentDirections
                            .actionGlobalPostDetailFragment(comment.postId)
                        navController.navigate(navAction)
                    }
                    CommentAction.AUTHOR_PROFILE -> { }
                    CommentAction.REPLIED_USER_PROFILE -> {
                        comment.replyToUserId?.let {
                            val navAction = ProfileFragmentDirections
                                .actionGlobalProfileFragment(it)
                            navController.navigate(navAction)
                        }
                    }

                    CommentAction.VIEW_REPLIES -> {
                        val navAction = ProfileFragmentDirections
                            .actionGlobalPostDetailFragment(comment.postId)
                        navController.navigate(navAction)
                    }
                }
            }
        )

        binding.contentRecyclerView.adapter = profileAdapter
    }

    private fun observeViewModel() {
        // Profile header
        viewModel.user.observe(viewLifecycleOwner) { user ->
            binding.username.text = user.fullName.ifBlank { getString(R.string.anonymous) }
            binding.userEmail.text = user.email

            binding.userAvatar.load(user.profilePictureUrl) {
                crossfade(true)
                placeholder(R.drawable.default_avatar)
                error(R.drawable.default_avatar)
            }
        }

        // Bottom bar badge counts
        notificationViewModel.unreadCount.observe(viewLifecycleOwner) { count ->
            android.util.Log.d("ProfileFragment", "Notification badge update: $count")
            binding.bottomBar.setNotificationBadge(count)
        }
        chatsViewModel.unreadChatCount.observe(viewLifecycleOwner) { count ->
            android.util.Log.d("ProfileFragment", "Chat badge update: $count")
            binding.bottomBar.setChatBadge(count)
        }

        // Profile stats
        viewModel.postsCount.observe(viewLifecycleOwner) { count ->
            binding.postsCount.text = count.toString()
        }
        viewModel.repliesCount.observe(viewLifecycleOwner) { count ->
            binding.repliesCount.text = count.toString()
        }
        viewModel.upvotesCount.observe(viewLifecycleOwner) { count ->
            binding.upvotesCount.text = count.toString()
        }

        // Update filter button styles and scroll to top when mode changes
        viewModel.mode.observe(viewLifecycleOwner) { mode ->
            updateFilterUI(mode)
            binding.contentRecyclerView.scrollToPosition(0)
        }

        viewModel.visibleItems.observe(viewLifecycleOwner) { items ->
            profileAdapter.submitList(items)
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Error: $msg", Toast.LENGTH_SHORT).show()
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

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
        }
    }

    // Highlight the active filter button
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

    private fun showPostMenu(post: Post, menuButton: View) {
        val popupMenu = PopupPostMenu(requireActivity() as androidx.appcompat.app.AppCompatActivity)
        popupMenu.onSaveClick = { viewModel.savePost(post) }
        popupMenu.onReportClick = { violation -> viewModel.saveReport(post, violation) }
        popupMenu.show(menuButton)
    }
}
