package com.hcmus.forumus_client.ui.chats

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.data.model.ChatType
import com.hcmus.forumus_client.databinding.ActivityChatsBinding
import com.hcmus.forumus_client.ui.conversation.ConversationActivity
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hcmus.forumus_client.ui.common.BottomNavigationBar
import com.hcmus.forumus_client.ui.navigation.AppNavigator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatsActivity : AppCompatActivity() {

//    private lateinit var binding: ActivityChatsBinding
//
//    private lateinit var chatsAdapter: ChatsAdapter
//    private lateinit var userSearchAdapter: UserSearchAdapter
//    private val viewModel: ChatsViewModel by viewModels()
//
//    private val navigator by lazy { AppNavigator(this) }
//
//    private val searchHandler = Handler(Looper.getMainLooper())
//    private var searchRunnable: Runnable? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityChatsBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        setupWindowInsetsHandling()
//        setupRecyclerView()
//        setupBottomNavigationBar()
//        setupSearchView()
//        setupObservers()
//        setupClickListeners()
//        setupLoadChats(ChatType.DEFAULT_CHATS)
//    }
//
//    private fun setupLoadChats(chatType: Enum<ChatType>) {
//        viewModel.resetChatResult()
//        viewModel.loadChats(chatType)
//    }
//
//    private fun setupRecyclerView() {
//        chatsAdapter = ChatsAdapter({ chatItem ->
//            // Handle chat item click - navigate to individual chat
//            navigateToChatActivity(chatItem)
//        }, { chatItem ->
//            // Handle chat item delete
//            deleteChat(chatItem)
//        })
//
//        binding.recyclerChats.apply {
//            layoutManager = LinearLayoutManager(this@ChatsActivity)
//            adapter = chatsAdapter
//        }
//    }
//
//    private fun setupWindowInsetsHandling() {
//        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
//            val systemBars = insets.getInsets(
//                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
//            )
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
//    }
//
//    private fun setupBottomNavigationBar() {
//        binding.bottomBar.apply {
//            onHomeClick = { navigator.openHome() }
//            onExploreClick = { navigator.openSearch() }
//            onCreatePostClick = { navigator.openCreatePost() }
//            onAlertsClick = { navigator.openAlerts() }
//            onChatClick = { navigator.openChat() }
//            setActiveTab(BottomNavigationBar.Tab.CHAT)
//        }
//    }
//
//    private fun setupObservers() {
//        lifecycleScope.launch {
//            // repeatOnLifecycle pauses the collection when the app is in the background
//            // This saves battery and prevents crashes when the view is destroyed
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
//
//                // Listen to the flow
//                viewModel.chats.collectLatest { chats ->
//                    chatsAdapter.submitList(chats)
//
//                    if (chats.isEmpty()) {
//                        binding.textEmptyChats.visibility = if (viewModel.isLoading.value == true) View.GONE else View.VISIBLE
//                    } else {
//                        binding.textEmptyChats.visibility = View.GONE
//                    }
//                }
//            }
//        }
//
//        viewModel.isLoading.observe(this, Observer { isLoading ->
//            // Show/hide loading indicator
//            binding.loadingContainer.visibility = if (isLoading) View.VISIBLE else View.GONE
//        })
//
//        viewModel.error.observe(this, Observer { errorMessage ->
//            if (errorMessage != null) {
//                Toast.makeText(this@ChatsActivity, errorMessage, Toast.LENGTH_LONG).show()
//                viewModel.clearError()
//            }
//        })
//
//        // Search observers
//        viewModel.isSearchVisible.observe(this, Observer { isVisible ->
//            binding.searchContainer.visibility = if (isVisible) View.VISIBLE else View.GONE
//            if (isVisible) {
//                binding.editSearch.requestFocus()
//                showKeyboard()
//            } else {
//                hideKeyboard()
//            }
//        })
//
//        viewModel.searchResults.observe(this, Observer { users ->
//            userSearchAdapter.submitList(users)
//            binding.textEmptySearch.visibility =
//                if (users.isEmpty() && binding.editSearch.text.toString().isNotBlank())
//                    View.VISIBLE else View.GONE
//        })
//
//        viewModel.isSearching.observe(this, Observer { isSearching ->
//            binding.progressSearch.visibility = if (isSearching) View.VISIBLE else View.GONE
//        })
//
//        viewModel.chatResult.observe(this, Observer { chatItem ->
//            Log.d("ChatsFragment", "Chat started with user, navigating to chat $chatItem")
//            chatItem?.let {
//                navigateToChatActivity(it)
//            }
//        })
//    }
//
//    private fun setupSearchView() {
//        userSearchAdapter = UserSearchAdapter { user ->
//            viewModel.startChatWithUser(user)
//        }
//
//        binding.recyclerSearchResults.apply {
//            layoutManager = LinearLayoutManager(this@ChatsActivity)
//            adapter = userSearchAdapter
//        }
//
//        // Setup search text watcher with debouncing
//        binding.editSearch.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                // Cancel previous search
//                searchRunnable?.let { searchHandler.removeCallbacks(it) }
//
//                // Schedule new search with 500ms delay
//                searchRunnable = Runnable {
//                    val query = s?.toString()?.trim() ?: ""
//                    if (query.isNotBlank()) {
//                        viewModel.searchUsers(query)
//                    } else {
//                        userSearchAdapter.submitList(emptyList())
//                        binding.textEmptySearch.visibility = View.GONE
//                    }
//                }
//                searchHandler.postDelayed(searchRunnable!!, 500)
//            }
//
//            override fun afterTextChanged(s: Editable?) {}
//        })
//    }
//
//    private fun setupClickListeners() {
//        binding.icSearch.setOnClickListener {
//            viewModel.toggleSearchVisibility()
//        }
//
//        binding.btnBackSearch.setOnClickListener {
//            viewModel.hideSearch()
//            binding.editSearch.text.clear()
//        }
//
//        binding.btnAllChats.setOnClickListener {
//            binding.btnAllChats.background = AppCompatResources.getDrawable(
//                this@ChatsActivity,
//                R.drawable.chat_filter_button_selected_background
//            )
//            binding.btnUnreadChats.background = null
//            setupLoadChats(ChatType.DEFAULT_CHATS)
//        }
//
//        binding.btnUnreadChats.setOnClickListener {
//            binding.btnUnreadChats.background = AppCompatResources.getDrawable(
//                this@ChatsActivity,
//                R.drawable.chat_filter_button_selected_background
//            )
//            binding.btnAllChats.background = null
//            setupLoadChats(ChatType.UNREAD_CHATS)
//        }
//    }
//
//    private fun showKeyboard() {
//        val imm = ContextCompat.getSystemService(this@ChatsActivity, InputMethodManager::class.java)
//        imm?.showSoftInput(binding.editSearch, InputMethodManager.SHOW_IMPLICIT)
//    }
//
//    private fun hideKeyboard() {
//        val imm = ContextCompat.getSystemService(this@ChatsActivity, InputMethodManager::class.java)
//        imm?.hideSoftInputFromWindow(binding.editSearch.windowToken, 0)
//    }
//
//    private fun navigateToChatActivity(chatItem: ChatItem) {
//        try {
//            Log.d("ChatsFragment", "Navigating to chat with: ${chatItem.contactName}")
//            val intent = Intent(this@ChatsActivity, ConversationActivity::class.java).apply {
//                putExtra(ConversationActivity.Companion.EXTRA_CHAT_ID, chatItem.id)
//                putExtra(ConversationActivity.Companion.EXTRA_USER_NAME, chatItem.contactName)
//                putExtra(ConversationActivity.Companion.EXTRA_USER_EMAIL, chatItem.email)
//                putExtra(ConversationActivity.Companion.EXTRA_USER_PICTURE_URL, chatItem.profilePictureUrl)
//            }
//            startActivity(intent)
//        } catch (e: Exception) {
//            Log.e("ChatsFragment", "Error navigating to chat", e)
//        }
//    }
//
//    private fun deleteChat(chatItem: ChatItem) {
//        Log.d("ChatsFragment", "Attempting to delete chat: ${chatItem.id}")
//        showDeleteChatConfirmationDialog(chatItem)
//    }
//
//    private fun showDeleteChatConfirmationDialog(chatItem: ChatItem) {
//        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_chat, null)
//        val dialog = AlertDialog.Builder(this@ChatsActivity)
//            .setView(dialogView)
//            .setCancelable(true)
//            .create()
//
//        // Set custom background
//        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
//
//        dialogView.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
//            dialog.dismiss()
//        }
//
//        dialogView.findViewById<Button>(R.id.btn_delete).setOnClickListener {
//            dialog.dismiss()
//            viewModel.deleteChat(chatItem.id)
//            Toast.makeText(this@ChatsActivity, "Chat deleted", Toast.LENGTH_SHORT).show()
//        }
//
//        dialog.show()
//    }
}