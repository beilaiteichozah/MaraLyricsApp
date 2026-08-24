package com.maralyrics.laitei.presentation.song_detail

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
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
import com.maralyrics.laitei.R
import com.maralyrics.laitei.domain.model.Song
import com.maralyrics.laitei.presentation.common.components.EmptyState
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val initialScrollPosition by viewModel.initialScrollPosition.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val scrollState = rememberScrollState()

    LaunchedEffect(initialScrollPosition) {
        if (initialScrollPosition > 0) {
            scrollState.scrollTo(initialScrollPosition)
        }
    }

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.value }
            .distinctUntilChanged()
            .collect { position ->
                if (position > 0) {
                    viewModel.onScrollChanged(position)
                }
            }
    }

    var showFeedbackDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (uiState is SongDetailUiState.Success) {
                        Text(
                            text = (uiState as SongDetailUiState.Success).song.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                },
                actions = {
                    if (uiState is SongDetailUiState.Success) {
                        val song = (uiState as SongDetailUiState.Success).song
                        IconButton(onClick = viewModel::toggleFavorite) {
                            Icon(
                                imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = stringResource(R.string.favorites_label),
                                tint = if (song.isFavorite) Color.Red else LocalContentColor.current
                            )
                        }
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(song.lyrics))
                            viewModel.onLyricsCopied()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.btn_copy))
                        }
                        IconButton(onClick = {
                            shareSong(context, song)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.btn_share))
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
                    EmptyState(
                        icon = Icons.Default.ErrorOutline,
                        message = stringResource(R.string.song_not_found)
                    )
                }
                is SongDetailUiState.Success -> {
                    val song = state.song
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        // 1. Full Song Title
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )

                        // 2. Metadata Section (FlowRow)
                        var contributorPopupList by remember { mutableStateOf<List<com.maralyrics.laitei.domain.model.SongContributor>?>(null) }
                        var popupTitle by remember { mutableStateOf("") }
                        var popupType by remember { mutableStateOf(com.maralyrics.laitei.domain.model.ProfileType.ARTIST) }
                        val artistsLabel = stringResource(R.string.stats_artists)
                        val composersLabel = stringResource(R.string.stats_composers)
                        val variousArtistsLabel = stringResource(R.string.various_artists)
                        val variousComposersLabel = stringResource(R.string.various_composers)

                        androidx.compose.foundation.layout.FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            if (song.artists.isNotEmpty()) {
                                AssistChip(
                                    onClick = {
                                        if (song.artists.size > 1) {
                                            contributorPopupList = song.artists
                                            popupTitle = artistsLabel
                                            popupType = com.maralyrics.laitei.domain.model.ProfileType.ARTIST
                                        } else {
                                            song.artists[0].slug.let { onArtistClick(it) }
                                        }
                                    },
                                    label = { Text(if (song.artists.size > 1) variousArtistsLabel else song.artists[0].name) },
                                    leadingIcon = { Icon(Icons.Outlined.Mic, contentDescription = null, Modifier.size(18.dp)) },
                                    shape = MaterialTheme.shapes.small
                                )
                            }
                            
                            if (song.composers.isNotEmpty()) {
                                AssistChip(
                                    onClick = {
                                        if (song.composers.size > 1) {
                                            contributorPopupList = song.composers
                                            popupTitle = composersLabel
                                            popupType = com.maralyrics.laitei.domain.model.ProfileType.COMPOSER
                                        } else {
                                            song.composers[0].slug.let { onComposerClick(it) }
                                        }
                                    },
                                    label = { Text(if (song.composers.size > 1) variousComposersLabel else song.composers[0].name) },
                                    leadingIcon = { Icon(Icons.Outlined.MusicNote, contentDescription = null, Modifier.size(18.dp)) },
                                    shape = MaterialTheme.shapes.small
                                )
                            }

                            if (song.category.isNotBlank()) {
                                AssistChip(
                                    onClick = { onCategoryClick(song.category) },
                                    label = { 
                                        val displayName = when (song.category) {
                                            "Gospel" -> stringResource(R.string.cat_gospel)
                                            "Love" -> stringResource(R.string.cat_love)
                                            "Patriotic" -> stringResource(R.string.cat_patriotic)
                                            "Traditional" -> stringResource(R.string.cat_traditional)
                                            "Uncategorized" -> stringResource(R.string.cat_uncategorized)
                                            else -> song.category
                                        }
                                        Text(displayName) 
                                    },
                                    leadingIcon = { Icon(Icons.Outlined.LocalOffer, contentDescription = null, Modifier.size(18.dp)) },
                                    shape = MaterialTheme.shapes.small
                                )
                            }

                            if (song.views > 0) {
                                AssistChip(
                                    onClick = { },
                                    label = { Text("${song.views} " + stringResource(R.string.views_suffix)) },
                                    leadingIcon = { Icon(Icons.Outlined.Visibility, contentDescription = null, Modifier.size(18.dp)) },
                                    shape = MaterialTheme.shapes.small
                                )
                            }
                        }

                        if (contributorPopupList != null) {
                            ModalBottomSheet(
                                onDismissRequest = { contributorPopupList = null },
                                dragHandle = { BottomSheetDefaults.DragHandle() }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 32.dp)
                                ) {
                                    Text(
                                        text = popupTitle,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(16.dp)
                                    )
                                    contributorPopupList?.forEach { contributor ->
                                        ListItem(
                                            headlineContent = { Text(contributor.name) },
                                            modifier = Modifier.clickable {
                                                contributorPopupList = null
                                                if (popupType == com.maralyrics.laitei.domain.model.ProfileType.ARTIST) {
                                                    onArtistClick(contributor.slug)
                                                } else {
                                                    onComposerClick(contributor.slug)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 3. Lyrics Section
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column {
                                Text(
                                    text = song.lyrics,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = fontSize.sp,
                                        lineHeight = (fontSize * lineSpacing).sp,
                                        letterSpacing = 0.5.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )

                                // Footer with Copyright on the left and buttons on the right
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Copyright Notice (Bottom Left)
                                    if (!song.copyrightOwnerName.isNullOrBlank()) {
                                        Text(
                                            text = stringResource(R.string.copyright_format, song.copyrightOwnerName ?: ""),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.width(1.dp))
                                    }

                                    // Action Button (Report/Feedback)
                                    TextButton(
                                        onClick = { showFeedbackDialog = true },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        ),
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Feedback,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.feedback),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
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
