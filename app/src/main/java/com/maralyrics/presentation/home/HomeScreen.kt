package com.maralyrics.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.maralyrics.R
import com.maralyrics.domain.model.Song
import com.maralyrics.domain.model.SongCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSongClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.homeState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            if (searchQuery.length >= 2) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(searchResults) { song ->
                        SongItem(song = song, onClick = { onSongClick(song.id) })
                    }
                }
            } else if (state == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (state?.recentlyViewed?.isNotEmpty() == true) {
                        item {
                            SongSection(
                                title = stringResource(R.string.recently_viewed),
                                songs = state?.recentlyViewed ?: emptyList(),
                                onSongClick = onSongClick
                            )
                        }
                    }

                    if (state?.favorites?.isNotEmpty() == true) {
                        item {
                            SongSection(
                                title = stringResource(R.string.favorite_songs),
                                songs = state?.favorites ?: emptyList(),
                                onSongClick = onSongClick
                            )
                        }
                    }

                    item {
                        SongSection(
                            title = stringResource(R.string.popular_songs),
                            songs = state?.popular ?: emptyList(),
                            onSongClick = onSongClick
                        )
                    }

                    item {
                        SongSection(
                            title = stringResource(R.string.random_suggestions),
                            songs = state?.random ?: emptyList(),
                            onSongClick = onSongClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SongSection(
    title: String,
    songs: List<Song>,
    onSongClick: (Long) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(songs) { song ->
                SongCard(song = song, onClick = { onSongClick(song.id) })
            }
        }
    }
}

@Composable
fun SongCard(song: Song, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.song_number, song.songNumber),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun SongItem(song: Song, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(song.title) },
        supportingContent = { Text(stringResource(R.string.song_number, song.songNumber)) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
