package com.hcmus.forumus_client.ui.fragments

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.hcmus.forumus_client.data.repository.ChatRepository
import com.hcmus.forumus_client.data.repository.UserRepository
import com.hcmus.forumus_client.data.model.User
import com.hcmus.forumus_client.ui.chats.ChatItem
import kotlinx.coroutines.launch

class ChatsViewModel : ViewModel() {
    
    private val chatRepository = ChatRepository()
    private val userRepository = UserRepository()
    private var chatsListener: ListenerRegistration? = null
    
    private val _chats = MutableLiveData<List<ChatItem>>()
    val chats: LiveData<List<ChatItem>> = _chats
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    
    // Search-related properties
    private val _searchResults = MutableLiveData<List<User>>()
    val searchResults: LiveData<List<User>> = _searchResults
    
    private val _isSearching = MutableLiveData<Boolean>()
    val isSearching: LiveData<Boolean> = _isSearching
    
    private val _isSearchVisible = MutableLiveData<Boolean>()
    val isSearchVisible: LiveData<Boolean> = _isSearchVisible

    private val _chatResult = MutableLiveData<ChatItem?>()
    val chatResult: LiveData<ChatItem?> = _chatResult
    
    companion object {
        private const val TAG = "ChatsViewModel"
    }
    
    fun loadChats() {
        _isLoading.value = true
        
        // Stop previous listener
        chatsListener?.remove()
        
        chatsListener = chatRepository.listenToUserChats(
            onChatsChanged = { chatList ->
                _chats.value = chatList
                _isLoading.value = false
                Log.d(TAG, "Loaded ${chatList.size} chats")
            },
            onError = { exception ->
                _error.value = exception.message ?: "Error loading chats"
                _isLoading.value = false
                Log.e(TAG, "Error loading chats", exception)
            }
        )
    }
    
    fun searchUsers(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            try {
                val results = userRepository.searchUsers(query)
                _searchResults.value = results
                Log.d(TAG, "Search completed: ${results.size} results for '$query'")
            } catch (e: Exception) {
                _error.value = e.message ?: "Error searching users"
                Log.e(TAG, "Error searching users", e)
            } finally {
                _isSearching.value = false
            }
        }
    }
    
    fun toggleSearchVisibility() {
        _isSearchVisible.value = _isSearchVisible.value != true
        if (_isSearchVisible.value != true) {
            _searchResults.value = emptyList()
        }
    }
    
    fun hideSearch() {
        _isSearchVisible.value = false
        _searchResults.value = emptyList()
    }
    
    fun startChatWithUser(user: User){
        viewModelScope.launch {
            try {
                val result = chatRepository.getOrCreateChat(user.uid)
                _chatResult.value = result
                Log.d(TAG, "Chat created/found with user: ${user.fullName}")
                hideSearch()
            } catch (e: Exception) {
                _error.value = e.message ?: "Error creating chat"
                Log.e(TAG, "Error creating chat with user", e)
            }
        }
    }
    
    fun clearError() {
        _error.value = ""
    }
    
    override fun onCleared() {
        super.onCleared()
        chatsListener?.remove()
    }
}