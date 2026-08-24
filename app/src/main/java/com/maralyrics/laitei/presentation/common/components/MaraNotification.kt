package com.maralyrics.laitei.presentation.common.components

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
import com.maralyrics.laitei.R
import com.maralyrics.laitei.presentation.common.notification.NotificationData

@Composable
fun MaraNotification(data: NotificationData) {
    val translatedMessage = if (data.message.contains("|")) {
        val parts = data.message.split("|")
        val templateResId = getResId(parts[0])
        if (templateResId != 0) {
            val argParts = parts[1].split(",")
            val resolvedArgs = mutableListOf<Any>()
            for (part in argParts) {
                val trimmed = part.trim()
                val argResId = getResId(trimmed)
                if (argResId != 0) {
                    resolvedArgs.add(stringResource(argResId))
                } else {
                    val number = trimmed.toLongOrNull()
                    resolvedArgs.add(number ?: trimmed)
                }
            }
            stringResource(templateResId, *resolvedArgs.toTypedArray())
        } else {
            data.message
        }
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
        "low_storage_error" -> R.string.low_storage_error
        "exit_press_back" -> R.string.exit_press_back
        "feedback_success" -> R.string.feedback_success
        "feedback_saved" -> R.string.feedback_saved
        "lang_mara" -> R.string.lang_mara
        "lang_english" -> R.string.lang_english
        "lang_burmese" -> R.string.lang_burmese
        "color_mara" -> R.string.color_mara
        "color_ocean" -> R.string.color_ocean
        "color_emerald" -> R.string.color_emerald
        "color_sunset" -> R.string.color_sunset
        "color_purple" -> R.string.color_purple
        "color_rose" -> R.string.color_rose
        "color_slate" -> R.string.color_slate
        "cat_all" -> R.string.cat_all
        "cat_gospel" -> R.string.cat_gospel
        "cat_love" -> R.string.cat_love
        "cat_patriotic" -> R.string.cat_patriotic
        "cat_traditional" -> R.string.cat_traditional
        "cat_uncategorized" -> R.string.cat_uncategorized
        else -> 0
    }
}
