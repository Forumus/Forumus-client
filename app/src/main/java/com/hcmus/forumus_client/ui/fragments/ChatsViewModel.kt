package com.hcmus.forumus_client.ui.fragments

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.hcmus.forumus_client.data.repository.ChatRepository
import com.hcmus.forumus_client.ui.chats.ChatItem
import kotlinx.coroutines.launch

class ChatsViewModel : ViewModel() {
    
    private val chatRepository = ChatRepository()
    private var chatsListener: ListenerRegistration? = null
    
    private val _chats = MutableLiveData<List<ChatItem>>()
    val chats: LiveData<List<ChatItem>> = _chats
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    
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
    
    fun clearError() {
        _error.value = ""
    }
    
    override fun onCleared() {
        super.onCleared()
        chatsListener?.remove()
    }
}