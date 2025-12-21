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

    fun loadNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val list = repository.getNotifications().sortedByDescending { it.createdAt?.toDate() }
                _notifications.value = list
                updateUnreadCount(list)
                processDisplayList(list)
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
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
            displayList.add(NotificationListItem.Header("Today"))
            displayList.addAll(todayItems.map { NotificationListItem.Item(it) })
        }
        
        // EARLIER Section
        if (earlierItems.isNotEmpty()) {
            displayList.add(NotificationListItem.Header("Before"))
            
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
    
    fun expandEarlierSection() {
        isEarlierExpanded = true
        _notifications.value?.let { processDisplayList(it) }
    }

    fun markAsRead(notification: Notification) {
        if (notification.isRead) return
        
        // OPTIMISTIC UPDATE: Update UI immediately
        val currentList = _notifications.value?.toMutableList() ?: return
        val index = currentList.indexOfFirst { it.id == notification.id }
        
        if (index != -1) {
            val updatedNoti = currentList[index].copy(isRead = true)
            currentList[index] = updatedNoti
            
            // Emit new list instance
            _notifications.value = ArrayList(currentList)
            updateUnreadCount(currentList)
            processDisplayList(currentList)
            
            android.util.Log.d("NotificationVM", "Optimistic update: ID=${notification.id} isRead=true. New Unread Count=${_unreadCount.value}")
        }

        viewModelScope.launch {
            try {
                repository.markAsRead(notification.id)
                android.util.Log.d("NotificationVM", "Firestore update success: ID=${notification.id}")
            } catch (e: Exception) {
                android.util.Log.e("NotificationVM", "Firestore update failed: ${e.message}")
                // Optionally revert optimistic update here if critical
            }
        }
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
