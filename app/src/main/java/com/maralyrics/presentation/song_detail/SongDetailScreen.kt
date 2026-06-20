package com.maralyrics.presentation.song_detail

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.maralyrics.R
import com.maralyrics.domain.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailScreen(
    onBackClick: () -> Unit,
    onArtistClick: (String) -> Unit,
    onComposerClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    viewModel: SongDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val lineSpacing by viewModel.lineSpacing.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showFeedbackDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (uiState is SongDetailUiState.Success) {
                        Text(
                            text = (uiState as SongDetailUiState.Success).song.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState is SongDetailUiState.Success) {
                        val song = (uiState as SongDetailUiState.Success).song
                        IconButton(onClick = viewModel::toggleFavorite) {
                            Icon(
                                imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (song.isFavorite) Color.Red else LocalContentColor.current
                            )
                        }
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(song.lyrics))
                            viewModel.onLyricsCopied()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                        IconButton(onClick = {
                            shareSong(context, song)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is SongDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is SongDetailUiState.Error -> {
                    Text(
                        text = state.message,
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                is SongDetailUiState.Success -> {
                    val song = state.song
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // 1. Metadata Section (Chips)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AssistChip(
                                onClick = { song.artistSlug?.let { onArtistClick(it) } },
                                label = { Text(song.artistName ?: stringResource(R.string.unknown_artist)) },
                                leadingIcon = { Icon(Icons.Outlined.Mic, contentDescription = null, Modifier.size(18.dp)) },
                                shape = RoundedCornerShape(8.dp)
                            )
                            AssistChip(
                                onClick = { song.composerSlug?.let { onComposerClick(it) } },
                                label = { Text(song.composerName ?: stringResource(R.string.unknown_composer)) },
                                leadingIcon = { Icon(Icons.Outlined.MusicNote, contentDescription = null, Modifier.size(18.dp)) },
                                shape = RoundedCornerShape(8.dp)
                            )
                            AssistChip(
                                onClick = { onCategoryClick(song.category) },
                                label = { Text(song.category.ifBlank { stringResource(R.string.cat_uncategorized) }) },
                                leadingIcon = { Icon(Icons.Outlined.LocalOffer, contentDescription = null, Modifier.size(18.dp)) },
                                shape = RoundedCornerShape(8.dp)
                            )
                            AssistChip(
                                onClick = { },
                                label = { Text("${song.views} views") },
                                leadingIcon = { Icon(Icons.Outlined.Visibility, contentDescription = null, Modifier.size(18.dp)) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 2. Lyrics Section
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = song.lyrics,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = fontSize.sp,
                                    lineHeight = (fontSize * lineSpacing).sp,
                                    letterSpacing = 0.5.sp
                                ),
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(48.dp))
                        
                        // 3. Footer Section
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.padding(bottom = 24.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            
                            if (!song.copyrightOwnerName.isNullOrBlank()) {
                                Text(
                                    text = stringResource(R.string.copyright_format, song.copyrightOwnerName),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            
                            TextButton(
                                onClick = { showFeedbackDialog = true },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Feedback,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.report_issue),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }

    if (showFeedbackDialog) {
        val song = (uiState as? SongDetailUiState.Success)?.song
        if (song != null) {
            ReportBottomSheet(
                song = song,
                onDismiss = { showFeedbackDialog = false },
                onSubmit = { name, email, message ->
                    viewModel.submitFeedback(name, email, message)
                }
            )
        }
    }
}

private fun shareSong(context: Context, song: Song) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "${song.title}\n\n${song.lyrics}")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}
