package com.maralyrics.laitei.presentation.song_detail

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.maralyrics.laitei.R
import com.maralyrics.laitei.domain.model.Song
import com.maralyrics.laitei.presentation.common.components.EmptyState
import com.maralyrics.laitei.utils.SiteLink
import com.maralyrics.laitei.utils.findActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt

// How long the floating reading controls stay up after the last tap on them.
private const val CONTROLS_HIDE_DELAY_MS = 3000L

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SongDetailScreen(
    onBackClick: () -> Unit,
    onArtistClick: (String) -> Unit,
    onComposerClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    // Previous/Next change the song on screen without a navigation, so the host is told to keep
    // "resume where I left off" pointing at the song actually being read.
    onCurrentSongChanged: (Long) -> Unit = {},
    viewModel: SongDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val neighbors by viewModel.neighbors.collectAsState()
    val zoom by viewModel.zoom.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val lineSpacing by viewModel.lineSpacing.collectAsState()
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()
    val initialScrollPosition by viewModel.initialScrollPosition.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val view = LocalView.current

    DisposableEffect(keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    // --- Reading controls: tap the lyrics to show/hide them ---------------------------------
    // Saveable, so rotating the screen (which rebuilds the activity) doesn't undo what was just chosen.
    var controlsVisible by rememberSaveable { mutableStateOf(false) }
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    var orientationOverridden by rememberSaveable { mutableStateOf(false) }
    // Bumped by every press on a control so the auto-hide countdown starts over.
    var interactionTick by remember { mutableIntStateOf(0) }
    val activity = remember(context) { context.findActivity() }
    val configuration = LocalConfiguration.current
    val toggleControlsLabel = stringResource(R.string.song_toggle_controls)

    LaunchedEffect(controlsVisible, interactionTick) {
        if (controlsVisible) {
            delay(CONTROLS_HIDE_DELAY_MS)
            controlsVisible = false
        }
    }

    LaunchedEffect(fullscreen, activity) {
        val window = activity?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        if (fullscreen) {
            // A swipe from the screen edge shows the bars briefly, then they tuck away again.
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Fullscreen and a forced orientation belong to the song view: put both back when it closes.
    // Rotation itself doesn't rebuild the activity (see configChanges in the manifest), but other
    // changes such as a theme or language switch still do, and then this is only a rebuild.
    DisposableEffect(activity) {
        onDispose {
            if (activity != null && !activity.isChangingConfigurations) {
                if (orientationOverridden) {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
                WindowCompat.getInsetsController(activity.window, view).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val scrollState = rememberScrollState()

    // Paging replaces the song in place, so the new one has to start at the top.
    val shownSongId = (uiState as? SongDetailUiState.Success)?.song?.id
    var lastShownSongId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(shownSongId) {
        if (shownSongId == null) return@LaunchedEffect
        if (lastShownSongId != null && lastShownSongId != shownSongId) {
            scrollState.scrollTo(0)
        }
        lastShownSongId = shownSongId
        onCurrentSongChanged(shownSongId)
    }

    // Changing the text size re-flows the lyrics, which would throw the reader to a different
    // part of the song. Scale the scroll offset by the same ratio to stay roughly where they were.
    // The zoom the offset was last matched to is tracked separately from the live zoom: a pinch
    // changes it many times a second, and a pass that gets cancelled mid-way leaves the next one
    // to make up the whole difference.
    var anchoredZoom by remember { mutableFloatStateOf(zoom) }
    LaunchedEffect(zoom) {
        if (zoom == anchoredZoom) return@LaunchedEffect
        withFrameNanos { } // let the lyrics lay out at the new size first, so the scroll range is current
        scrollState.scrollTo((scrollState.value * (zoom / anchoredZoom)).roundToInt())
        anchoredZoom = zoom
    }

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
            AnimatedVisibility(
                visible = !fullscreen,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
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
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                // Two fingers pinch the lyrics bigger or smaller; one finger is left alone.
                .pinchToZoom(viewModel::zoomBy)
                // A plain tap anywhere on the lyrics shows or hides the reading controls. Taps on
                // chips and buttons are consumed by those, and scrolling is untouched.
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { controlsVisible = !controlsVisible })
                }
                // Same action for TalkBack, which can't perform the raw tap above.
                .semantics {
                    onClick(label = toggleControlsLabel) {
                        controlsVisible = !controlsVisible
                        true
                    }
                }
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
                                        fontSize = (fontSize * zoom).sp,
                                        lineHeight = (fontSize * zoom * lineSpacing).sp,
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
                        
                        Spacer(modifier = Modifier.height(24.dp))

                        // 4. Tagline
                        Text(
                            text = stringResource(R.string.tagline_made_in_maraland),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily(
                                    androidx.compose.ui.text.font.Font(R.font.cmu_serif_slanted)
                                ),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            (uiState as? SongDetailUiState.Success)?.song?.let { song ->
                val slideFromBottom = fadeIn() + slideInVertically { it / 2 }
                val slideToBottom = fadeOut() + slideOutVertically { it / 2 }
                val slideFromTop = fadeIn() + slideInVertically { -it / 2 }
                val slideToTop = fadeOut() + slideOutVertically { -it / 2 }

                AnimatedVisibility(
                    visible = controlsVisible,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = slideFromBottom,
                    exit = slideToBottom
                ) {
                    SongBottomControls(
                        neighbors = neighbors,
                        isFullscreen = fullscreen,
                        zoom = zoom,
                        onZoomIn = {
                            viewModel.zoomIn()
                            interactionTick++
                        },
                        onZoomOut = {
                            viewModel.zoomOut()
                            interactionTick++
                        },
                        onZoomReset = {
                            viewModel.resetZoom()
                            interactionTick++
                        },
                        onPrevious = {
                            viewModel.openPrevious()
                            interactionTick++
                        },
                        onNext = {
                            viewModel.openNext()
                            interactionTick++
                        },
                        onRotate = {
                            val inLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE                            // SENSOR_* rather than USER_*: they turn the screen even when the phone's
                            // auto-rotate is off, and are public on every Android version this app supports.
                            activity?.requestedOrientation = if (inLandscape) {
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            }
                            orientationOverridden = true
                            interactionTick++
                        },
                        onToggleFullscreen = {
                            fullscreen = !fullscreen
                            interactionTick++
                        }
                    )
                }

                // Nothing to show a position in when the song is the only one in its list.
                neighbors?.takeIf { it.total > 1 }?.let { position ->
                    AnimatedVisibility(
                        visible = controlsVisible,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
                        enter = slideFromTop,
                        exit = slideToTop
                    ) {
                        SongPositionPill(position)
                    }
                }

                AnimatedVisibility(
                    visible = controlsVisible && fullscreen,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 16.dp),
                    enter = slideFromTop,
                    exit = slideToTop
                ) {
                    FloatingFavoriteButton(
                        isFavorite = song.isFavorite,
                        onClick = {
                            viewModel.toggleFavorite()
                            interactionTick++
                        }
                    )
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

/**
 * Shares just a link to the song on the website — no title, no lyrics — so the message is a single
 * clean, tappable URL: for someone with the app it opens that song in the app, for everyone else
 * it opens the song's page in the browser. (Copy, next to Share, still puts the lyrics on the
 * clipboard.)
 */
private fun shareSong(context: Context, song: Song) {
    val message = if (song.slug.isBlank()) {
        // No slug means no page to link to; send the words rather than a dead link.
        "${song.title}\n\n${song.lyrics}"
    } else {
        SiteLink.songUrl(song.slug)
    }
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, message)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}
