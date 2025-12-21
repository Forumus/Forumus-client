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
import com.hcmus.forumus_client.ui.common.TopAppBar
import com.hcmus.forumus_client.R

class NotificationFragment : Fragment() {

    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NotificationViewModel by viewModels()

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
        viewModel.loadNotifications()
    }

    private fun setupUI() {
        // Setup TopAppBar
        // binding.topAppBar.setTitle("Notifications") // Not supported by custom view
        // binding.topAppBar.setNavigationOnClickListener { } // Not supported by custom view
        binding.topAppBar.setIconFuncButton(R.drawable.ic_hamburger_button)
        binding.topAppBar.onFuncClick = {
            // Open drawer or do nothing? NotificationFragment is not inside drawer layout usually?
            // If main activity has a drawer, we might need access to it.
            // For now, let's just leave it empty or log.
        }

        // Setup BottomNavigationBar
        binding.bottomBar.setActiveTab(BottomNavigationBar.Tab.ALERTS)
        
        binding.bottomBar.onHomeClick = {
            findNavController().navigate(R.id.homeFragment)
        }
        
        binding.bottomBar.onCreatePostClick = {
             findNavController().navigate(R.id.createPostFragment)
        }

        binding.bottomBar.onChatClick = {
            findNavController().navigate(R.id.chatsFragment)
        }
        
        // Setup RecyclerView
        val adapter = NotificationAdapter { notification ->
            viewModel.markAsRead(notification)
            // Navigate to Post Detail
            if (notification.targetId.isNotEmpty()) {
                val action = NavGraphDirections.actionGlobalPostDetailFragment(notification.targetId)
                findNavController().navigate(action)
            }
        }
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
        viewModel.notifications.observe(viewLifecycleOwner) { notifications ->
            (binding.rvNotifications.adapter as NotificationAdapter).submitList(notifications)
            binding.swipeRefreshLayout.isRefreshing = false
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading && viewModel.notifications.value.isNullOrEmpty()) {
                binding.swipeRefreshLayout.isRefreshing = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
