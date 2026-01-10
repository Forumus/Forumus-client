package com.hcmus.forumus_client.ui.notification

import com.hcmus.forumus_client.data.model.Notification

sealed class NotificationListItem {
    abstract val id: String

    data class Header(val title: String) : NotificationListItem() {
        override val id: String = title
    }

    data class Item(val notification: Notification) : NotificationListItem() {
        override val id: String = notification.id
    }

    data object ShowMore : NotificationListItem() {
        override val id: String = "SHOW_MORE_ITEM_ID"
    }
}
