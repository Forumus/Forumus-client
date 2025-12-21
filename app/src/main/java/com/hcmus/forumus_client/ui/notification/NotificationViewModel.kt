package com.hcmus.forumus_client.ui.notification

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hcmus.forumus_client.data.model.Notification
import com.hcmus.forumus_client.data.repository.NotificationRepository
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {
    private val repository = NotificationRepository()

    private val _notifications = MutableLiveData<List<Notification>>()
    val notifications: LiveData<List<Notification>> = _notifications

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val list = repository.getNotifications()
                _notifications.value = list
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markAsRead(notification: Notification) {
        viewModelScope.launch {
            try {
                repository.markAsRead(notification.id)
                // Optionally update local list or reload
                // loadNotifications() 
            } catch (e: Exception) {
                // error
            }
        }
    }
}
