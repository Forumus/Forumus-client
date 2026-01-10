package com.hcmus.forumus_client.ui.settings.savedposts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.model.PostAction
import com.hcmus.forumus_client.databinding.FragmentSavedPostsBinding
import com.hcmus.forumus_client.ui.common.PopupPostMenu
import com.hcmus.forumus_client.ui.home.HomeFragmentDirections

/**
 * Fragment displaying all posts saved by the user.
 * Follows MVVM architecture with Data Binding.
 * Users can view saved posts, navigate to post details, and unsave posts.
 */
class SavedPostsFragment : Fragment() {

    private lateinit var binding: FragmentSavedPostsBinding
    private lateinit var adapter: SavedPostsAdapter
    private val viewModel: SavedPostsViewModel by viewModels()
    private val navController by lazy { findNavController() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSavedPostsBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeader()
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()

        // Load saved posts
        viewModel.loadSavedPosts()
    }

    /**
     * Setup header with back button navigation.
     */
    private fun setupHeader() {
        binding.ibBack.setOnClickListener {
            navController.popBackStack()
        }
    }

    /**
     * Setup RecyclerView with adapter to display saved posts.
     */
    private fun setupRecyclerView() {
        adapter = SavedPostsAdapter(
            items = emptyList(),
            onActionClick = { post, action, view -> handlePostAction(post, action, view) }
        )

        binding.rvSavedPosts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SavedPostsFragment.adapter
        }
    }

    /**
     * Setup SwipeRefreshLayout for pull-to-refresh functionality.
     */
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadSavedPosts()
        }
    }

    /**
     * Observe ViewModel LiveData for UI updates.
     */
    private fun observeViewModel() {
        // Observe saved posts list
        viewModel.savedPosts.observe(viewLifecycleOwner) { posts ->
            adapter.submitList(posts)
            
            // Show/hide empty state
            if (posts.isEmpty()) {
                binding.llEmptyState.visibility = View.VISIBLE
                binding.swipeRefresh.visibility = View.GONE
            } else {
                binding.llEmptyState.visibility = View.GONE
                binding.swipeRefresh.visibility = View.VISIBLE
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
        }

        // Observe errors
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Handle post action clicks (upvote, downvote, reply, menu).
     *
     * @param post The post that was interacted with
     * @param action The action performed
     * @param view The view that was clicked
     */
    private fun handlePostAction(post: Post, action: PostAction, view: View) {
        when (action) {
            PostAction.UPVOTE -> {
                // Voting not implemented in saved posts - could be added later
                Toast.makeText(requireContext(), "Voting coming soon", Toast.LENGTH_SHORT).show()
            }
            PostAction.DOWNVOTE -> {
                // Voting not implemented in saved posts - could be added later
                Toast.makeText(requireContext(), "Voting coming soon", Toast.LENGTH_SHORT).show()
            }
            PostAction.REPLY -> {
                // Navigate to post detail
                val action = HomeFragmentDirections.actionGlobalPostDetailFragment(post.id)
                navController.navigate(action)
            }
            PostAction.SHARE -> {
                // Navigate to post detail
                val action = HomeFragmentDirections.actionGlobalPostDetailFragment(post.id)
                navController.navigate(action)
            }
            PostAction.MENU -> {
                showPostMenu(post, view)
            }
            PostAction.OPEN -> {
                // Navigate to post detail
                val action = HomeFragmentDirections.actionGlobalPostDetailFragment(post.id)
                navController.navigate(action)
            }
            PostAction.AUTHOR_PROFILE ->{
                // Navigate to post detail
                val action = HomeFragmentDirections.actionGlobalPostDetailFragment(post.id)
                navController.navigate(action)
            }
        }
    }

    /**
     * Display the post action menu popup when user taps the menu icon on a post.
     * Allows users to unsave the post and report violations.
     *
     * @param post The post to show menu for
     * @param menuButton The menu button view
     */
    private fun showPostMenu(post: Post, menuButton: View) {
        val popupMenu = PopupPostMenu(requireActivity() as androidx.appcompat.app.AppCompatActivity)

        // Set button text to "Unsave" since posts are already saved on this screen
        popupMenu.saveButtonText = "Unsave"

        // Handle unsave button click
        popupMenu.onSaveClick = {
            viewModel.unsavePost(post)
            Toast.makeText(requireContext(), "Post removed from saved", Toast.LENGTH_SHORT).show()
        }

        // Handle report click
        popupMenu.onReportClick = { violation ->
            viewModel.saveReport(post, violation)
            Toast.makeText(requireContext(), "Post reported", Toast.LENGTH_SHORT).show()
        }

        // Show popup at menu button
        popupMenu.show(menuButton)
    }
}
