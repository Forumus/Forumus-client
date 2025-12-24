package com.hcmus.forumus_client.ui.search

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout
import com.hcmus.forumus_client.NavGraphDirections
import com.hcmus.forumus_client.data.model.PostAction
import com.hcmus.forumus_client.databinding.FragmentSearchBinding
import com.hcmus.forumus_client.ui.home.HomeAdapter

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()

    private lateinit var postsAdapter: HomeAdapter
    private lateinit var userAdapter: UserAdapter
    private lateinit var recentPostsAdapter: HomeAdapter // Adapter cho history post
    private lateinit var recentUsersAdapter: UserAdapter // Adapter cho history user

    private val navController by lazy { findNavController() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        setupAdapters()
        setupUI()
        setupObservers()

        // Load Trending ngay khi vào
        viewModel.loadTrendingTopics()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { navController.popBackStack() }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { performSearch(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    showSuggestions(true) // Hiện lại Trending/Recent khi xóa text
                }
                return false
            }
        })

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                updateLayoutForTab(tab?.position ?: 0)

                // Nếu đang có text search thì search lại theo tab mới
                val query = binding.searchView.query.toString()
                if (query.isNotEmpty()) performSearch(query)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Mặc định setup cho Tab 0
        updateLayoutForTab(0)
    }

    private fun updateLayoutForTab(position: Int) {
        if (position == 0) {
            // TAB POSTS: Hiện Trending
            binding.sectionTrending.visibility = View.VISIBLE
            // Chuyển adapter của Recent List sang Post History
            binding.rvRecentList.adapter = recentPostsAdapter
        } else {
            // TAB PEOPLE: Ẩn Trending
            binding.sectionTrending.visibility = View.GONE
            // Chuyển adapter của Recent List sang User History
            binding.rvRecentList.adapter = recentUsersAdapter
        }
    }

    private fun setupAdapters() {
        // 1. Adapter Kết quả Posts
        postsAdapter = HomeAdapter(emptyList()) { post, action, _ ->
            if (action == PostAction.OPEN) {
                viewModel.addToRecentPosts(post)
                val navAction = NavGraphDirections.actionGlobalPostDetailFragment(post.id)
                navController.navigate(navAction)
            }
        }
        binding.rvPostsResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPostsResults.adapter = postsAdapter

        // 2. Adapter Kết quả Users
        userAdapter = UserAdapter(emptyList()) { user ->
            viewModel.addToRecentUsers(user)
            val navAction = NavGraphDirections.actionGlobalProfileFragment(user.uid)
            navController.navigate(navAction)
        }
        binding.rvUsersResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUsersResults.adapter = userAdapter

        // 3. Adapter Lịch sử Posts (Click vào là mở)
        recentPostsAdapter = HomeAdapter(emptyList()) { post, action, _ ->
            if (action == PostAction.OPEN) {
                val navAction = NavGraphDirections.actionGlobalPostDetailFragment(post.id)
                navController.navigate(navAction)
            }
        }

        // 4. Adapter Lịch sử Users
        recentUsersAdapter = UserAdapter(emptyList()) { user ->
            val navAction = NavGraphDirections.actionGlobalProfileFragment(user.uid)
            navController.navigate(navAction)
        }

        binding.rvRecentList.layoutManager = LinearLayoutManager(requireContext())
        // Mặc định gán adapter post
        binding.rvRecentList.adapter = recentPostsAdapter
    }

    private fun performSearch(query: String) {
        showSuggestions(false)
        if (binding.tabLayout.selectedTabPosition == 0) {
            viewModel.searchPosts(query)
        } else {
            viewModel.searchUsers(query)
        }
    }

    private fun showSuggestions(show: Boolean) {
        binding.layoutSuggestions.visibility = if (show) View.VISIBLE else View.GONE
        binding.layoutResults.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun setupObservers() {
        // 1. Trending Topics (Color + Alpha)
        viewModel.trendingTopics.observe(viewLifecycleOwner) { topics ->
            binding.chipGroupTrending.removeAllViews()
            for (topic in topics) {
                val chip = Chip(requireContext())
                chip.text = "#${topic.name}"
                chip.isCheckable = false

                try {
                    val baseColor = Color.parseColor(topic.fillColor)
                    // Tính alpha (0.0 -> 1.0) thành (0 -> 255)
                    val alphaInt = (topic.fillAlpha * 255).toInt().coerceIn(0, 255)
                    val bgWithAlpha = Color.argb(alphaInt, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))

                    chip.chipBackgroundColor = ColorStateList.valueOf(bgWithAlpha)
                    chip.setTextColor(baseColor)
                } catch (e: Exception) {
                    // Fallback nếu màu lỗi
                    chip.chipBackgroundColor = ColorStateList.valueOf(Color.LTGRAY)
                }

                chip.setOnClickListener {
                    binding.searchView.setQuery(topic.name, true)
                }
                binding.chipGroupTrending.addView(chip)
            }
        }

        // 2. Search Results
        viewModel.postResults.observe(viewLifecycleOwner) { posts ->
            if (binding.tabLayout.selectedTabPosition == 0) {
                binding.rvPostsResults.visibility = View.VISIBLE
                binding.rvUsersResults.visibility = View.GONE
                postsAdapter.submitList(posts)
                binding.tvNoResults.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewModel.userResults.observe(viewLifecycleOwner) { users ->
            if (binding.tabLayout.selectedTabPosition == 1) {
                binding.rvPostsResults.visibility = View.GONE
                binding.rvUsersResults.visibility = View.VISIBLE
                userAdapter.submitList(users)
                binding.tvNoResults.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        // 3. Recent History
        viewModel.recentPosts.observe(viewLifecycleOwner) { posts ->
            if (binding.tabLayout.selectedTabPosition == 0) {
                recentPostsAdapter.submitList(posts)
                binding.tvNoRecent.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewModel.recentUsers.observe(viewLifecycleOwner) { users ->
            if (binding.tabLayout.selectedTabPosition == 1) {
                recentUsersAdapter.submitList(users)
                binding.tvNoRecent.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
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