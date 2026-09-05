package com.maralyrics.laitei.presentation.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.maralyrics.laitei.MainActivity
import com.maralyrics.laitei.R
import androidx.compose.ui.graphics.Color as ComposeColor

/**
 * A tap-to-search shortcut for the home screen — mirrors how Google's own Search
 * widget behaves: tapping it opens the app straight into the search field with the
 * keyboard already up, since a home screen widget can't host live text input itself.
 */
class SearchBarWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            SearchBarContent()
        }
    }

    @Composable
    private fun SearchBarContent() {
        val context = LocalContext.current
        val openSearchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_SEARCH, true)
        }

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(4.dp)
                .background(ComposeColor.White)
                .cornerRadius(28.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clickable(actionStartActivity(openSearchIntent)),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_stat_notification),
                contentDescription = null,
                modifier = GlanceModifier.size(20.dp),
                colorFilter = ColorFilter.tint(ColorProvider(ComposeColor(0xFFE53935)))
            )
            Spacer(modifier = GlanceModifier.width(12.dp))
            Text(
                text = context.getString(R.string.widget_search_hint),
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorProvider(ComposeColor(0xFF757575))
                )
            )
        }
    }
}

class SearchBarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SearchBarWidget()
}
