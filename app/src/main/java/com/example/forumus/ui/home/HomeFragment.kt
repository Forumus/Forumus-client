package com.example.forumus.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.forumus.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter()
        binding.rvPosts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = postAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnMenu.setOnClickListener {
            toggleUserMenu()
        }
        
        binding.btnProfile.setOnClickListener {
            toggleUserMenu()
        }
        
        binding.btnSearch.setOnClickListener {
            // Navigate to search
        }

        // User Menu Options
        binding.menuViewProfile.setOnClickListener {
            // Navigate to profile
            binding.userMenuContainer.visibility = View.GONE
        }

        binding.menuEditProfile.setOnClickListener {
            // Navigate to edit profile
            binding.userMenuContainer.visibility = View.GONE
        }

        binding.menuDarkMode.setOnClickListener {
            // Toggle dark mode
            binding.userMenuContainer.visibility = View.GONE
        }

        binding.menuSettings.setOnClickListener {
            // Navigate to settings
            binding.userMenuContainer.visibility = View.GONE
        }

        // Bottom Navigation
        binding.navHome.setOnClickListener {
            // Home already selected
        }

        binding.navExplore.setOnClickListener {
            // Navigate to explore
        }

        binding.btnCreatePost.setOnClickListener {
            // Navigate to create post
        }

        binding.navAlerts.setOnClickListener {
            // Navigate to alerts
        }

        binding.navChat.setOnClickListener {
            // Navigate to chat
        }

        // Close menu when clicking outside
        binding.root.setOnClickListener { view ->
            if (binding.userMenuContainer.visibility == View.VISIBLE) {
                val menuBounds = android.graphics.Rect()
                binding.userMenuContainer.getGlobalVisibleRect(menuBounds)
                val x = view.x.toInt()
                val y = view.y.toInt()
                if (!menuBounds.contains(x, y)) {
                    binding.userMenuContainer.visibility = View.GONE
                }
            }
        }
    }

    private fun toggleUserMenu() {
        binding.userMenuContainer.visibility = when (binding.userMenuContainer.visibility) {
            View.VISIBLE -> View.GONE
            else -> View.VISIBLE
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.postsFlow.collect { posts ->
                postAdapter.submitList(posts)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loadingState.collect { isLoading ->
                // Update UI based on loading state
                binding.rvPosts.alpha = if (isLoading) 0.5f else 1.0f
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.errorState.collect { error ->
                error?.let {
                    // Show error message
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
