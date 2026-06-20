package com.maralyrics.presentation.category

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.maralyrics.domain.model.SongLayoutType
import com.maralyrics.presentation.common.components.SongListContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySongsScreen(
    onBackClick: () -> Unit,
    onSongClick: (Long) -> Unit,
    viewModel: CategorySongsViewModel = hiltViewModel()
) {
    val songs by viewModel.songs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val layoutType by viewModel.layoutType.collectAsState()
    val category = viewModel.category

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(category.replaceFirstChar { it.uppercase() }) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            SongListContent(
                songs = songs,
                isLoading = isLoading,
                searchQuery = "",
                sortOrder = sortOrder,
                layoutType = layoutType,
                onSortOrderChange = viewModel::onSortOrderChange,
                onSongClick = onSongClick,
                onFavoriteClick = viewModel::toggleFavorite
            )
        }
    }
}
