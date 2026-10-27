package com.maralyrics.laitei.presentation.idol

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.maralyrics.laitei.R
import com.maralyrics.laitei.domain.model.IdolContestant
import com.maralyrics.laitei.domain.model.IdolSeason
import com.maralyrics.laitei.presentation.common.components.BannerImage
import com.maralyrics.laitei.presentation.common.components.ListSkeleton
import com.maralyrics.laitei.presentation.common.components.LoadErrorState
import com.maralyrics.laitei.presentation.common.components.OfflineNotice
import com.maralyrics.laitei.presentation.common.components.ProfileImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdolSeasonScreen(
    onBackClick: () -> Unit,
    onArtistClick: (String) -> Unit,
    viewModel: IdolSeasonViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { (uiState as? IdolSeasonUiState.Success)?.let { Text(it.season.title) } },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is IdolSeasonUiState.Loading -> ListSkeleton()
                is IdolSeasonUiState.Error -> LoadErrorState(onRetry = viewModel::load)
                is IdolSeasonUiState.Success -> {
                    if (state.isFromCache) OfflineNotice()
                    SeasonContent(
                        season = state.season,
                        linkableArtistSlugs = state.linkableArtistSlugs,
                        onArtistClick = onArtistClick
                    )
                }
            }
        }
    }
}

@Composable
private fun SeasonContent(
    season: IdolSeason,
    linkableArtistSlugs: Set<String>,
    onArtistClick: (String) -> Unit
) {
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item(key = "overview") {
            Column {
                if (season.coverUrl != null) {
                    BannerImage(
                        url = season.coverUrl,
                        contentDescription = season.title,
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                }
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = season.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    season.venue?.let { InfoLine(Icons.Default.Place, it) }
                    season.dateRange()?.let { InfoLine(Icons.Default.CalendarMonth, it) }
                    (season.description ?: season.summary)?.let {
                        Text(
                            text = it,
                            modifier = Modifier.padding(top = 6.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

        if (season.videos.isNotEmpty()) {
            item(key = "videos_header") { SectionHeader(stringResource(R.string.idol_videos)) }
            itemsIndexed(season.videos, key = { index, video -> "video_$index${video.url}" }) { index, video ->
                ListItem(
                    headlineContent = {
                        Text(video.title ?: stringResource(R.string.idol_video_fallback, index + 1))
                    },
                    leadingContent = {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.clickable { uriHandler.openUri(video.url) }
                )
            }
        }

        item(key = "contestants_header") { SectionHeader(stringResource(R.string.idol_contestants)) }
        if (season.contestants.isEmpty()) {
            item(key = "contestants_empty") {
                Text(
                    text = stringResource(R.string.idol_no_contestants),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            itemsIndexed(season.contestants, key = { index, c -> "contestant_$index${c.name}" }) { _, contestant ->
                val artistSlug = contestant.artistSlug?.takeIf { it in linkableArtistSlugs }
                ContestantRow(
                    contestant = contestant,
                    onClick = artistSlug?.let { slug -> { onArtistClick(slug) } }
                )
            }
        }
    }
}

@Composable
private fun InfoLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ContestantRow(contestant: IdolContestant, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClickLabel = stringResource(R.string.idol_view_artist), onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        ProfileImage(imageUrl = contestant.photoUrl, name = contestant.name, size = 56.dp)
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = contestant.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            ResultBadge(result = contestant.result, modifier = Modifier.padding(top = 4.dp))
            contestant.summary?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.align(Alignment.CenterVertically),
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
