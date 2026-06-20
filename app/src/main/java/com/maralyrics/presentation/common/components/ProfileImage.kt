package com.maralyrics.presentation.common.components

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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
        val base64Bitmap = remember(imageUrl) {
            if (imageUrl != null && imageUrl.startsWith("data:image")) {
                try {
                    val base64String = imageUrl.substringAfter(",")
                    val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }

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
                    DefaultAvatar(name = name)
                }
            )
        } else {
            DefaultAvatar(name = name)
        }
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
private fun DefaultAvatar(name: String) {
    val firstLetter = name.firstOrNull()?.uppercase() ?: "?"
    val colorPair = getGradientColors(firstLetter)
    
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
                fontSize = 48.sp,
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
