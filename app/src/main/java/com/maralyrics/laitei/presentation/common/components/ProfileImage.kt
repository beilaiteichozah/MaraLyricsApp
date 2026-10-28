package com.maralyrics.laitei.presentation.common.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import androidx.compose.foundation.Image

@Composable
fun ProfileImage(
    imageUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val targetPx = with(LocalDensity.current) { size.roundToPx() }
        val base64Bitmap = remember(imageUrl, targetPx) { decodeDataUri(imageUrl, targetPx) }

        if (base64Bitmap != null) {
            Image(
                bitmap = base64Bitmap,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (!imageUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = {
                    ShimmerLoading()
                },
                error = {
                    DefaultAvatar(name = name, size = size)
                }
            )
        } else {
            DefaultAvatar(name = name, size = size)
        }
    }
}

/**
 * Decodes an inline `data:image/...;base64,` photo, shrunk to about [targetPx]. Profile photos
 * arrive as full-size base64, and a long list of small avatars would otherwise hold every one of
 * them in memory at full resolution.
 */
private fun decodeDataUri(imageUrl: String?, targetPx: Int): ImageBitmap? {
    if (imageUrl == null || !imageUrl.startsWith("data:image")) return null
    return try {
        val bytes = Base64.decode(imageUrl.substringAfter(","), Base64.DEFAULT)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        var sample = 1
        while (targetPx > 0 && bounds.outWidth / (sample * 2) >= targetPx && bounds.outHeight / (sample * 2) >= targetPx) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun ShimmerLoading() {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.surfaceVariant,
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "translate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    Box(modifier = Modifier.fillMaxSize().background(brush))
}

@Composable
private fun DefaultAvatar(name: String, size: Dp) {
    val firstLetter = name.firstOrNull()?.uppercase() ?: "?"
    val colorPair = getGradientColors(firstLetter)
    // 48sp on the big profile photo, scaled down to fit a small list avatar.
    val letterSize = minOf(48f, size.value * 0.4f).sp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(colorPair.first, colorPair.second)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = firstLetter,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = letterSize,
                color = Color.White
            )
        )
    }
}

private fun getGradientColors(letter: String): Pair<Color, Color> {
    return when (letter.uppercase()) {
        "A", "B", "C", "Â" -> Color(0xFF6200EE) to Color(0xFF3700B3)
        "D", "E", "F" -> Color(0xFF03DAC6) to Color(0xFF018786)
        "G", "H", "I" -> Color(0xFFFF0266) to Color(0xFFC51162)
        "J", "K", "L" -> Color(0xFFFFDE03) to Color(0xFFFBC02D)
        "M", "N", "O", "Ô" -> Color(0xFF00E5FF) to Color(0xFF00B8D4)
        "P", "Q", "R" -> Color(0xFF76FF03) to Color(0xFF64DD17)
        "S", "T", "U" -> Color(0xFFFF3D00) to Color(0xFFDD2C00)
        "V", "W", "X", "AW" -> Color(0xFF651FFF) to Color(0xFF6200EA)
        "Y", "Z" -> Color(0xFF2979FF) to Color(0xFF2962FF)
        else -> Color(0xFF9E9E9E) to Color(0xFF616161)
    }
}
