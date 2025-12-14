package com.hcmus.forumus_client.ui.home

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
import com.hcmus.forumus_client.databinding.FragmentHomeBinding
import com.hcmus.forumus_client.ui.common.PopupPostMenu

/**
 * Home Fragment displaying a feed of posts with voting and interaction features.
 * Uses NavController for navigation and Safe Args for passing data.
 * Shares MainSharedViewModel for current user data.
 */
class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var homeAdapter: HomeAdapter
    private val viewModel: HomeViewModel by viewModels()
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

        setupSwipeRefresh()
        setupRecyclerView()
        observeViewModel()

        viewModel.loadPosts()
        //viewModel.addFieldForPosts()
    }

    /**
     * Sets up the RecyclerView with HomeAdapter to display posts.
     * Configures post action callbacks for upvote, downvote, and navigation.
     */
    private fun setupRecyclerView() {
        homeAdapter = HomeAdapter(emptyList()) { post, action, view ->
            when (action) {
                PostAction.OPEN -> {
                    // Navigate to PostDetailFragment using Safe Args
                    val action = HomeFragmentDirections.actionGlobalPostDetailFragment(post.id)
                    navController.navigate(action)
                }
                PostAction.UPVOTE -> viewModel.onPostAction(post, PostAction.UPVOTE)
                PostAction.DOWNVOTE -> viewModel.onPostAction(post, PostAction.DOWNVOTE)
                PostAction.REPLY -> {
                    // Navigate to PostDetailFragment using Safe Args
                    val action = HomeFragmentDirections.actionGlobalPostDetailFragment(post.id)
                    navController.navigate(action)
                }
                PostAction.SHARE -> Toast.makeText(requireContext(), "Share feature coming soon", Toast.LENGTH_SHORT).show()
                PostAction.AUTHOR_PROFILE -> {
                    // Navigate to ProfileFragment using Safe Args
                    val action = HomeFragmentDirections
                        .actionGlobalProfileFragment(post.authorId)
                    navController.navigate(action)
                }
                PostAction.MENU -> showPostMenu(post, view)
            }
        }

        binding.postRecyclerView.apply {
            adapter = homeAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadPosts()
        }
    }

    /**
     * Observe all ViewModel LiveData streams and update UI accordingly.
     */
    private fun observeViewModel() {
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            homeAdapter.submitList(posts)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.swipeRefresh.isRefreshing = loading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrBlank()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Display the post action menu popup when user taps the menu icon on a post.
     * Allows users to save or report the post.
     */
    private fun showPostMenu(post: Post, menuButton: View) {
        val popupMenu = PopupPostMenu(requireActivity() as androidx.appcompat.app.AppCompatActivity)

        // Handle save button click
        popupMenu.onSaveClick = {
            Toast.makeText(requireContext(), "Post saved", Toast.LENGTH_SHORT).show()
        }

        // Handle violation selection from report menu
        popupMenu.onReportClick = { violation ->
            viewModel.saveReport(post, violation)
        }

        // Show popup at menu button
        popupMenu.show(menuButton)
    }
}
