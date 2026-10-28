package com.maralyrics.laitei.presentation.common.notification

import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.maralyrics.laitei.MainActivity
import com.maralyrics.laitei.R
import com.maralyrics.laitei.domain.model.CommunityEvent
import com.maralyrics.laitei.domain.model.EventMoment
import com.maralyrics.laitei.domain.model.EventReminders
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts the "event today" system notification. Separate from [SongUpdateNotifier] (its own
 * channel, so it can be silenced on its own) and from the in-app toasts. Remembers which events it
 * has announced so a re-run the same day, or a restart, never repeats one.
 */
@Singleton
class EventReminderNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_events),
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_events_desc)
            }
            context.getSystemService(android.app.NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    /** Announces whichever of [events] are on [today] and haven't been announced yet. */
    fun notifyToday(today: EventMoment, events: List<CommunityEvent>) {
        val announced = EventReminders.pruned(today, prefs.getStringSet(KEY_ANNOUNCED, emptySet()).orEmpty())
        val due = EventReminders.unannounced(today, events, announced)
        if (due.isEmpty()) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val title = if (due.size == 1) {
            context.getString(R.string.notif_event_today_title)
        } else {
            context.resources.getQuantityString(R.plurals.notif_events_today_count, due.size, due.size)
        }
        val text = due.joinToString(", ") { it.title }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_CALENDAR, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            PENDING_INTENT_REQUEST_CODE,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val inbox = NotificationCompat.InboxStyle().setBigContentTitle(title)
        due.forEach { inbox.addLine(it.title) }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setColor(Color.parseColor("#FF0000"))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(if (due.size == 1) NotificationCompat.BigTextStyle().bigText(text) else inbox)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            // Only remembered once it was actually posted, so a failure is retried on the next run.
            prefs.edit()
                .putStringSet(KEY_ANNOUNCED, announced + due.map { EventReminders.announcementKey(today, it) })
                .apply()
        } catch (e: SecurityException) {
            // Permission was revoked between the check and notify().
        }
    }

    private companion object {
        const val PREFS = "event_reminder_log"
        const val KEY_ANNOUNCED = "announced"
        const val CHANNEL_ID = "community_events"
        const val NOTIFICATION_ID = 1002
        const val PENDING_INTENT_REQUEST_CODE = 2002
    }
}
