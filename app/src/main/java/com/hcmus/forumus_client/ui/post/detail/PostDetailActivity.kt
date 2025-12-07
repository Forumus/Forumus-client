package com.hcmus.forumus_client.ui.post.detail

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.hcmus.forumus_client.databinding.ActivityPostDetailBinding
import com.hcmus.forumus_client.ui.navigation.AppNavigator
import androidx.core.view.ViewCompat
import com.hcmus.forumus_client.ui.common.ProfileMenuAction
import com.hcmus.forumus_client.data.model.PostAction
import com.hcmus.forumus_client.data.model.CommentAction
import androidx.core.view.WindowInsetsCompat

/**
 * Activity for displaying post details and managing nested comment discussions.
 *
 * This activity shows:
 * - The post content with voting capabilities
 * - All comments and nested replies organized in a hierarchical structure
 * - Comment expansion/collapse for managing nested reply visibility
 * - Input bar for composing new comments or replies to existing comments
 *
 * The activity uses PostDetailViewModel to manage post/comment state and
 * PostDetailAdapter to render items in a RecyclerView.
 */
class PostDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_POST_ID = "extra_post_id"
    }

    private lateinit var binding: ActivityPostDetailBinding
    private val viewModel: PostDetailViewModel by viewModels()
    private val navigator by lazy { AppNavigator(this) }
    private lateinit var detailAdapter: PostDetailAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle system window insets for proper padding with system UI
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Extract post ID from intent (required parameter)
        val postId = intent.getStringExtra(EXTRA_POST_ID) ?: run {
            Toast.makeText(this, "Missing postId", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Setup UI components and load data
        setupWindowInsetsHandling()
        setupTopAppBar()
        setupBottomInputBar()
        setupRecyclerView()

        viewModel.loadCurrentUser()
        observeViewModel()

        viewModel.loadPostDetail(postId)
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
     * Initializes the top app bar with menu and navigation callbacks.
     *
     * Handles:
     * - Menu click events
     * - Home navigation
     * - Search navigation
     * - Profile menu actions (view profile, edit profile, dark mode, settings)
     */
    private fun setupTopAppBar() {
        binding.topAppBar.apply {
            onMenuClick = { Toast.makeText(this@PostDetailActivity, "Menu clicked", Toast.LENGTH_SHORT).show() }
            onHomeClick = { navigator.openHome() }
            onSearchClick = { navigator.openSearch() }
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
                        // TODO: Implement theme toggle functionality
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
     * Sets up the bottom input bar for composing comments and replies.
     *
     * Handles send button click events and passes the text to the view model.
     */
    private fun setupBottomInputBar() {
        binding.bottomInputBar.apply {
            onSendClick = { text ->
                viewModel.sendComment(text)
            }
        }
    }

    /**
     * Initializes the RecyclerView with PostDetailAdapter and configures action callbacks.
     *
     * Post action handlers:
     * - OPEN: No action (detail page already shows full post)
     * - UPVOTE/DOWNVOTE: Vote on the post
     * - REPLY: Open reply input for this post
     * - SHARE: Share the post
     * - AUTHOR_PROFILE: Navigate to author's profile
     *
     * Comment action handlers:
     * - OPEN: Expand/collapse nested replies
     * - UPVOTE/DOWNVOTE: Vote on the comment
     * - REPLY: Open reply input for this comment
     * - AUTHOR_PROFILE: Navigate to comment author's profile
     * - REPLIED_USER_PROFILE: Navigate to the user being replied to
     */
    private fun setupRecyclerView() {
        detailAdapter = PostDetailAdapter(
            onPostAction = { post, action ->
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
                        // TODO: Implement post sharing functionality
                    }
                    PostAction.AUTHOR_PROFILE -> {
                        navigator.openProfile(post.authorId)
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
                        navigator.openProfile(comment.authorId)
                    }
                    CommentAction.REPLIED_USER_PROFILE -> {
                        comment.replyToUserId?.let {
                            navigator.openProfile(it)
                        }
                    }
                }
            }
        )

        binding.postRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@PostDetailActivity)
            adapter = detailAdapter
        }
    }

    /**
     * Observes LiveData from the view model and updates UI accordingly.
     *
     * Observes:
     * - currentUser: Updates top app bar with user's avatar
     * - items: Updates RecyclerView adapter with posts and comments
     * - openReplyInput: Opens keyboard and focuses input bar when replying
     * - replyTargetComment: Updates input bar hint based on reply target
     */
    private fun observeViewModel() {
        // Update top app bar with current user's avatar
        viewModel.currentUser.observe(this) { user ->
            binding.topAppBar.setProfileImage(user?.profilePictureUrl)
        }

        // Update RecyclerView with posts and comments
        viewModel.items.observe(this) { items ->
            detailAdapter.submitList(items)
        }

        // Open keyboard and focus input bar when user replies to post or comment
        viewModel.openReplyInput.observe(this) { open ->
            if (open == true) {
                binding.bottomInputBar.focusAndShowKeyboard()
            }
        }

        // Update input bar hint based on reply target (post or specific comment)
        viewModel.replyTargetComment.observe(this) { target ->
            val hint = when {
                target == null -> "Write a comment..."
                else -> "Reply to ${target.authorName}"
            }
            binding.bottomInputBar.setHint(hint)
        }
    }
}