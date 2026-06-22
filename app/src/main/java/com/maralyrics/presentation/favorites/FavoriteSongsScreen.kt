package com.maralyrics.presentation.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.maralyrics.R
import com.maralyrics.domain.model.FavoriteStats
import com.maralyrics.domain.model.SongLayoutType
import com.maralyrics.presentation.common.components.EmptyState
import com.maralyrics.presentation.common.components.SongLazyList
import com.maralyrics.presentation.common.components.SortToggleRow
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteSongsScreen(
    onBackClick: () -> Unit,
    onSongClick: (Long) -> Unit,
    onBrowseClick: () -> Unit,
    viewModel: FavoriteSongsViewModel = hiltViewModel()
) {
    val songs by viewModel.songs.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val categories by viewModel.availableCategories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val layoutType by viewModel.layoutType.collectAsState()
    val initialScrollState by viewModel.initialScrollState.collectAsState()

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(initialScrollState) {
        if (initialScrollState.first > 0 || initialScrollState.second > 0) {
            listState.scrollToItem(initialScrollState.first, initialScrollState.second)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { pair ->
                viewModel.onScrollStateChanged(pair.first, pair.second)
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.favorite_songs)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.getSurpriseSong(onSongClick) }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = stringResource(R.string.surprise_me))
                    }
                    IconButton(onClick = {
                        val nextLayout = if (layoutType == SongLayoutType.NUMBER_TITLE) SongLayoutType.TITLE_BADGE else SongLayoutType.NUMBER_TITLE
                        viewModel.onLayoutTypeChange(nextLayout)
                    }) {
                        Icon(
                            imageVector = if (layoutType == SongLayoutType.NUMBER_TITLE) Icons.Default.FormatListNumbered else Icons.AutoMirrored.Filled.Label,
                            contentDescription = stringResource(R.string.toggle_layout)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            FavoriteStatsHeader(stats)
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.search_favorites)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.btn_clear))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            if (categories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { viewModel.onCategorySelect(category) },
                            label = { 
                                val displayName = when (category) {
                                    "Gospel" -> stringResource(R.string.cat_gospel)
                                    "Love" -> stringResource(R.string.cat_love)
                                    "Patriotic" -> stringResource(R.string.cat_patriotic)
                                    "Traditional" -> stringResource(R.string.cat_traditional)
                                    "Uncategorized" -> stringResource(R.string.cat_uncategorized)
                                    else -> category
                                }
                                Text(displayName) 
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            SortToggleRow(
                currentOrder = sortOrder,
                onOrderChange = viewModel::onSortOrderChange,
                showAddedSort = true
            )

            Box(modifier = Modifier.weight(1f)) {
                if (songs.isEmpty()) {
                    if (searchQuery.isEmpty() && selectedCategory == null) {
                        EmptyFavoritesState(onBrowseClick, { viewModel.getSurpriseSong(onSongClick) })
                    } else {
                        EmptyState(
                            icon = Icons.Default.SearchOff,
                            message = stringResource(R.string.no_matching_favorites),
                            description = stringResource(R.string.adjust_search_filters)
                        )
                    }
                } else {
                    val listState = rememberLazyListState()
                    SongLazyList(
                        songs = songs,
                        sortOrder = sortOrder,
                        layoutType = layoutType,
                        onSongClick = onSongClick,
                        onFavoriteClick = viewModel::toggleFavorite,
                        listState = listState,
                        enableSwipeToRemove = true
                    )
                }
            }
        }
    }
}

@Composable
fun FavoriteStatsHeader(stats: FavoriteStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(label = stringResource(R.string.stats_favorites), count = stats.songCount)
        StatItem(label = stringResource(R.string.stats_artists), count = stats.artistCount)
        StatItem(label = stringResource(R.string.stats_composers), count = stats.composerCount)
    }
}

@Composable
fun StatItem(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyFavoritesState(onBrowseClick: () -> Unit, onSurpriseClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FavoriteBorder,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.no_favorites),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.no_favorites_desc),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onBrowseClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Explore, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.browse_songs))
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onSurpriseClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.discover_random))
        }
    }
}
