package com.maralyrics.laitei.presentation.common.notification

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maralyrics.laitei.presentation.common.components.MaraNotification
import kotlinx.coroutines.delay

@Composable
fun NotificationHost(
    manager: NotificationManager,
    modifier: Modifier = Modifier
) {
    var currentNotification by remember { mutableStateOf<NotificationData?>(null) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        manager.notifications.collect { notification ->
            if (visible) {
                visible = false
                delay(300) // Wait for exit animation
            }
            currentNotification = notification
            visible = true
            delay(notification.durationMillis)
            visible = false
            delay(400) // Wait for exit animation
            currentNotification = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 80.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            currentNotification?.let { data ->
                MaraNotification(data)
            }
        }
    }
}
