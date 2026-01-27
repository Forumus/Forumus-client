package com.hcmus.forumus_client.ui.post.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.hcmus.forumus_client.NavGraphDirections
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.data.model.CommentAction
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.model.PostAction
import com.hcmus.forumus_client.databinding.FragmentPostDetailBinding
import com.hcmus.forumus_client.ui.common.PopupPostMenu
import com.hcmus.forumus_client.ui.common.ProfileMenuAction
import com.hcmus.forumus_client.ui.common.SharePostDialog
import com.hcmus.forumus_client.ui.main.MainSharedViewModel
import com.hcmus.forumus_client.data.repository.SavePostResult

/**
 * Post Detail Fragment displaying a single post and all its comments.
 * Receives postId via Safe Args from navigation.
 * Hides BottomNavigationBar while showing an input bar for commenting.
 */
class PostDetailFragment : Fragment() {

    private lateinit var binding: FragmentPostDetailBinding
    private val viewModel: PostDetailViewModel by viewModels()
    private val mainSharedViewModel: MainSharedViewModel by activityViewModels()
    private val navController by lazy { findNavController() }
    private lateinit var detailAdapter: PostDetailAdapter

    // Receive postId via Safe Args
    private val args: PostDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPostDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize summary cache with context
        viewModel.initSummaryCache(requireContext())

        val postId = args.postId
        if (postId.isEmpty()) {
            Toast.makeText(requireContext(), "Missing postId", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
            return
        }

        setupTopAppBar()
        setupSwipeRefresh()
        setupBottomInputBar()
        setupRecyclerView()
        observeViewModel()

        viewModel.loadPostDetail(postId)
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

    /**
     * Sets up the bottom input bar for composing new comments.
     */
    private fun setupBottomInputBar() {
        binding.bottomInputBar.apply {
            onSendClick = { text ->
                viewModel.sendComment(text)
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            val postId = args.postId
            viewModel.loadPostDetail(postId)
        }
    }

    /**
     * Sets up the RecyclerView with PostDetailAdapter to display post and comments.
     * Handles post voting and comment interactions.
     */
    private fun setupRecyclerView() {
        detailAdapter = PostDetailAdapter(
            onPostAction = { post, action, view ->
                when (action) {
                    PostAction.OPEN -> {
                        // No action needed - already on detail page
                    }
                    PostAction.UPVOTE -> {
                        viewModel.handleVote(post, true)
                    }
                    PostAction.DOWNVOTE -> {
                        viewModel.handleVote(post, false)
                    }
                    PostAction.REPLY -> {
                        viewModel.handleReply(post)
                    }
                    PostAction.SHARE -> {
                        val shareDialog = SharePostDialog.newInstance(post.id)
                        shareDialog.show(childFragmentManager, "SharePostDialog")
                    }
                    PostAction.SUMMARY -> {
                        viewModel.requestSummary()
                    }
                        PostAction.AUTHOR_PROFILE -> {
                            val action = PostDetailFragmentDirections.actionGlobalProfileFragment(post.authorId)
                            navController.navigate(action)
                        }
                    PostAction.MENU -> {
                        showPostMenu(post, view)
                    }
                }
            },
            onCommentAction = { comment, action ->
                when (action) {
                    CommentAction.OPEN -> {
                        // Toggle expand/collapse for nested replies
                        viewModel.handleOpen(comment)
                    }
                    CommentAction.UPVOTE -> {
                        viewModel.handleVote(comment, true)
                    }
                    CommentAction.DOWNVOTE -> {
                        viewModel.handleVote(comment, false)
                    }
                    CommentAction.REPLY -> {
                        viewModel.handleReply(comment)
                    }

                    CommentAction.AUTHOR_PROFILE -> {
                        val action = PostDetailFragmentDirections.actionGlobalProfileFragment(comment.authorId)
                        navController.navigate(action)
                    }

                    CommentAction.REPLIED_USER_PROFILE -> {
                        comment.replyToUserId?.let {
                            val action = PostDetailFragmentDirections.actionGlobalProfileFragment(it)
                            navController.navigate(action)
                        }
                    }

                    CommentAction.VIEW_REPLIES -> {
                        // Toggle expand/collapse for comment's nested replies
                        viewModel.toggleReplies(comment)
                    }
                }
            }
        )

        binding.postRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = detailAdapter
        }
    }

    /**
     * Observe all ViewModel LiveData streams and update UI accordingly.
     */
    private fun observeViewModel() {
        mainSharedViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            binding.topAppBar.setProfileImage(user?.profilePictureUrl)
        }

        viewModel.items.observe(viewLifecycleOwner) { items ->
            // Preserve scroll position during updates
            val layoutManager = binding.postRecyclerView.layoutManager as? LinearLayoutManager
            val firstVisiblePosition = layoutManager?.findFirstCompletelyVisibleItemPosition() ?: 0
            val scrollOffset = layoutManager?.findViewByPosition(firstVisiblePosition)?.top ?: 0
            
            detailAdapter.submitList(items)
            
            // Restore scroll position if it changed
            if (firstVisiblePosition > 0) {
                binding.postRecyclerView.post {
                    layoutManager?.scrollToPositionWithOffset(firstVisiblePosition, scrollOffset)
                }
            }
        }

        viewModel.topics.observe(viewLifecycleOwner) { topics ->
            // Only update if topics list is not empty to prevent redundant calls
            if (topics.isNotEmpty()) {
                detailAdapter.setTopics(topics)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
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

        // Open keyboard and focus input bar when user replies to post or comment
        viewModel.openReplyInput.observe(viewLifecycleOwner) { open ->
            if (open == true) {
                binding.bottomInputBar.focusAndShowKeyboard()
            }
        }

        // Update input bar hint based on reply target (post or specific comment)
        viewModel.replyTargetComment.observe(viewLifecycleOwner) { target ->
            val hint = when {
                target == null -> "Write a comment..."
                else -> "Reply to ${target.authorName}"
            }
            binding.bottomInputBar.setHint(hint)
        }

        // Observe AI summary loading state for button UI
        viewModel.isSummaryLoading.observe(viewLifecycleOwner) { isLoading ->
            detailAdapter.setSummaryLoading(isLoading)
        }

        // Observe AI summary result to show dialog or error
        viewModel.summaryResult.observe(viewLifecycleOwner) { result ->
            result?.let { summaryResult ->
                summaryResult.onSuccess { summary ->
                    showSummaryDialog(summary)
                }.onFailure { error ->
                    showSummaryError(error.message ?: "Failed to generate summary")
                }
                viewModel.clearSummaryResult()
            }
        }
    }

    /**
     * Displays a bottom sheet dialog with the AI-generated summary.
     *
     * @param summary The summary text to display
     */
    private fun showSummaryDialog(summary: String) {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_post_summary, null)

        view.findViewById<TextView>(R.id.tvSummaryContent).text = summary
        view.findViewById<ImageButton>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    /**
     * Displays an error toast when summary generation fails.
     *
     * @param message The error message to display
     */
    private fun showSummaryError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
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
