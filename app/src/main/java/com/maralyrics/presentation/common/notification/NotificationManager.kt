package com.maralyrics.presentation.common.notification

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationManager @Inject constructor() {
    private val _notifications = MutableSharedFlow<NotificationData>(extraBufferCapacity = 10)
    val notifications = _notifications.asSharedFlow()

    private var lastNotification: NotificationData? = null
    private var lastTimestamp: Long = 0

    suspend fun showNotification(notification: NotificationData) {
        // Simple duplicate prevention: don't show the same message within 2 seconds
        if (notification.message == lastNotification?.message && 
            System.currentTimeMillis() - lastTimestamp < 2000
        ) {
            return
        }
        
        lastNotification = notification
        lastTimestamp = System.currentTimeMillis()
        _notifications.emit(notification)
    }

    suspend fun showSuccess(message: String) = showNotification(NotificationData(message = message, type = NotificationType.SUCCESS))
    suspend fun showError(message: String) = showNotification(NotificationData(message = message, type = NotificationType.ERROR))
    suspend fun showWarning(message: String) = showNotification(NotificationData(message = message, type = NotificationType.WARNING))
    suspend fun showInfo(message: String) = showNotification(NotificationData(message = message, type = NotificationType.INFO))
    
    suspend fun showOffline() = showNotification(NotificationData(message = "You're offline. Showing downloaded content.", type = NotificationType.OFFLINE))
    suspend fun showOnline() = showNotification(NotificationData(message = "Back online.", type = NotificationType.ONLINE))
    
    suspend fun showFavoriteAdded() = showNotification(NotificationData(message = "Added to favorites.", type = NotificationType.FAVORITE_ADDED))
    suspend fun showFavoriteRemoved() = showNotification(NotificationData(message = "Removed from favorites.", type = NotificationType.FAVORITE_REMOVED))
    
    suspend fun showLanguageChanged(language: String) = showNotification(NotificationData(message = "Language changed to $language.", type = NotificationType.LANGUAGE_CHANGED))
    suspend fun showThemeChanged(theme: String) = showNotification(NotificationData(message = "$theme enabled.", type = NotificationType.THEME_CHANGED))
    suspend fun showCopied() = showNotification(NotificationData(message = "Lyrics copied.", type = NotificationType.COPIED))
    
    suspend fun showSyncStarted() = showNotification(NotificationData(message = "Synchronizing data...", type = NotificationType.SYNCING))
    suspend fun showSyncSuccess() = showNotification(NotificationData(message = "Database synchronized successfully.", type = NotificationType.SUCCESS))
    suspend fun showSyncFailed() = showNotification(NotificationData(message = "Synchronization failed.", type = NotificationType.ERROR))
}
