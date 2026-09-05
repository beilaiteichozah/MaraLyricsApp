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
import androidx.glance.appwidget.updateAll
import com.maralyrics.laitei.data.local.dao.SongDao
import com.maralyrics.laitei.presentation.widget.FeaturedLyricsWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts a system (status bar) notification when new songs finish downloading —
 * deliberately scoped to songs only, never for artist/composer/copyright-owner-only
 * changes. This is separate from [NotificationManager], which is the in-app toast
 * system and can't reach the user when a background sync happens with the app closed.
 */
@Singleton
class SongUpdateNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao
) {
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_new_songs),
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_new_songs_desc)
            }
            context.getSystemService(android.app.NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    suspend fun notifyNewSongs(count: Int) {
        if (count <= 0) return

        // Give the Featured Lyrics widget a chance to show something from the new batch,
        // rather than waiting for its own periodic refresh.
        try {
            FeaturedLyricsWidget().updateAll(context)
        } catch (e: Exception) {
            // No widget instances placed on any home screen — nothing to update.
        }

        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val singleSong = if (count == 1) songDao.getMostRecentSongEntity() else null

        val title = context.getString(R.string.notif_new_songs_title)
        val text = if (singleSong != null) {
            context.getString(R.string.notif_new_song_single, singleSong.title)
        } else {
            context.resources.getQuantityString(R.plurals.notif_new_songs_count, count, count)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (singleSong != null) {
                putExtra(MainActivity.EXTRA_OPEN_SONG_ID, singleSong.id)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            PENDING_INTENT_REQUEST_CODE,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setColor(Color.parseColor("#FF0000"))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission was revoked between the areNotificationsEnabled() check and notify().
        }
    }

    companion object {
        private const val CHANNEL_ID = "new_songs"
        private const val NOTIFICATION_ID = 1001
        private const val PENDING_INTENT_REQUEST_CODE = 2001
    }
}
