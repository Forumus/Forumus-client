package com.hcmus.forumus_client.ui.chats

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.data.model.ChatType
import com.hcmus.forumus_client.databinding.FragmentChatsBinding
import com.hcmus.forumus_client.ui.common.BottomNavigationBar
import com.hcmus.forumus_client.NavGraphDirections
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.getValue

import androidx.fragment.app.activityViewModels

class ChatsFragment : Fragment (){
    private lateinit var binding: FragmentChatsBinding
    private val viewModel: ChatsViewModel by activityViewModels()
    private val notificationViewModel: com.hcmus.forumus_client.ui.notification.NotificationViewModel by activityViewModels()
    private val navController by lazy { findNavController() }
    private lateinit var chatsAdapter: ChatsAdapter
    private lateinit var userSearchAdapter: UserSearchAdapter
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private var chatType: Enum<ChatType> = ChatType.DEFAULT_CHATS

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSwipeRefresh()
        setupRecyclerView()
        setupBottomNavigation()
        setupSearchView()
        setupObservers()
        setupClickListeners()
        setupLoadChats(chatType)
    }

    private fun setupLoadChats(chatType: Enum<ChatType>) {
        viewModel.resetChatResult()
        viewModel.loadChats(chatType)
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            setupLoadChats(chatType)
        }
    }

    private fun setupRecyclerView() {
        chatsAdapter = ChatsAdapter({ chatItem ->
            // Handle chat item click - navigate to individual chat
            val action = ChatsFragmentDirections.actionChatsFragmentToConversationFragment(chatItem.id,
                chatItem.contactName, chatItem.email, chatItem.profilePictureUrl)
            navController.navigate(action)
        }, { chatItem ->
            // Handle chat item delete
            deleteChat(chatItem)
        })

        binding.recyclerChats.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatsAdapter
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            // repeatOnLifecycle pauses the collection when the app is in the background
            // This saves battery and prevents crashes when the view is destroyed
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Listen to the flow
                viewModel.chats.collectLatest { chats ->
                    chatsAdapter.submitList(chats)

                    if (chats.isEmpty()) {
                        binding.textEmptyChats.visibility = if (viewModel.isLoading.value == true) View.GONE else View.VISIBLE
                    } else {
                        binding.textEmptyChats.visibility = View.GONE
                    }
                }
            }
        }

        notificationViewModel.unreadCount.observe(viewLifecycleOwner) { count ->
             android.util.Log.d("ChatsFragment", "Notification badge update: $count")
             binding.bottomBar.setNotificationBadge(count)
        }

        viewModel.unreadChatCount.observe(viewLifecycleOwner) { count ->
             android.util.Log.d("ChatsFragment", "Chat badge update: $count")
             binding.bottomBar.setChatBadge(count)
        }

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            // Show/hide loading indicator
            binding.swipeRefresh.isRefreshing = isLoading
        })

        viewModel.error.observe(viewLifecycleOwner, Observer { errorMessage ->
            if (errorMessage != null) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        })

        // Search observers
        viewModel.isSearchVisible.observe(viewLifecycleOwner, Observer { isVisible ->
            binding.searchContainer.visibility = if (isVisible) View.VISIBLE else View.GONE
            if (isVisible) {
                binding.editSearch.requestFocus()
                showKeyboard()
            } else {
                hideKeyboard()
            }
        })

        viewModel.searchResults.observe(viewLifecycleOwner, Observer { users ->
            userSearchAdapter.submitList(users)
            binding.textEmptySearch.visibility =
                if (users.isEmpty() && binding.editSearch.text.toString().isNotBlank())
                    View.VISIBLE else View.GONE
        })

        viewModel.isSearching.observe(viewLifecycleOwner, Observer { isSearching ->
            binding.progressSearch.visibility = if (isSearching) View.VISIBLE else View.GONE
        })

        viewModel.chatResult.observe(viewLifecycleOwner, Observer { chatItem ->
            Log.d("ChatsFragment", "Chat started with user, navigating to chat $chatItem")
            chatItem?.let {
                val action = ChatsFragmentDirections.actionChatsFragmentToConversationFragment(it.id,
                    it.contactName, it.email, it.profilePictureUrl)
                navController.navigate(action)
            }
        })
    }

    private fun setupSearchView() {
        userSearchAdapter = UserSearchAdapter { user ->
            viewModel.startChatWithUser(user)
        }

        binding.recyclerSearchResults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userSearchAdapter
        }

        // Setup search text watcher with debouncing
        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Cancel previous search
                searchRunnable?.let { searchHandler.removeCallbacks(it) }

                // Schedule new search with 500ms delay
                searchRunnable = Runnable {
                    val query = s?.toString()?.trim() ?: ""
                    if (query.isNotBlank()) {
                        viewModel.searchUsers(query)
                    } else {
                        userSearchAdapter.submitList(emptyList())
                        binding.textEmptySearch.visibility = View.GONE
                    }
                }
                searchHandler.postDelayed(searchRunnable!!, 500)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupClickListeners() {
        binding.icSearch.setOnClickListener {
            viewModel.toggleSearchVisibility()
        }

        binding.btnBackSearch.setOnClickListener {
            viewModel.hideSearch()
            binding.editSearch.text.clear()
        }

        binding.btnAllChats.setOnClickListener {
            binding.btnAllChats.background = AppCompatResources.getDrawable(
                requireContext(),
                R.drawable.chat_filter_button_selected_background
            )
            binding.btnUnreadChats.background = null
            setupLoadChats(ChatType.DEFAULT_CHATS)
            chatType = ChatType.DEFAULT_CHATS
        }

        binding.btnUnreadChats.setOnClickListener {
            binding.btnUnreadChats.background = AppCompatResources.getDrawable(
                requireContext(),
                R.drawable.chat_filter_button_selected_background
            )
            binding.btnAllChats.background = null
            setupLoadChats(ChatType.UNREAD_CHATS)
            chatType = ChatType.UNREAD_CHATS
        }
    }

    /**
     * Setup bottom navigation bar for fragment switching.
     */
    private fun setupBottomNavigation() {
        binding.bottomBar.apply {
            setActiveTab(BottomNavigationBar.Tab.CHAT)
            onHomeClick = { navController.navigate(R.id.homeFragment) }
            onExploreClick = { navController.navigate(R.id.searchFragment) }
            onCreatePostClick = { navController.navigate(R.id.createPostFragment) }
            onAlertsClick = { navController.navigate(NavGraphDirections.actionGlobalNotificationFragment()) }
            onChatClick = { navController.navigate(R.id.chatsFragment) }
        }
    }

    private fun showKeyboard() {
        val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.showSoftInput(binding.editSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(binding.editSearch.windowToken, 0)
    }

    private fun deleteChat(chatItem: ChatItem) {
        Log.d("ChatsFragment", "Attempting to delete chat: ${chatItem.id}")
        showDeleteChatConfirmationDialog(chatItem)
    }

    private fun showDeleteChatConfirmationDialog(chatItem: ChatItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_chat, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Set custom background
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        dialogView.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btn_delete).setOnClickListener {
            dialog.dismiss()
            viewModel.deleteChat(chatItem.id)
            Toast.makeText(requireContext(), "Chat deleted", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }
}