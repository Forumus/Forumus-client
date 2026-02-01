package com.hcmus.forumus_client.ui.settings.savedposts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.model.PostAction
import com.hcmus.forumus_client.databinding.FragmentSavedPostsBinding
import com.hcmus.forumus_client.ui.common.PopupPostMenu
import com.hcmus.forumus_client.ui.home.HomeFragmentDirections
import kotlinx.coroutines.launch

/** Displays posts saved by the user. */
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

    private fun setupHeader() {
        binding.ibBack.setOnClickListener {
            navController.popBackStack()
        }
    }

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

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadSavedPosts()
        }
    }

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

    private fun handlePostAction(post: Post, action: PostAction, view: View) {
        when (action) {
            PostAction.UPVOTE -> {
                viewModel.onPostAction(post, PostAction.UPVOTE)
            }
            PostAction.DOWNVOTE -> {
                viewModel.onPostAction(post, PostAction.DOWNVOTE)
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
                // Navigate to ProfileFragment using Safe Args
                val action =
                    HomeFragmentDirections.actionGlobalProfileFragment(
                        post.authorId
                    )
                navController.navigate(action)
            }
            PostAction.SUMMARY -> {
                // Navigate to post detail for summary view
                val action = HomeFragmentDirections.actionGlobalPostDetailFragment(post.id)
                navController.navigate(action)
            }
        }
    }

    // Shows post menu with unsave and report options
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
