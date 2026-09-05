package com.maralyrics.laitei.presentation.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.maralyrics.laitei.MainActivity
import com.maralyrics.laitei.R
import com.maralyrics.laitei.data.mapper.toDomain
import com.maralyrics.laitei.di.WidgetEntryPoint
import com.maralyrics.laitei.domain.model.Song
import dagger.hilt.android.EntryPointAccessors
import androidx.compose.ui.graphics.Color as ComposeColor

/**
 * Shows a random song from the local library, refreshed periodically (every 6 hours,
 * per the OS-driven update cycle in its AppWidgetProviderInfo) and on demand via the
 * shuffle button, or whenever a background sync brings in new songs.
 */
class FeaturedLyricsWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val songDao = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).songDao()
        val song = songDao.getRandomSongEntity()?.toDomain()

        provideContent {
            FeaturedLyricsContent(song)
        }
    }

    @Composable
    private fun FeaturedLyricsContent(song: Song?) {
        val context = LocalContext.current

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ComposeColor(0xFFFFF5F5))
                .cornerRadius(20.dp)
                .padding(16.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_stat_notification),
                    contentDescription = null,
                    modifier = GlanceModifier.size(16.dp),
                    colorFilter = androidx.glance.ColorFilter.tint(ColorProvider(ComposeColor(0xFFE53935)))
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = context.getString(R.string.widget_featured_title),
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(ComposeColor(0xFFE53935))
                    )
                )
                Box(modifier = GlanceModifier.defaultWeight()) {}
                Box(
                    modifier = GlanceModifier
                        .size(28.dp)
                        .cornerRadius(14.dp)
                        .background(ComposeColor.White)
                        .clickable(actionRunCallback<RefreshFeaturedLyricsAction>()),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(android.R.drawable.ic_popup_sync),
                        contentDescription = context.getString(R.string.widget_refresh),
                        modifier = GlanceModifier.size(16.dp),
                        colorFilter = androidx.glance.ColorFilter.tint(ColorProvider(ComposeColor(0xFF757575)))
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            if (song == null) {
                Text(
                    text = context.getString(R.string.widget_no_songs),
                    style = TextStyle(fontSize = 13.sp, color = ColorProvider(ComposeColor(0xFF757575)))
                )
            } else {
                val openSongIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(MainActivity.EXTRA_OPEN_SONG_ID, song.id)
                }
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .clickable(actionStartActivity(openSongIntent))
                ) {
                    Text(
                        text = song.title,
                        maxLines = 1,
                        style = TextStyle(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(ComposeColor(0xFF1A1A1A))
                        )
                    )
                    if (!song.artistName.isNullOrBlank()) {
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = song.artistName,
                            maxLines = 1,
                            style = TextStyle(fontSize = 13.sp, color = ColorProvider(ComposeColor(0xFF757575)))
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    // Full lyrics, not just an excerpt — the widget's own bounds (fixed
                    // or user-resized) naturally clip whatever doesn't fit, so this shows
                    // as much of the song as the widget's current height allows.
                    Box(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = song.lyrics,
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = ColorProvider(ComposeColor(0xFF424242)),
                                textAlign = TextAlign.Start
                            )
                        )
                    }
                }
            }
        }
    }
}

class RefreshFeaturedLyricsAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        FeaturedLyricsWidget().update(context, glanceId)
    }
}

class FeaturedLyricsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FeaturedLyricsWidget()
}
