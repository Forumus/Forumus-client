package com.hcmus.forumus_client.ui.notification

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hcmus.forumus_client.data.model.Notification
import com.hcmus.forumus_client.data.repository.NotificationRepository
import kotlinx.coroutines.launch

import androidx.lifecycle.MediatorLiveData
import java.util.Calendar
import java.util.Date

class NotificationViewModel : ViewModel() {
    private val repository = NotificationRepository()

    
    // Raw data
    private val _notifications = MutableLiveData<List<Notification>>()
    
    // UI State for list items
    private val _displayItems = MutableLiveData<List<NotificationListItem>>()
    val displayItems: LiveData<List<NotificationListItem>> = _displayItems

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Unread count
    private val _unreadCount = MutableLiveData<Int>()
    val unreadCount: LiveData<Int> = _unreadCount
    
    private var isEarlierExpanded = false
    
    private var listeningJob: kotlinx.coroutines.Job? = null
    
    init {
        startListening()
    }

    private fun startListening() {
        listeningJob?.cancel()
        listeningJob = viewModelScope.launch {
            _isLoading.value = true
            repository.getNotificationsFlow()
                .collect { list ->
                    _isLoading.value = false
                    // Sort descending by date just to be safe, though query does it
                    val sortedList = list.sortedByDescending { it.createdAt?.toDate() }
                    
                    _notifications.value = sortedList
                    updateUnreadCount(sortedList)
                    processDisplayList(sortedList)
                }
        }
    }

    fun loadNotifications() {
        startListening()
    }
    
    fun expandEarlierSection() {
        isEarlierExpanded = true
        _notifications.value?.let { processDisplayList(it) }
    }



    fun markAsRead(notification: Notification) {
        if (notification.isRead) return
        
        // Optimistic Update (Visual only, to feel instant)
        val currentList = _notifications.value?.toMutableList() ?: return
        val index = currentList.indexOfFirst { it.id == notification.id }
        if (index != -1) {
            val updatedNoti = currentList[index].copy(isRead = true)
            currentList[index] = updatedNoti
            _notifications.value = ArrayList(currentList)
            updateUnreadCount(currentList)
            processDisplayList(currentList)
        }

        viewModelScope.launch {
            try {
                repository.markAsRead(notification.id)
                // Flow will update automatically on success
            } catch (e: Exception) {
                // Revert or log error
            }
        }
    }
    
    private fun updateUnreadCount(list: List<Notification>) {
        val count = list.count { !it.isRead }
        _unreadCount.value = count
    }

    private fun processDisplayList(list: List<Notification>) {
        val todayItems = mutableListOf<Notification>()
        val earlierItems = mutableListOf<Notification>()
        
        val calendar = Calendar.getInstance()
        val todayStart = getStartOfDay(calendar.time)
        
        list.forEach { noti ->
            val date = noti.createdAt?.toDate() ?: Date()
            if (date.after(todayStart)) {
                todayItems.add(noti)
            } else {
                earlierItems.add(noti)
            }
        }
        
        val displayList = mutableListOf<NotificationListItem>()
        
        // TODAY Section
        if (todayItems.isNotEmpty()) {
            displayList.add(NotificationListItem.Header(com.hcmus.forumus_client.R.string.today))
            displayList.addAll(todayItems.map { NotificationListItem.Item(it) })
        }
        
        // EARLIER Section
        if (earlierItems.isNotEmpty()) {
            displayList.add(NotificationListItem.Header(com.hcmus.forumus_client.R.string.earlier))
            
            if (isEarlierExpanded || earlierItems.size <= 5) {
                displayList.addAll(earlierItems.map { NotificationListItem.Item(it) })
            } else {
                // Show top 5
                displayList.addAll(earlierItems.take(5).map { NotificationListItem.Item(it) })
                displayList.add(NotificationListItem.ShowMore)
            }
        }
        
        _displayItems.value = displayList
    }

    private fun getStartOfDay(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }
}
