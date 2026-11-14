package com.dinamikada

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

// Bildirim erişimi: hem müzik oturumuna erişim hem de gelen bildirimleri adada göstermek için gerekli.
class MediaListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        IslandService.instance?.onNotif(sbn)
    }
}
