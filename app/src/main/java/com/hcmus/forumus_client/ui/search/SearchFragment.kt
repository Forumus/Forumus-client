package com.hcmus.forumus_client.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout
import com.hcmus.forumus_client.NavGraphDirections
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.data.model.PostAction
import com.hcmus.forumus_client.databinding.FragmentSearchBinding
import com.hcmus.forumus_client.ui.home.HomeAdapter

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SearchViewModel by viewModels()

    private lateinit var postsAdapter: HomeAdapter
    private lateinit var userAdapter: UserAdapter
    private val navController by lazy { findNavController() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupAdapters()
        setupObservers()

        // Load Trending khi mở màn hình
        viewModel.loadTrendingTopics()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { navController.popBackStack() }

        // Setup Search View
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { performSearch(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    showSuggestions(true)
                }
                return false
            }
        })

        // Setup Tabs
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                updateResultsVisibility()
                // Nếu đang có text search thì search lại theo tab mới
                val query = binding.searchView.query.toString()
                if (query.isNotEmpty()) performSearch(query)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupAdapters() {
        // 1. Adapter cho Posts (Dùng lại HomeAdapter)
        postsAdapter = HomeAdapter(emptyList()) { post, action, view ->
            when (action) {
                PostAction.OPEN -> {
                    val navAction = NavGraphDirections
                        .actionGlobalPostDetailFragment(post.id)
                    navController.navigate(navAction)
                }
                PostAction.AUTHOR_PROFILE -> {
                    val navAction = NavGraphDirections
                        .actionGlobalProfileFragment(post.authorId)
                    navController.navigate(navAction)
                }
                else -> {}
            }
        }
        binding.rvPostsResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPostsResults.adapter = postsAdapter

        // 2. Adapter cho Users
        userAdapter = UserAdapter(emptyList()) { user ->
            val navAction = NavGraphDirections
                .actionGlobalPostDetailFragment(user.uid)
            navController.navigate(navAction)
        }
        binding.rvUsersResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUsersResults.adapter = userAdapter
    }

    private fun performSearch(query: String) {
        showSuggestions(false)
        if (binding.tabLayout.selectedTabPosition == 0) {
            viewModel.searchPosts(query)
        } else {
            viewModel.searchUsers(query)
        }
    }

    private fun updateResultsVisibility() {
        val isPostsTab = binding.tabLayout.selectedTabPosition == 0
        binding.rvPostsResults.visibility = if (isPostsTab) View.VISIBLE else View.GONE
        binding.rvUsersResults.visibility = if (isPostsTab) View.GONE else View.VISIBLE
    }

    private fun showSuggestions(show: Boolean) {
        binding.layoutSuggestions.visibility = if (show) View.VISIBLE else View.GONE
        binding.layoutResults.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun setupObservers() {
        // Trending Topics
        viewModel.trendingTopics.observe(viewLifecycleOwner) { topics ->
            binding.chipGroupTrending.removeAllViews()
            for (topic in topics) {
                val chip = Chip(requireContext())
                chip.text = "#$topic"
                chip.setOnClickListener {
                    binding.searchView.setQuery(topic, true)
                }
                binding.chipGroupTrending.addView(chip)
            }
        }

        // Search Results
        viewModel.postResults.observe(viewLifecycleOwner) { posts ->
            if (binding.tabLayout.selectedTabPosition == 0) {
                postsAdapter.submitList(posts)
                binding.tvNoResults.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewModel.userResults.observe(viewLifecycleOwner) { users ->
            if (binding.tabLayout.selectedTabPosition == 1) {
                userAdapter.submitList(users)
                binding.tvNoResults.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}