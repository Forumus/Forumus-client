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
import android.app.AlertDialog

class NotificationFragment : Fragment() {

    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NotificationViewModel by activityViewModels()
    private val chatsViewModel: com.hcmus.forumus_client.ui.chats.ChatsViewModel by activityViewModels()

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
                
                if (notification.type == "STATUS_CHANGED") {
                     // Show Custom Status Detail Modal
                     val dialogView = LayoutInflater.from(requireContext()).inflate(com.hcmus.forumus_client.R.layout.dialog_notification_status, null)
                     val dialog = android.app.AlertDialog.Builder(requireContext())
                        .setView(dialogView)
                        .setCancelable(true)
                        .create()

                     // Bind Views
                     val tvMessage = dialogView.findViewById<android.widget.TextView>(com.hcmus.forumus_client.R.id.tvMessage)
                     val tvCurrentStatus = dialogView.findViewById<android.widget.TextView>(com.hcmus.forumus_client.R.id.tvCurrentStatus)
                     val btnOk = dialogView.findViewById<android.view.View>(com.hcmus.forumus_client.R.id.btnOk)

                     tvMessage.text = notification.previewText
                     
                     // Determine status from notification text
                     val statusText = notification.previewText.lowercase()
                     val status = when {
                         statusText.contains("normal") -> "NORMAL"
                         statusText.contains("reminder") -> "REMINDED"
                         statusText.contains("warning") -> "WARNED"
                         statusText.contains("suspended") || statusText.contains("banned") -> "BANNED"
                         else -> "UPDATED"
                     }
                     
                     tvCurrentStatus.text = status

                     btnOk.setOnClickListener {
                         dialog.dismiss()
                     }
                     
                     // Make background transparent for rounded corners if possible, or just show
                     dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                     dialog.show()
                } else if (notification.type == "POST_DELETED" || notification.type == "POST_REJECTED") {
                    // Show Post Removal Dialog
                    val dialogView = LayoutInflater.from(requireContext()).inflate(com.hcmus.forumus_client.R.layout.dialog_notification_post_removed, null)
                    val dialog = android.app.AlertDialog.Builder(requireContext())
                        .setView(dialogView)
                        .create()

                    // Bind Views
                    val tvNotificationMessage = dialogView.findViewById<android.widget.TextView>(com.hcmus.forumus_client.R.id.tvNotificationMessage)
                    val tvRejectionReason = dialogView.findViewById<android.widget.TextView>(com.hcmus.forumus_client.R.id.tvRejectionReason)
                    val tvOriginalTitle = dialogView.findViewById<android.widget.TextView>(com.hcmus.forumus_client.R.id.tvOriginalTitle)
                    val tvOriginalContent = dialogView.findViewById<android.widget.TextView>(com.hcmus.forumus_client.R.id.tvOriginalContent)
                    val btnDismiss = dialogView.findViewById<android.view.View>(com.hcmus.forumus_client.R.id.btnDismiss)

                    tvNotificationMessage.text = notification.previewText
                    
                    if (!notification.rejectionReason.isNullOrEmpty()) {
                        tvRejectionReason.text = getString(R.string.reason_label, notification.rejectionReason)
                        tvRejectionReason.visibility = View.VISIBLE
                    } else {
                        tvRejectionReason.visibility = View.GONE
                    }
                    tvOriginalTitle.text = notification.originalPostTitle ?: getString(R.string.original_post_fallback)
                    tvOriginalContent.text = notification.originalPostContent ?: getString(R.string.content_not_available)

                    btnDismiss.setOnClickListener {
                        dialog.dismiss()
                    }
                    
                    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                    dialog.show()
                } else if (notification.targetId.isNotEmpty()) {
                    // Navigate to Post Detail
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

        chatsViewModel.unreadChatCount.observe(viewLifecycleOwner) { count ->
             android.util.Log.d("NotificationFragment", "Chat badge update: $count")
             binding.bottomBar.setChatBadge(count)
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
