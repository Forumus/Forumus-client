package com.hcmus.forumus_client.ui.fragments

import android.content.Intent
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
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.hcmus.forumus_client.data.model.ChatType
import com.hcmus.forumus_client.databinding.FragmentChatsBinding
import com.hcmus.forumus_client.ui.chats.ChatsAdapter
import com.hcmus.forumus_client.ui.chats.ChatItem
import com.hcmus.forumus_client.ui.chats.UserSearchAdapter
import com.hcmus.forumus_client.ui.conversation.ConversationActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatsFragment : Fragment() {

    private var _binding: FragmentChatsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var chatsAdapter: ChatsAdapter
    private lateinit var userSearchAdapter: UserSearchAdapter
    private val viewModel: ChatsViewModel by viewModels()
    
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSearchView()
        setupObservers()
        setupClickListeners()
        setupLoadChats(ChatType.DEFAULT_CHATS)
    }

    private fun setupLoadChats(chatType: Enum<ChatType>) {
        viewModel.resetChatResult()
        viewModel.loadChats(chatType)
    }

    private fun setupRecyclerView() {
        chatsAdapter = ChatsAdapter { chatItem ->
            // Handle chat item click - navigate to individual chat
            navigateToChatActivity(chatItem)
        }
        
        binding.recyclerChats.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatsAdapter
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            // repeatOnLifecycle pauses the collection when the app is in the background
            // This saves battery and prevents crashes when the view is destroyed
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {

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

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            // Show/hide loading indicator
            binding.loadingContainer.visibility = if (isLoading) View.VISIBLE else View.GONE
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
                navigateToChatActivity(it)
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
                com.hcmus.forumus_client.R.drawable.chat_filter_button_selected_background
            )
            binding.btnUnreadChats.background = null
            setupLoadChats(ChatType.DEFAULT_CHATS)
        }

        binding.btnUnreadChats.setOnClickListener {
            binding.btnUnreadChats.background = AppCompatResources.getDrawable(
                requireContext(),
                com.hcmus.forumus_client.R.drawable.chat_filter_button_selected_background
            )
            binding.btnAllChats.background = null
            setupLoadChats(ChatType.UNREAD_CHATS)
        }
    }
    
    private fun showKeyboard() {
        val imm = getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.showSoftInput(binding.editSearch, InputMethodManager.SHOW_IMPLICIT)
    }
    
    private fun hideKeyboard() {
        val imm = getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(binding.editSearch.windowToken, 0)
    }

    private fun navigateToChatActivity(chatItem: ChatItem) {
        try {
            Log.d("ChatsFragment", "Navigating to chat with: ${chatItem.contactName}")
            val intent = Intent(requireContext(), ConversationActivity::class.java).apply {
                putExtra(ConversationActivity.EXTRA_CHAT_ID, chatItem.id)
                putExtra(ConversationActivity.EXTRA_USER_NAME, chatItem.contactName)
                putExtra(ConversationActivity.EXTRA_USER_EMAIL, chatItem.email)
                putExtra(ConversationActivity.EXTRA_USER_PICTURE_URL, chatItem.profilePictureUrl)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("ChatsFragment", "Error navigating to chat", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchRunnable?.let { searchHandler.removeCallbacks(it) }
        _binding = null
    }
}