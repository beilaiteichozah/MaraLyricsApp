package com.maralyrics.laitei.presentation.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maralyrics.laitei.R
import com.maralyrics.laitei.domain.model.Song
import java.util.Locale

/**
 * Sits above the song list on Home: today's pick from the website, then shortcuts to the
 * sections that aren't songs. Scrolls away with the list so it never costs a small screen
 * its song rows.
 */
@Composable
fun HomeHeader(
    songOfTheDay: Song?,
    onSongClick: (Long) -> Unit,
    onArticlesClick: () -> Unit,
    onMaraIdolClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onPeopleClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (songOfTheDay != null) {
            SongOfTheDayCard(
                song = songOfTheDay,
                onClick = { onSongClick(songOfTheDay.id) },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
            )
        }
        ExploreRow(
            onArticlesClick = onArticlesClick,
            onMaraIdolClick = onMaraIdolClick,
            onCalendarClick = onCalendarClick,
            onPeopleClick = onPeopleClick
        )
    }
}

@Composable
private fun SongOfTheDayCard(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val variousArtists = stringResource(R.string.various_artists)
    val byline = remember(song.artists, song.artistName) {
        when {
            song.artists.size > 1 -> variousArtists
            else -> song.artists.firstOrNull()?.name ?: song.artistName
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${stringResource(R.string.song_of_the_day)} · ${stringResource(R.string.song_of_the_day_badge)}",
                    modifier = Modifier.padding(start = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!byline.isNullOrBlank()) {
                        Text(
                            text = byline,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = String.format(Locale.US, "%03d", song.id % 1000),
                    modifier = Modifier.padding(start = 12.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}

@Composable
private fun ExploreRow(
    onArticlesClick: () -> Unit,
    onMaraIdolClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onPeopleClick: () -> Unit
) {
    // Scrollable so large font sizes or translated labels never wrap or clip a chip.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ExploreChip(stringResource(R.string.explore_articles), Icons.Default.Newspaper, onArticlesClick)
        ExploreChip(stringResource(R.string.explore_mara_idol), Icons.Default.EmojiEvents, onMaraIdolClick)
        ExploreChip(stringResource(R.string.explore_calendar), Icons.Default.CalendarMonth, onCalendarClick)
        ExploreChip(stringResource(R.string.explore_people), Icons.Default.Groups, onPeopleClick)
    }
}

@Composable
private fun ExploreChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        leadingIcon = {
            Icon(icon, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
        },
        shape = MaterialTheme.shapes.medium
    )
}
