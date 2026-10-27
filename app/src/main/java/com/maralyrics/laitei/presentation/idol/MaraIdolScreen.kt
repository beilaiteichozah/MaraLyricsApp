package com.maralyrics.laitei.presentation.idol

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.maralyrics.laitei.R
import com.maralyrics.laitei.domain.model.IdolSeason
import com.maralyrics.laitei.presentation.common.components.BannerImage
import com.maralyrics.laitei.presentation.common.components.EmptyState
import com.maralyrics.laitei.presentation.common.components.ListSkeleton
import com.maralyrics.laitei.presentation.common.components.LoadErrorState
import com.maralyrics.laitei.presentation.common.components.OfflineNotice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaraIdolScreen(
    onBackClick: () -> Unit,
    onSeasonClick: (String) -> Unit,
    viewModel: MaraIdolViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.idol_title)) },
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
                is MaraIdolUiState.Loading -> ListSkeleton()
                is MaraIdolUiState.Error -> LoadErrorState(onRetry = viewModel::load, icon = Icons.Default.EmojiEvents)
                is MaraIdolUiState.Success -> {
                    if (state.isFromCache) OfflineNotice()
                    if (state.seasons.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.EmojiEvents,
                            message = stringResource(R.string.idol_empty_title),
                            description = stringResource(R.string.idol_empty_desc)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.seasons, key = { it.slug }) { season ->
                                SeasonCard(season = season, onClick = { onSeasonClick(season.slug) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonCard(season: IdolSeason, onClick: () -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large
    ) {
        Column {
            if (season.coverUrl != null) {
                BannerImage(
                    url = season.coverUrl,
                    contentDescription = season.title,
                    modifier = Modifier.fillMaxWidth().height(170.dp)
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = season.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                val meta = listOfNotNull(
                    season.metaLine().ifEmpty { null },
                    season.contestantCount.takeIf { it > 0 }
                        ?.let { pluralStringResource(R.plurals.idol_contestant_count, it, it) }
                ).joinToString(" • ")
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        modifier = Modifier.padding(top = 2.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (season.summary != null) {
                    Text(
                        text = season.summary,
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (season.winners.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = stringResource(R.string.idol_result_winner),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = season.winners.joinToString { it.name },
                            modifier = Modifier.padding(start = 6.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
