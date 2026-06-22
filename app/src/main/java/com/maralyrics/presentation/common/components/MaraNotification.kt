package com.maralyrics.presentation.common.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maralyrics.R
import com.maralyrics.presentation.common.notification.NotificationData

@Composable
fun MaraNotification(data: NotificationData) {
    val translatedMessage = if (data.message.contains("|")) {
        val parts = data.message.split("|")
        val resId = getResId(parts[0])
        if (resId != 0) stringResource(resId, parts[1]) else data.message
    } else {
        val resId = getResId(data.message)
        if (resId != 0) stringResource(resId) else data.message
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconColor = data.type.color ?: MaterialTheme.colorScheme.primary
            
            Icon(
                imageVector = data.type.icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = translatedMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.25.sp,
                        lineHeight = 20.sp
                    )
                )
                if (data.showProgress) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = iconColor,
                        trackColor = iconColor.copy(alpha = 0.2f),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
fun getResId(key: String): Int {
    return when (key) {
        "notif_offline" -> R.string.notif_offline
        "notif_online" -> R.string.notif_online
        "notif_fav_added" -> R.string.notif_fav_added
        "notif_fav_removed" -> R.string.notif_fav_removed
        "notif_lang_changed" -> R.string.notif_lang_changed
        "notif_theme_enabled" -> R.string.notif_theme_enabled
        "notif_mode_light" -> R.string.notif_mode_light
        "notif_mode_dark" -> R.string.notif_mode_dark
        "notif_mode_system" -> R.string.notif_mode_system
        "notif_default_cat_updated" -> R.string.notif_default_cat_updated
        "notif_color_theme_updated" -> R.string.notif_color_theme_updated
        "notif_cache_cleared" -> R.string.notif_cache_cleared
        "notif_fav_backup_success" -> R.string.notif_fav_backup_success
        "notif_fav_backup_failed" -> R.string.notif_fav_backup_failed
        "notif_fav_restore_success" -> R.string.notif_fav_restore_success
        "notif_fav_restore_no_file" -> R.string.notif_fav_restore_no_file
        "notif_fav_restore_failed" -> R.string.notif_fav_restore_failed
        "notif_copied" -> R.string.notif_copied
        "notif_sync_started" -> R.string.notif_sync_started
        "notif_sync_success" -> R.string.notif_sync_success
        "notif_sync_failed" -> R.string.notif_sync_failed
        "sync_downloading_db" -> R.string.sync_downloading_db
        "sync_download_success" -> R.string.sync_download_success
        "sync_download_failed" -> R.string.sync_download_failed
        "sync_redownloading" -> R.string.sync_redownloading
        "exit_press_back" -> R.string.exit_press_back
        else -> 0
    }
}
