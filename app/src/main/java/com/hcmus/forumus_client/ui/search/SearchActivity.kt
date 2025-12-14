package com.hcmus.forumus_client.ui.search

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.data.model.PostAction
import com.hcmus.forumus_client.databinding.ActivitySearchBinding
import com.hcmus.forumus_client.ui.home.HomeAdapter // Tái sử dụng
import com.hcmus.forumus_client.ui.navigation.AppNavigator

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private val viewModel: SearchViewModel by viewModels()
    private val navigator by lazy { AppNavigator(this) }

    private lateinit var postsAdapter: HomeAdapter
    private lateinit var userAdapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupAdapters()
        setupObservers()

        // Load Trending khi mở màn hình
        viewModel.loadTrendingTopics()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

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
            // Copy logic xử lý click từ HomeActivity qua
            when (action) {
                PostAction.OPEN -> navigator.onDetailPost(post.id)
                PostAction.AUTHOR_PROFILE -> navigator.openProfile(post.authorId)
                // ... Xử lý các action khác nếu cần
                else -> {}
            }
        }
        binding.rvPostsResults.layoutManager = LinearLayoutManager(this)
        binding.rvPostsResults.adapter = postsAdapter

        // 2. Adapter cho Users
        userAdapter = UserAdapter(emptyList()) { user ->
            navigator.openProfile(user.uid) // Giả sử User model có uid
        }
        binding.rvUsersResults.layoutManager = LinearLayoutManager(this)
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
        viewModel.trendingTopics.observe(this) { topics ->
            binding.chipGroupTrending.removeAllViews()
            for (topic in topics) {
                val chip = Chip(this)
                chip.text = "#$topic"
                chip.setOnClickListener {
                    binding.searchView.setQuery(topic, true)
                }
                binding.chipGroupTrending.addView(chip)
            }
        }

        // Search Results
        viewModel.postResults.observe(this) { posts ->
            if (binding.tabLayout.selectedTabPosition == 0) {
                postsAdapter.submitList(posts)
                binding.tvNoResults.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewModel.userResults.observe(this) { users ->
            if (binding.tabLayout.selectedTabPosition == 1) {
                userAdapter.submitList(users)
                binding.tvNoResults.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }
}