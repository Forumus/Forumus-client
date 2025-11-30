package com.hcmus.forumus_client.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.hcmus.forumus_client.databinding.FragmentChatsBinding
import com.hcmus.forumus_client.ui.chats.ChatsAdapter
import com.hcmus.forumus_client.ui.chats.ChatItem
import com.hcmus.forumus_client.ui.message.MessageActivity

class ChatsFragment : Fragment() {

    private var _binding: FragmentChatsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var chatsAdapter: ChatsAdapter

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
        loadSampleChats()
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

    private fun loadSampleChats() {
        val sampleChats = listOf(
            ChatItem(
                id = "1",
                contactName = "Sarah Johnson",
                lastMessage = "Hey! Did you see the photos from last weekend?",
                timestamp = "2m",
                isUnread = true,
                unreadCount = 3,
                profileImageUrl = null
            ),
            ChatItem(
                id = "2",
                contactName = "Michael Chen",
                lastMessage = "Thanks for your help with the project!",
                timestamp = "15m",
                isUnread = true,
                unreadCount = 1,
                profileImageUrl = null
            ),
            ChatItem(
                id = "3",
                contactName = "Emma Williams",
                lastMessage = "See you tomorrow at 3pm 👋",
                timestamp = "1h",
                isUnread = false,
                unreadCount = 0,
                profileImageUrl = null
            ),
            ChatItem(
                id = "4",
                contactName = "James Rodriguez",
                lastMessage = "The meeting has been rescheduled",
                timestamp = "3h",
                isUnread = true,
                unreadCount = 2,
                profileImageUrl = null
            ),
            ChatItem(
                id = "5",
                contactName = "Olivia Taylor",
                lastMessage = "Perfect! I'll send you the details",
                timestamp = "5h",
                isUnread = false,
                unreadCount = 0,
                profileImageUrl = null
            ),
            ChatItem(
                id = "6",
                contactName = "David Martinez",
                lastMessage = "Can we catch up this week?",
                timestamp = "1d",
                isUnread = false,
                unreadCount = 0,
                profileImageUrl = null
            ),
            ChatItem(
                id = "7",
                contactName = "Sophia Anderson",
                lastMessage = "I loved that restaurant recommendation!",
                timestamp = "1d",
                isUnread = false,
                unreadCount = 0,
                profileImageUrl = null
            ),
            ChatItem(
                id = "8",
                contactName = "Daniel Brown",
                lastMessage = "Just sent you the files",
                timestamp = "2d",
                isUnread = false,
                unreadCount = 0,
                profileImageUrl = null
            )
        )
        
        android.util.Log.d("ChatsFragment", "Loading ${sampleChats.size} chat items")
        chatsAdapter.submitList(sampleChats)
    }

    private fun navigateToChatActivity(chatItem: ChatItem) {
        try {
            android.util.Log.d("ChatsFragment", "Navigating to chat with: ${chatItem.contactName}")
            val intent = Intent(requireContext(), MessageActivity::class.java).apply {
                putExtra(MessageActivity.EXTRA_USER_NAME, chatItem.contactName)
                putExtra(MessageActivity.EXTRA_USER_EMAIL, "${chatItem.contactName.lowercase().replace(" ", "")}@example.com")
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