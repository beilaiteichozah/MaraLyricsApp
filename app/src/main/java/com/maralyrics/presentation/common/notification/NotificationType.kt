package com.maralyrics.presentation.common.notification

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class NotificationType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO,
    OFFLINE,
    ONLINE,
    SYNCING,
    DOWNLOAD_COMPLETE,
    UPDATE_AVAILABLE,
    FAVORITE_ADDED,
    FAVORITE_REMOVED,
    LANGUAGE_CHANGED,
    THEME_CHANGED,
    FONT_SIZE_CHANGED,
    SORT_ORDER_CHANGED,
    COPIED;

    val icon: ImageVector
        get() = when (this) {
            SUCCESS -> Icons.Outlined.CheckCircle
            ERROR -> Icons.Outlined.Error
            WARNING -> Icons.Outlined.Warning
            INFO -> Icons.Outlined.Info
            OFFLINE -> Icons.Default.WifiOff
            ONLINE -> Icons.Default.Wifi
            SYNCING -> Icons.Default.Sync
            DOWNLOAD_COMPLETE -> Icons.Default.DownloadDone
            UPDATE_AVAILABLE -> Icons.Default.Update
            FAVORITE_ADDED -> Icons.Default.Favorite
            FAVORITE_REMOVED -> Icons.Default.HeartBroken
            LANGUAGE_CHANGED -> Icons.Default.Language
            THEME_CHANGED -> Icons.Default.Palette
            FONT_SIZE_CHANGED -> Icons.Default.TextFields
            SORT_ORDER_CHANGED -> Icons.Default.Sort
            COPIED -> Icons.Default.ContentCopy
        }

    val color: Color?
        get() = when (this) {
            SUCCESS -> Color(0xFF4CAF50)
            ERROR -> Color(0xFFF44336)
            WARNING -> Color(0xFFFFC107)
            INFO -> Color(0xFF2196F3)
            OFFLINE -> Color(0xFF757575)
            ONLINE -> Color(0xFF4CAF50)
            LANGUAGE_CHANGED, THEME_CHANGED, FONT_SIZE_CHANGED, SORT_ORDER_CHANGED -> Color(0xFF9C27B0)
            COPIED -> Color(0xFF607D8B)
            else -> null // Use theme color
        }
}
