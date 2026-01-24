package com.hcmus.forumus_client.ui.conversation

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import com.hcmus.forumus_client.data.model.Message
import com.hcmus.forumus_client.data.model.MessageType
import com.hcmus.forumus_client.data.repository.ChatRepository
import com.hcmus.forumus_client.workers.SendMessageWorker
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class ConversationViewModel(application: Application) : AndroidViewModel(application) {
    
    private val chatRepository = ChatRepository()
    private val workManager = WorkManager.getInstance(application)
    private val gson = Gson()
    private var messagesListener: ListenerRegistration? = null

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _isLoadingMore = MutableLiveData<Boolean>()
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore
    
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    
    private val _sendMessageResult = MutableStateFlow(false)
    val sendMessageResult: StateFlow<Boolean> = _sendMessageResult
    
    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading
    
    private var currentChatId: String? = null
    private var lastErrorTime = 0L // Prevent error spam
    
    companion object {
        private const val TAG = "ConversationViewModel"
        private const val ERROR_DEBOUNCE_TIME = 3000L // 3 seconds between errors
        private const val MESSAGES_PER_PAGE = 50
    }
    
    fun getCurrentUserId(): String? {
        return ChatRepository.getCurrentUserId()
    }
    
    fun loadMessages(chatId: String) {
        currentChatId = chatId
        _isLoading.value = true

        Log.d(TAG, "Loading messages for chatId: $chatId")

        // CRITICAL: Stop previous listener to prevent memory leaks
        messagesListener?.remove()
        messagesListener = null

        viewModelScope.launch {
            chatRepository.getChatMessagesFlow(chatId, MESSAGES_PER_PAGE)
                .catch { e ->
                    Log.e("ChatViewModel", "Error loading messages", e)
                }
                .collect { messageList ->
                    // This runs on Main thread, but purely for updating the UI list.
                    // All the heavy allocation work was done in the background.
                    _messages.value = messageList
                    _isLoading.value = false
                }
        }
        // Mark messages as read - optimize to avoid unnecessary coroutine
        markMessagesAsRead(chatId)
    }

    fun loadPreviousMessages() {
        val chatId = currentChatId
        if (chatId == null) {
            setErrorWithDebounce("No chat selected")
            return
        }

        val currentMessages = _messages.value
        if (currentMessages.isEmpty()) {
            return
        }

        // Get timestamp of the first (oldest) message
        val oldestMessage = currentMessages.firstOrNull()
        if (oldestMessage?.timestamp == null) {
            return
        }

        _isLoadingMore.value = true

        viewModelScope.launch {
            try {
                val oldestTimestampMillis = oldestMessage.timestamp!!.toDate().time
                val result = withContext(Dispatchers.IO) {
                    chatRepository.loadPreviousMessages(
                        chatId,
                        oldestTimestampMillis,
                        MESSAGES_PER_PAGE
                    )
                }

                if (result.isSuccess) {
                    val previousMessages = result.getOrNull() ?: emptyList()
                    if (previousMessages.isNotEmpty()) {
                        // Prepend previous messages to the beginning
                        val updatedMessages = previousMessages + currentMessages
                        _messages.value = updatedMessages
                        Log.d(TAG, "Loaded ${previousMessages.size} previous messages")
                    }
                } else {
                    Log.e(TAG, "Failed to load previous messages", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading previous messages", e)
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun sendMessage(content: String, imageUrls: MutableList<String> = mutableListOf(), type: MessageType = MessageType.TEXT) {
        val chatId = currentChatId
        if (chatId == null) {
            setErrorWithDebounce("No chat selected")
            return
        }

        val hasContent = content.trim().isNotEmpty()
        val hasImages = imageUrls.isNotEmpty()

        if (!hasContent && !hasImages) {
            setErrorWithDebounce("Message cannot be empty")
            return
        }

        if (imageUrls.size > Message.MAX_IMAGES_PER_MESSAGE) {
            setErrorWithDebounce("Maximum ${Message.MAX_IMAGES_PER_MESSAGE} images allowed")
            return
        }

        // If there are images, use WorkManager for background processing
        if (hasImages) {
            sendMessageWithWorkManager(chatId, content.trim(), type, imageUrls)
        } else {
            // For text-only messages, send immediately (faster)
            sendMessageDirect(chatId, content.trim(), type, imageUrls)
        }
    }

    /**
     * Send message with images using WorkManager for background processing.
     * This ensures uploads continue even if the user navigates away.
     */
    private fun sendMessageWithWorkManager(
        chatId: String,
        content: String,
        type: MessageType,
        imageUrls: MutableList<String>
    ) {
        Log.d(TAG, "Enqueuing message send work with ${imageUrls.size} images")
        
        _isLoading.value = true
        _isUploading.value = true

        // Prepare input data for the worker
        val inputData = Data.Builder()
            .putString(SendMessageWorker.KEY_CHAT_ID, chatId)
            .putString(SendMessageWorker.KEY_MESSAGE_CONTENT, content)
            .putString(SendMessageWorker.KEY_MESSAGE_TYPE, type.name)
            .putString(SendMessageWorker.KEY_IMAGE_URLS, gson.toJson(imageUrls))
            .build()

        // Create and enqueue the work request
        val sendMessageWork = OneTimeWorkRequestBuilder<SendMessageWorker>()
            .setInputData(inputData)
            .build()

        workManager.enqueue(sendMessageWork)

        // Observe the work status
        observeWorkStatus(sendMessageWork.id)
        
        // Clear UI states immediately (work continues in background)
        _isLoading.value = false
        _isUploading.value = false
        _sendMessageResult.value = true
        
        Log.d(TAG, "Message send work enqueued. Upload will continue in background.")
    }

    /**
     * Send message directly (for text-only messages).
     */
    private fun sendMessageDirect(
        chatId: String,
        content: String,
        type: MessageType,
        imageUrls: MutableList<String>
    ) {
        _isLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = chatRepository.sendMessage(chatId, content, type, imageUrls)

                if (result.isSuccess) {
                    _sendMessageResult.value = true
                    Log.d(TAG, "Message sent successfully")
                } else {
                    _sendMessageResult.value = false
                    Log.e(TAG, "Error sending message", result.exceptionOrNull())
                }
                _isLoading.value = false

            } catch (e: Exception) {
                _isLoading.value = false
                Log.e(TAG, "Send message error: ${e.message}", e)
            }
        }
    }

    /**
     * Observe the work status and log progress
     */
    private fun observeWorkStatus(workId: UUID) {
        workManager.getWorkInfoByIdLiveData(workId).observeForever { workInfo ->
            if (workInfo != null) {
                when (workInfo.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        Log.d(TAG, "Background message send completed successfully")
                    }
                    WorkInfo.State.FAILED -> {
                        val error = workInfo.outputData.getString(SendMessageWorker.KEY_RESULT_ERROR)
                        Log.e(TAG, "Background message send failed: $error")
                        setErrorWithDebounce(error ?: "Failed to send message")
                    }
                    WorkInfo.State.RUNNING -> {
                        Log.d(TAG, "Background message send in progress...")
                    }
                    else -> {
                        Log.d(TAG, "Work state: ${workInfo.state}")
                    }
                }
            }
        }
    }
    
    private fun markMessagesAsRead(chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
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
    
    fun deleteMessage(messageId: String) {
        val chatId = currentChatId
        if (chatId == null) {
            setErrorWithDebounce("No chat selected")
            return
        }
        
        viewModelScope.launch {
            try {
                val result = chatRepository.deleteMessage(chatId, messageId)
                if (result.isFailure) {
                    setErrorWithDebounce(result.exceptionOrNull()?.message ?: "Error deleting message")
                }
            } catch (e: Exception) {
                setErrorWithDebounce(e.message ?: "Error deleting message")
                Log.e(TAG, "Error deleting message", e)
            }
        }
    }
    
    private fun setErrorWithDebounce(errorMessage: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastErrorTime > ERROR_DEBOUNCE_TIME) {
            _error.value = errorMessage
            lastErrorTime = currentTime
            Log.e(TAG, "Error (debounced): $errorMessage")
        } else {
            Log.e(TAG, "Error (suppressed to prevent spam): $errorMessage")
        }
    }
    
    override fun onCleared() {
        Log.d(TAG, "ConversationViewModel being cleared - cleaning up resources")

        // CRITICAL: Remove Firebase listener first to prevent callbacks
        messagesListener?.remove()
        messagesListener = null

        // Clear all references
        currentChatId = null
        lastErrorTime = 0L

        // MEMORY FIX: Clear LiveData to release references
        _messages.value = emptyList()
        _error.value = ""
        _sendMessageResult.value = false
        _isLoading.value = false
        _isUploading.value = false
        _isLoadingMore.value = false

        super.onCleared()
    }
}