package com.maralyrics.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maralyrics.R
import com.maralyrics.domain.model.SearchResponse
import com.maralyrics.domain.model.SearchSuggestion
import com.maralyrics.domain.model.Song
import com.maralyrics.domain.model.SongLayoutType
import com.maralyrics.domain.model.SongSortOrder
import com.maralyrics.presentation.home.components.FuzzySuggestionsList
import com.maralyrics.utils.MaraAlphabetUtils
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongListContent(
    songs: List<Song>,
    isLoading: Boolean,
    searchQuery: String,
    sortOrder: SongSortOrder,
    layoutType: SongLayoutType,
    onSortOrderChange: (SongSortOrder) -> Unit,
    onSongClick: (Long) -> Unit,
    onFavoriteClick: (Long) -> Unit,
    searchResponse: SearchResponse = SearchResponse.Empty,
    listState: LazyListState = rememberLazyListState(),
    showAddedSort: Boolean = false,
    onSuggestionClick: (SearchSuggestion) -> Unit = {},
    emptyState: @Composable () -> Unit = {
        if (searchResponse is SearchResponse.Suggestions) {
            FuzzySuggestionsList(
                suggestions = searchResponse.grouped,
                onSuggestionClick = onSuggestionClick
            )
        } else {
            EmptyState(
                icon = Icons.Default.Search,
                message = stringResource(R.string.empty_no_results),
                description = stringResource(R.string.empty_search_desc)
            )
        }
    }
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SortToggleRow(
            currentOrder = sortOrder,
            onOrderChange = onSortOrderChange,
            showAddedSort = showAddedSort
        )

        Box(modifier = Modifier.weight(1f)) {
            if (isLoading) {
                ListSkeleton()
            } else if (songs.isEmpty() && (searchQuery.isBlank() || searchResponse is SearchResponse.Empty || searchResponse is SearchResponse.Suggestions)) {
                emptyState()
            } else if (songs.isNotEmpty()) {
                SongLazyList(
                    songs = songs,
                    sortOrder = sortOrder,
                    layoutType = layoutType,
                    onSongClick = onSongClick,
                    onFavoriteClick = onFavoriteClick,
                    listState = listState
                )
            }

            val isAlphabetical = sortOrder == SongSortOrder.A_Z || sortOrder == SongSortOrder.Z_A
            if (isAlphabetical && searchQuery.isEmpty() && songs.isNotEmpty()) {
                AlphabetScrubber(
                    songs = songs,
                    listState = listState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SortToggleRow(
    currentOrder: SongSortOrder,
    onOrderChange: (SongSortOrder) -> Unit,
    showAddedSort: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SortChip(
            label = if (currentOrder == SongSortOrder.NUMBER_DESC) stringResource(R.string.sort_number_desc) else stringResource(R.string.sort_number_asc),
            selected = currentOrder == SongSortOrder.NUMBER_ASC || currentOrder == SongSortOrder.NUMBER_DESC,
            onClick = {
                onOrderChange(if (currentOrder == SongSortOrder.NUMBER_ASC) SongSortOrder.NUMBER_DESC else SongSortOrder.NUMBER_ASC)
            }
        )
        SortChip(
            label = if (currentOrder == SongSortOrder.Z_A) stringResource(R.string.sort_za) else stringResource(R.string.sort_az),
            selected = currentOrder == SongSortOrder.A_Z || currentOrder == SongSortOrder.Z_A,
            onClick = {
                onOrderChange(if (currentOrder == SongSortOrder.A_Z) SongSortOrder.Z_A else SongSortOrder.A_Z)
            }
        )
        SortChip(
            label = if (currentOrder == SongSortOrder.OLDEST) stringResource(R.string.sort_old_new) else stringResource(R.string.sort_new_old),
            selected = currentOrder == SongSortOrder.NEWEST || currentOrder == SongSortOrder.OLDEST,
            onClick = {
                onOrderChange(if (currentOrder == SongSortOrder.NEWEST) SongSortOrder.OLDEST else SongSortOrder.NEWEST)
            }
        )
        if (showAddedSort) {
            SortChip(
                label = stringResource(R.string.sort_added),
                selected = currentOrder == SongSortOrder.RECENTLY_ADDED,
                onClick = {
                    onOrderChange(SongSortOrder.RECENTLY_ADDED)
                }
            )
        }
    }
}

@Composable
fun SortChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SongLazyList(
    songs: List<Song>,
    sortOrder: SongSortOrder,
    layoutType: SongLayoutType,
    onSongClick: (Long) -> Unit,
    onFavoriteClick: (Long) -> Unit,
    listState: LazyListState,
    enableSwipeToRemove: Boolean = false
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        val isAlphabetical = sortOrder == SongSortOrder.A_Z || sortOrder == SongSortOrder.Z_A
        if (isAlphabetical) {
            val grouped = songs.groupBy { MaraAlphabetUtils.getFirstLetter(it.title) }
            grouped.forEach { (initial, sectionSongs) ->
                stickyHeader {
                    ListHeader(initial)
                }
                items(sectionSongs, key = { it.id }) { song ->
                    SongRowWrapper(
                        song = song,
                        layoutType = layoutType,
                        onSongClick = onSongClick,
                        onFavoriteClick = onFavoriteClick,
                        enableSwipeToRemove = enableSwipeToRemove
                    )
                }
            }
        } else {
            items(songs, key = { it.id }) { song ->
                SongRowWrapper(
                    song = song,
                    layoutType = layoutType,
                    onSongClick = onSongClick,
                    onFavoriteClick = onFavoriteClick,
                    enableSwipeToRemove = enableSwipeToRemove
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongRowWrapper(
    song: Song,
    layoutType: SongLayoutType,
    onSongClick: (Long) -> Unit,
    onFavoriteClick: (Long) -> Unit,
    enableSwipeToRemove: Boolean
) {
    if (enableSwipeToRemove) {
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = {
                if (it == SwipeToDismissBoxValue.EndToStart) {
                    onFavoriteClick(song.id)
                    true
                } else false
            }
        )

        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                val color = when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                    else -> Color.Transparent
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            },
            enableDismissFromStartToEnd = false
        ) {
            Column {
                SongRow(
                    song = song,
                    layoutType = layoutType,
                    onSongClick = onSongClick,
                    onFavoriteClick = onFavoriteClick
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    } else {
        Column {
            SongRow(
                song = song,
                layoutType = layoutType,
                onSongClick = onSongClick,
                onFavoriteClick = onFavoriteClick
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
fun ListHeader(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SongRow(
    song: Song,
    layoutType: SongLayoutType,
    onSongClick: (Long) -> Unit,
    onFavoriteClick: (Long) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSongClick(song.id) }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (layoutType == SongLayoutType.NUMBER_TITLE) {
            Text(
                text = String.format(Locale.US, "%03d", song.id % 1000),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.width(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            val artist = song.artistName ?: song.composerName
            if (!artist.isNullOrBlank()) {
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (layoutType == SongLayoutType.TITLE_BADGE) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "%03d", song.id % 1000),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(
            onClick = { onFavoriteClick(song.id) },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = if (song.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = stringResource(R.string.favorites_label),
                tint = if (song.isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun AlphabetScrubber(
    songs: List<Song>,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val alphabet = songs.map { MaraAlphabetUtils.getFirstLetter(it.title) }.distinct()
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                RoundedCornerShape(16.dp)
            )
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val y = change.position.y
                    val itemHeight = size.height / alphabet.size
                    val index = (y / itemHeight).toInt().coerceIn(0, alphabet.size - 1)
                    val targetChar = alphabet[index]
                    
                    val targetIndex = findFirstIndexForChar(songs, targetChar)
                    if (targetIndex != -1) {
                        scope.launch {
                            listState.scrollToItem(targetIndex)
                        }
                    }
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        alphabet.forEach { char ->
            Text(
                text = char,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(vertical = 1.dp)
            )
        }
    }
}

private fun findFirstIndexForChar(songs: List<Song>, char: String): Int {
    val grouped = songs.groupBy { MaraAlphabetUtils.getFirstLetter(it.title) }
    var currentIndex = 0
    for ((initial, sectionSongs) in grouped) {
        if (initial == char) return currentIndex
        currentIndex += sectionSongs.size + 1 // +1 for header
    }
    return -1
}
