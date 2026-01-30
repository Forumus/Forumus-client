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
 * Displays a single post with all its comments and allows commenting.
 */
class PostDetailFragment : Fragment() {

    private lateinit var binding: FragmentPostDetailBinding
    private val viewModel: PostDetailViewModel by viewModels()
    private val mainSharedViewModel: MainSharedViewModel by activityViewModels()
    private val navController by lazy { findNavController() }
    private lateinit var detailAdapter: PostDetailAdapter

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
                        navController.navigate(R.id.createPostFragment)
                    }
                }
            }

            setIconFuncButton(R.drawable.ic_back)
        }
    }

    private fun setupBottomInputBar() {
        binding.bottomInputBar.apply {
            onSendClick = { text ->
                viewModel.sendComment(text)
            }
            onCancelReply = {
                viewModel.cancelReply()
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            val postId = args.postId
            viewModel.loadPostDetail(postId)
        }
    }

    private fun setupRecyclerView() {
        detailAdapter = PostDetailAdapter(
            onPostAction = { post, action, view ->
                when (action) {
                    PostAction.OPEN -> { }
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

    private fun observeViewModel() {
        mainSharedViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            binding.topAppBar.setProfileImage(user?.profilePictureUrl)
        }

        viewModel.items.observe(viewLifecycleOwner) { items ->
            // Preserve scroll position
            val layoutManager = binding.postRecyclerView.layoutManager as? LinearLayoutManager
            val firstVisiblePosition = layoutManager?.findFirstCompletelyVisibleItemPosition() ?: 0
            val scrollOffset = layoutManager?.findViewByPosition(firstVisiblePosition)?.top ?: 0
            
            detailAdapter.submitList(items)
            
            if (firstVisiblePosition > 0) {
                binding.postRecyclerView.post {
                    layoutManager?.scrollToPositionWithOffset(firstVisiblePosition, scrollOffset)
                }
            }
        }

        viewModel.topics.observe(viewLifecycleOwner) { topics ->
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

        viewModel.openReplyInput.observe(viewLifecycleOwner) { open ->
            if (open == true) {
                binding.bottomInputBar.focusAndShowKeyboard()
            }
        }

        viewModel.replyTargetComment.observe(viewLifecycleOwner) { target ->
            if (target != null) {
                binding.bottomInputBar.showReplyBanner(target.authorName)
                binding.bottomInputBar.setHint("Reply to ${target.authorName}")
            } else {
                binding.bottomInputBar.hideReplyBanner()
                binding.bottomInputBar.setHint("Write a comment...")
                binding.bottomInputBar.clearInput()
            }
        }

        viewModel.isSummaryLoading.observe(viewLifecycleOwner) { isLoading ->
            detailAdapter.setSummaryLoading(isLoading)
        }

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

    private fun showSummaryError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showPostMenu(post: Post, menuButton: View) {
        val popupMenu = PopupPostMenu(requireActivity() as androidx.appcompat.app.AppCompatActivity)

        popupMenu.onSaveClick = {
            viewModel.savePost(post)
        }

        popupMenu.onReportClick = { violation -> viewModel.saveReport(post, violation) }

        popupMenu.show(menuButton)
    }
}
