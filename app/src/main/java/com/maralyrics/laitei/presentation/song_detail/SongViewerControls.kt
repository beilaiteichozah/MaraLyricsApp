package com.maralyrics.laitei.presentation.song_detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maralyrics.laitei.R
import com.maralyrics.laitei.domain.model.SongNeighbors

// Opaque, so the icons stay readable while the buttons float over the lyrics — a disabled button
// is only dimmed a little, not see-through, because lyrics showing through it just look like noise.
@Composable
private fun controlColors() = IconButtonDefaults.filledTonalIconButtonColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
)

/**
 * The reading controls that appear when the lyrics are tapped: text size on top; Previous / Next
 * on the sides with rotate and fullscreen between them below. Previous and Next dim at either end
 * of the list, and the size buttons at the smallest and largest size.
 */
@Composable
internal fun SongBottomControls(
    neighbors: SongNeighbors?,
    isFullscreen: Boolean,
    zoom: Float,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRotate: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ZoomControls(
            zoom = zoom,
            onZoomIn = onZoomIn,
            onZoomOut = onZoomOut,
            onZoomReset = onZoomReset
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            FilledTonalIconButton(
                onClick = onPrevious,
                enabled = neighbors?.previousId != null,
                colors = controlColors(),
                modifier = Modifier.size(52.dp).align(Alignment.CenterStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.song_previous))
            }

            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = onRotate,
                    colors = controlColors(),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.ScreenRotation, contentDescription = stringResource(R.string.song_rotate))
                }
                FilledTonalIconButton(
                    onClick = onToggleFullscreen,
                    colors = controlColors(),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = stringResource(
                            if (isFullscreen) R.string.song_exit_fullscreen else R.string.song_enter_fullscreen
                        )
                    )
                }
            }

            FilledTonalIconButton(
                onClick = onNext,
                enabled = neighbors?.nextId != null,
                colors = controlColors(),
                modifier = Modifier.size(52.dp).align(Alignment.CenterEnd)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.song_next))
            }
        }
    }
}

/** − [ 120% ] +  — the middle part shows the current size and puts it back to default when tapped. */
@Composable
private fun ZoomControls(
    zoom: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomReset: () -> Unit
) {
    val isDefault = LyricsZoom.isDefault(zoom)
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onZoomOut, enabled = LyricsZoom.canZoomOut(zoom)) {
                Icon(Icons.Default.ZoomOut, contentDescription = stringResource(R.string.song_zoom_out))
            }
            Text(
                text = stringResource(R.string.song_zoom_percent, LyricsZoom.percent(zoom)),
                modifier = Modifier
                    .widthIn(min = 64.dp)
                    // Tapping the percentage is "back to default"; there's nothing to reset at 100%.
                    .clickable(
                        enabled = !isDefault,
                        onClickLabel = stringResource(R.string.song_zoom_reset)
                    ) { onZoomReset() }
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isDefault) FontWeight.Normal else FontWeight.Bold,
                color = if (isDefault) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
                textAlign = TextAlign.Center
            )
            IconButton(onClick = onZoomIn, enabled = LyricsZoom.canZoomIn(zoom)) {
                Icon(Icons.Default.ZoomIn, contentDescription = stringResource(R.string.song_zoom_in))
            }
        }
    }
}

/** "12 / 229": where this song sits in the list Previous/Next are walking. */
@Composable
internal fun SongPositionPill(neighbors: SongNeighbors, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Text(
            text = stringResource(R.string.song_position_format, neighbors.position, neighbors.total),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/** Fullscreen hides the app bar, and its favorite star with it — so it floats here instead. */
@Composable
internal fun FloatingFavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalIconButton(
        onClick = onClick,
        colors = controlColors(),
        modifier = modifier.size(40.dp)
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = stringResource(R.string.favorites_label),
            tint = if (isFavorite) Color.Red else LocalContentColor.current
        )
    }
}
