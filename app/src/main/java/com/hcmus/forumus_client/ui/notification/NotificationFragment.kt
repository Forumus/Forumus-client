package com.hcmus.forumus_client.ui.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.hcmus.forumus_client.NavGraphDirections
import com.hcmus.forumus_client.databinding.FragmentNotificationBinding
import com.hcmus.forumus_client.ui.common.BottomNavigationBar
import com.hcmus.forumus_client.R

import androidx.fragment.app.activityViewModels

class NotificationFragment : Fragment() {

    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NotificationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()
        if (viewModel.displayItems.value.isNullOrEmpty()) {
            viewModel.loadNotifications()
        }
    }

    private fun setupUI() {
        // Setup BottomNavigationBar
        binding.bottomBar.setActiveTab(BottomNavigationBar.Tab.ALERTS)
        
        binding.bottomBar.onHomeClick = {
            findNavController().navigate(R.id.homeFragment)
        }

        binding.bottomBar.onExploreClick = {
            findNavController().navigate(R.id.searchFragment)
        }
        
        binding.bottomBar.onCreatePostClick = {
             findNavController().navigate(R.id.createPostFragment)
        }

        binding.bottomBar.onChatClick = {
            findNavController().navigate(R.id.chatsFragment)
        }
        
        // Setup RecyclerView
        val adapter = NotificationAdapter(
            onNotificationClick = { notification ->
                viewModel.markAsRead(notification)
                // Navigate to Post Detail
                if (notification.targetId.isNotEmpty()) {
                    val action = NavGraphDirections.actionGlobalPostDetailFragment(notification.targetId)
                    findNavController().navigate(action)
                }
            },
            onShowMoreClick = {
                viewModel.expandEarlierSection()
            }
        )
        binding.rvNotifications.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = adapter
        }

        // Swipe Refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadNotifications()
        }
    }

    private fun observeViewModel() {
        viewModel.displayItems.observe(viewLifecycleOwner) { items ->
            (binding.rvNotifications.adapter as NotificationAdapter).submitList(items)
            binding.swipeRefreshLayout.isRefreshing = false
            
            if (items.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvNotifications.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvNotifications.visibility = View.VISIBLE
            }
        }
        
        viewModel.unreadCount.observe(viewLifecycleOwner) { count ->
             binding.bottomBar.setNotificationBadge(count)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading && viewModel.displayItems.value.isNullOrEmpty()) {
                binding.swipeRefreshLayout.isRefreshing = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
