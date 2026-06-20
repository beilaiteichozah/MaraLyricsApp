package com.maralyrics.presentation.common.notification

import java.util.UUID

data class NotificationData(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val type: NotificationType,
    val durationMillis: Long = 3000L,
    val showProgress: Boolean = false
)
