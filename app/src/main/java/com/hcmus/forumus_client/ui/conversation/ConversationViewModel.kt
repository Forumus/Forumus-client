package com.hcmus.forumus_client.ui.conversation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.hcmus.forumus_client.data.model.Message
import com.hcmus.forumus_client.data.model.MessageType
import com.hcmus.forumus_client.data.repository.ChatRepository
import kotlinx.coroutines.launch

class ConversationViewModel : ViewModel() {
    
    private val chatRepository = ChatRepository()
    private var messagesListener: ListenerRegistration? = null
    
    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    
    private val _sendMessageResult = MutableLiveData<Boolean>()
    val sendMessageResult: LiveData<Boolean> = _sendMessageResult
    
    private var currentChatId: String? = null
    
    companion object {
        private const val TAG = "ChatViewModel"
    }
    
    fun getCurrentUserId(): String? {
        return ChatRepository.getCurrentUserId()
    }
    
    fun loadMessages(chatId: String) {
        currentChatId = chatId
        _isLoading.value = true
        
        // Stop previous listener
        messagesListener?.remove()
        
        messagesListener = chatRepository.listenToMessages(
            chatId = chatId,
            onMessagesChanged = { messageList ->
                _messages.value = messageList
                _isLoading.value = false
                Log.d(TAG, "Loaded ${messageList.size} messages")
            },
            onError = { exception ->
                _error.value = exception.message ?: "Error loading messages"
                _isLoading.value = false
                Log.e(TAG, "Error loading messages", exception)
            }
        )
        
        // Mark messages as read
        markMessagesAsRead(chatId)
    }
    
    fun sendMessage(content: String, imageUrls: MutableList<String> = mutableListOf(), type: MessageType = MessageType.TEXT) {
        val chatId = currentChatId
        if (chatId == null) {
            _error.value = "No chat selected"
            return
        }
        
        val hasContent = content.trim().isNotEmpty()
        val hasImages = imageUrls.isNotEmpty()
        
        if (!hasContent && !hasImages) {
            _error.value = "Message cannot be empty"
            return
        }
        
        if (imageUrls.size > Message.MAX_IMAGES_PER_MESSAGE) {
            _error.value = "Maximum ${Message.MAX_IMAGES_PER_MESSAGE} images allowed"
            return
        }
        
        viewModelScope.launch {
            try {
                val result = chatRepository.sendMessage(chatId, content.trim(), type, imageUrls)
                if (result.isSuccess) {
                    _sendMessageResult.value = true
                    Log.d(TAG, "Message sent successfully")
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to send message"
                    _sendMessageResult.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Error sending message"
                _sendMessageResult.value = false
                Log.e(TAG, "Error sending message", e)
            }
        }
    }
    
    private fun markMessagesAsRead(chatId: String) {
        viewModelScope.launch {
            try {
                chatRepository.markMessagesAsRead(chatId)
            } catch (e: Exception) {
                Log.e(TAG, "Error marking messages as read", e)
            }
        }
    }
    
    fun clearError() {
        _error.value = ""
    }
    
    fun clearSendResult() {
        _sendMessageResult.value = false
    }
    
    override fun onCleared() {
        super.onCleared()
        messagesListener?.remove()
    }
}