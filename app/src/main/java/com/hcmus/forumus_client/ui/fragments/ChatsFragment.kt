package com.hcmus.forumus_client.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.hcmus.forumus_client.databinding.FragmentChatsBinding
import com.hcmus.forumus_client.ui.chats.ChatsAdapter
import com.hcmus.forumus_client.ui.chats.ChatItem
import com.hcmus.forumus_client.ui.conversation.ConversationActivity

class ChatsFragment : Fragment() {

    private var _binding: FragmentChatsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var chatsAdapter: ChatsAdapter
    private val viewModel: ChatsViewModel by viewModels()

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
        setupObservers()
        
        // Load chats from Firebase
        viewModel.loadChats()
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
        viewModel.chats.observe(viewLifecycleOwner, Observer { chats ->
            android.util.Log.d("ChatsFragment", "Received ${chats.size} chats from Firebase")
            chatsAdapter.submitList(chats)
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            // Show/hide loading indicator
            // binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        })

        viewModel.error.observe(viewLifecycleOwner, Observer { errorMessage ->
            if (errorMessage != null) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        })
    }



    private fun navigateToChatActivity(chatItem: ChatItem) {
        try {
            android.util.Log.d("ChatsFragment", "Navigating to chat with: ${chatItem.contactName}")
            val intent = Intent(requireContext(), ConversationActivity::class.java).apply {
                putExtra(ConversationActivity.EXTRA_CHAT_ID, chatItem.id)
                putExtra(ConversationActivity.EXTRA_USER_NAME, chatItem.contactName)
                putExtra(ConversationActivity.EXTRA_USER_EMAIL, chatItem.email)
                putExtra(ConversationActivity.EXTRA_USER_PICTURE_URL, chatItem.profilePictureUrl)
            }
            startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("ChatsFragment", "Error navigating to chat", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}