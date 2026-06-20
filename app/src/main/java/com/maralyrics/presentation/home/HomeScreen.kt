package com.maralyrics.presentation.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.maralyrics.R
import com.maralyrics.domain.model.SearchInitializationState
import com.maralyrics.domain.model.SongLayoutType
import com.maralyrics.domain.model.SuggestionType
import com.maralyrics.presentation.common.components.SearchProgressBar
import com.maralyrics.presentation.common.components.SongListContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSongClick: (Long) -> Unit,
    onArtistClick: (String) -> Unit,
    onComposerClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val allSongs by viewModel.allSongs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResponse by viewModel.searchResponse.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val layoutType by viewModel.layoutType.collectAsState()
    val searchInitState by viewModel.searchInitializationState.collectAsState()
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var backPressedOnce by remember { mutableStateOf(false) }

    BackHandler {
        if (backPressedOnce) {
            (context as? android.app.Activity)?.finish()
        } else {
            backPressedOnce = true
            scope.launch {
                viewModel.onBackPressed()
                delay(2000)
                backPressedOnce = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.mara_lyrics_logo),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.home_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val nextLayout = if (layoutType == SongLayoutType.NUMBER_TITLE) SongLayoutType.TITLE_BADGE else SongLayoutType.NUMBER_TITLE
                        viewModel.onLayoutTypeChange(nextLayout)
                    }) {
                        Icon(
                            imageVector = if (layoutType == SongLayoutType.NUMBER_TITLE) Icons.Default.FormatListNumbered else Icons.Default.Label,
                            contentDescription = "Toggle Layout"
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    enabled = searchInitState == SearchInitializationState.READY,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    val displaySongs = if (searchQuery.isNotEmpty()) searchResults else allSongs

                    SongListContent(
                        songs = displaySongs,
                        isLoading = (allSongs.isEmpty() && isRefreshing),
                        searchQuery = searchQuery,
                        sortOrder = sortOrder,
                        layoutType = layoutType,
                        onSortOrderChange = viewModel::onSortOrderChange,
                        onSongClick = onSongClick,
                        onFavoriteClick = viewModel::toggleFavorite,
                        searchResponse = searchResponse,
                        onSuggestionClick = { suggestion ->
                            when (suggestion.type) {
                                SuggestionType.SONG -> onSongClick(suggestion.id)
                                SuggestionType.ARTIST -> onArtistClick(suggestion.slug)
                                SuggestionType.COMPOSER -> onComposerClick(suggestion.slug)
                            }
                        }
                    )
                }
            }

            if (searchInitState != SearchInitializationState.READY) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        SearchProgressBar(state = searchInitState)
                    }
                }
            }
        }
    }
}
