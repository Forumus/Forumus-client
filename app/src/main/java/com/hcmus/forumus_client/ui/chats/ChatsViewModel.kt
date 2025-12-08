package com.hcmus.forumus_client.ui.chats

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hcmus.forumus_client.data.model.ChatType
import com.hcmus.forumus_client.data.model.User
import com.hcmus.forumus_client.data.repository.ChatRepository
import com.hcmus.forumus_client.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ChatsViewModel : ViewModel() {

    private val chatRepository = ChatRepository()
    private val userRepository = UserRepository()

    private val _chats = MutableStateFlow<List<ChatItem>>(emptyList())
    val chats: StateFlow<List<ChatItem>> = _chats

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

    fun resetChatResult() {
        _chats.value = emptyList()
        _chatResult.value = null
    }

    fun loadChats(chatType: Enum<ChatType>) {
        _isLoading.value = true

        viewModelScope.launch {
            chatRepository.getUserChatsFlow(chatType)
                .catch { e ->
                    Log.e("ViewModel", "Error loading chats", e)
                }
                .collect { chatList ->
                    _chats.value = chatList
                    _isLoading.value = false
                    Log.d(TAG, "Chats loaded: ${chatList.size} items")
                }
        }
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

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            try {
                val result = chatRepository.deleteChat(chatId)
                if (result.isSuccess) {
                    Log.d(TAG, "Chat deleted successfully: $chatId")
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Error deleting chat"
                    Log.e(TAG, "Error deleting chat", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Error deleting chat"
                Log.e(TAG, "Error deleting chat", e)
            }
        }
    }

    fun clearError() {
        _error.value = ""
    }

}