package com.maralyrics.laitei.presentation.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.maralyrics.laitei.R
import com.maralyrics.laitei.domain.model.SearchInitializationState
import com.maralyrics.laitei.domain.model.SearchResponse
import com.maralyrics.laitei.domain.model.SongLayoutType
import com.maralyrics.laitei.domain.model.SuggestionType
import com.maralyrics.laitei.presentation.common.components.SearchProgressBar
import com.maralyrics.laitei.presentation.common.components.SongListContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSongClick: (Long) -> Unit,
    onArtistClick: (String) -> Unit,
    onComposerClick: (String) -> Unit,
    onFavoritesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    focusSearchOnStart: Boolean = false,
    onSearchFocusConsumed: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val allSongs by viewModel.allSongs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResponse by viewModel.searchResponse.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val layoutType by viewModel.layoutType.collectAsState()
    val initialScrollState by viewModel.initialScrollState.collectAsState()
    val searchInitState by viewModel.searchInitializationState.collectAsState()
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Opened via the Search widget or a similar shortcut — jump straight into typing.
    // The short delay avoids a common Compose/IME race where show() fires before the
    // field's focus has actually attached to the input session.
    LaunchedEffect(focusSearchOnStart) {
        if (focusSearchOnStart) {
            delay(150)
            searchFocusRequester.requestFocus()
            keyboardController?.show()
            onSearchFocusConsumed()
        }
    }

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

    var backPressedOnce by remember { mutableStateOf(false) }

    BackHandler {
        if (backPressedOnce) {
            var currentContext = context
            while (currentContext is android.content.ContextWrapper) {
                if (currentContext is android.app.Activity) {
                    currentContext.finish()
                    return@BackHandler
                }
                currentContext = currentContext.baseContext
            }
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
                    IconButton(onClick = onFavoritesClick) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = stringResource(R.string.favorites_label),
                            tint = Color.Red
                        )
                    }
                    IconButton(onClick = {
                        val nextLayout = if (layoutType == SongLayoutType.NUMBER_TITLE) SongLayoutType.TITLE_BADGE else SongLayoutType.NUMBER_TITLE
                        viewModel.onLayoutTypeChange(nextLayout)
                    }) {
                        Icon(
                            imageVector = if (layoutType == SongLayoutType.NUMBER_TITLE) Icons.Default.FormatListNumbered else Icons.Default.Label,
                            contentDescription = stringResource(R.string.toggle_layout)
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
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
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .focusRequester(searchFocusRequester),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_hint),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.btn_clear))
                            }
                        }
                    },
                    singleLine = true,
                    enabled = searchInitState == SearchInitializationState.READY,
                    shape = MaterialTheme.shapes.large,
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
                    val displaySongs = if (searchQuery.isNotEmpty() && searchResponse is SearchResponse.Results) {
                        (searchResponse as SearchResponse.Results).songs
                    } else if (searchQuery.isNotEmpty()) {
                        emptyList()
                    } else {
                        allSongs
                    }

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
                        listState = listState,
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
