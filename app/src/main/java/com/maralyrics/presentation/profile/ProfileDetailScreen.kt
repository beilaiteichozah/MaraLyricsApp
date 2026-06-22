package com.maralyrics.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.maralyrics.R
import com.maralyrics.domain.model.Profile
import com.maralyrics.domain.model.Song
import com.maralyrics.presentation.common.components.DynamicSocialLinks
import com.maralyrics.presentation.common.components.ListSkeleton
import com.maralyrics.presentation.common.components.ProfileImage
import com.maralyrics.presentation.common.components.ProfileSongCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailScreen(
    onBackClick: () -> Unit,
    onSongClick: (Long) -> Unit,
    viewModel: ProfileDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (uiState is ProfileUiState.Success) {
                        Text((uiState as ProfileUiState.Success).profile.name)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> ListSkeleton()
                is ProfileUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, textAlign = TextAlign.Center)
                    }
                }
                is ProfileUiState.Success -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = viewModel::refresh
                    ) {
                        ProfileContent(state.profile, state.songs, onSongClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    profile: Profile,
    songs: List<Song>,
    onSongClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Hero Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ProfileImage(
                    imageUrl = profile.imageUrl,
                    name = profile.name,
                    size = 140.dp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Type Badge
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (profile.type == com.maralyrics.domain.model.ProfileType.ARTIST) 
                            stringResource(R.string.artist) else stringResource(R.string.composer),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                profile.bio?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Dynamic Social Links
                DynamicSocialLinks(links = profile.socialLinks)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Song Count
                Text(
                    text = stringResource(R.string.song_count_format, profile.songCount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // Song List Section
        item {
            Text(
                text = stringResource(R.string.songs_by_format, profile.name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        items(songs) { song ->
            ProfileSongCard(
                song = song,
                onClick = { onSongClick(song.id) }
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}
