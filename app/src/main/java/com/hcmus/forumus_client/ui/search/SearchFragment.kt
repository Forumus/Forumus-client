package com.hcmus.forumus_client.ui.search

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private lateinit var historyAdapter: HistoryAdapter

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
        viewModel.loadTrendingTopics()
    }

    private fun setupUI() {
        setupBottomNavigation()

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { performSearch(it) }
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) showSuggestions(true)
                return false
            }
        })

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                updateLayoutForTab(tab?.position ?: 0)
                val query = binding.searchView.query.toString()
                if (query.isNotEmpty()) performSearch(query)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        updateLayoutForTab(0)
    }

    private fun updateLayoutForTab(position: Int) {
        val isPostTab = position == 0
        if (isPostTab) {
            binding.searchView.queryHint = getString(com.hcmus.forumus_client.R.string.search_hint_posts)
        } else {
            binding.searchView.queryHint = getString(com.hcmus.forumus_client.R.string.search_hint_people)
        }
        binding.sectionTrending.visibility = if (isPostTab) View.VISIBLE else View.GONE

        if (isPostTab) {
            val list = viewModel.recentPostKeywords.value ?: emptyList()
            historyAdapter.submitList(list)
            checkHistoryVisibility(list)
        } else {
            val list = viewModel.recentPeopleKeywords.value ?: emptyList()
            historyAdapter.submitList(list)
            checkHistoryVisibility(list)
        }
    }

    private fun checkHistoryVisibility(list: List<String>) {
        val hasData = list.isNotEmpty()
        binding.tvNoRecent.visibility = if (hasData) View.GONE else View.VISIBLE
        binding.rvRecentList.visibility = if (hasData) View.VISIBLE else View.GONE
    }

    private fun setupAdapters() {
        postsAdapter = HomeAdapter(emptyList()) { post, action, _ ->
            if (action == PostAction.OPEN) {
                val navAction = NavGraphDirections.actionGlobalPostDetailFragment(post.id)
                navController.navigate(navAction)
            }
        }
        binding.rvPostsResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPostsResults.adapter = postsAdapter

        userAdapter = UserAdapter(emptyList()) { user ->
            val navAction = NavGraphDirections.actionGlobalProfileFragment(user.uid)
            navController.navigate(navAction)
        }
        binding.rvUsersResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUsersResults.adapter = userAdapter

        historyAdapter = HistoryAdapter { keyword ->
            binding.searchView.setQuery(keyword, true)
        }
        binding.rvRecentList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentList.adapter = historyAdapter
    }

    private fun performSearch(query: String) {
        showSuggestions(false)
        if (binding.tabLayout.selectedTabPosition == 0) viewModel.searchPosts(query)
        else viewModel.searchUsers(query)
    }

    private fun showSuggestions(show: Boolean) {
        binding.layoutSuggestions.visibility = if (show) View.VISIBLE else View.GONE
        binding.layoutResults.visibility = if (show) View.GONE else View.VISIBLE
        if (show) {
            updateLayoutForTab(binding.tabLayout.selectedTabPosition)
        }
    }

    private fun setupObservers() {
        // [MỚI] Quan sát danh sách Topic và đưa vào Adapter để hiển thị tên đẹp
        viewModel.allTopics.observe(viewLifecycleOwner) { topics ->
            postsAdapter.setTopics(topics)
        }

        viewModel.trendingTopics.observe(viewLifecycleOwner) { topics ->
            binding.chipGroupTrending.removeAllViews()
            for (topic in topics) {
                val chip = Chip(requireContext())
                chip.text = "#${topic.name}"
                try {
                    val baseColor = Color.parseColor(topic.fillColor)
                    val alphaInt = (topic.fillAlpha * 255).toInt().coerceIn(0, 255)
                    chip.chipBackgroundColor = ColorStateList.valueOf(Color.argb(alphaInt, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor)))
                    chip.setTextColor(baseColor)
                } catch (e: Exception) {
                    chip.chipBackgroundColor = ColorStateList.valueOf(Color.LTGRAY)
                }
                chip.setOnClickListener { binding.searchView.setQuery(topic.name, true) }
                binding.chipGroupTrending.addView(chip)
            }
        }

        viewModel.postResults.observe(viewLifecycleOwner) { posts ->
            if (binding.tabLayout.selectedTabPosition == 0) {
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

        // Observer lịch sử (giữ nguyên)
        viewModel.recentPostKeywords.observe(viewLifecycleOwner) { list ->
            if (binding.tabLayout.selectedTabPosition == 0) {
                historyAdapter.submitList(list)
                checkHistoryVisibility(list)
            }
        }
        viewModel.recentPeopleKeywords.observe(viewLifecycleOwner) { list ->
            if (binding.tabLayout.selectedTabPosition == 1) {
                historyAdapter.submitList(list)
                checkHistoryVisibility(list)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE
        }
    }

    class HistoryAdapter(
        private var keywords: List<String> = emptyList(),
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
        fun submitList(newList: List<String>) {
            keywords = newList
            notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
            return ViewHolder(view)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val keyword = keywords[position]
            holder.textView.text = keyword
            holder.textView.setCompoundDrawablesWithIntrinsicBounds(com.hcmus.forumus_client.R.drawable.ic_search, 0, 0, 0)
            holder.textView.compoundDrawablePadding = 24
            holder.itemView.setOnClickListener { onClick(keyword) }
        }
        override fun getItemCount() = keywords.size
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textView: TextView = view.findViewById(android.R.id.text1)
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomBar.apply {
            setActiveTab(com.hcmus.forumus_client.ui.common.BottomNavigationBar.Tab.EXPLORE)
            onHomeClick = { navController.navigate(com.hcmus.forumus_client.R.id.homeFragment) }
            onExploreClick = { /* Already on Explore */ }
            onCreatePostClick = { navController.navigate(com.hcmus.forumus_client.R.id.createPostFragment) }
            onAlertsClick = { navController.navigate(NavGraphDirections.actionGlobalNotificationFragment()) }
            onChatClick = { navController.navigate(com.hcmus.forumus_client.R.id.chatsFragment) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}