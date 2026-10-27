package com.maralyrics.laitei.presentation.common.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A rectangular image from either a normal URL or a base64 `data:` URI. The Mara Idol API sends
 * season covers inline as data URIs (~75 KB of base64 each), so those are decoded off the main
 * thread and downsampled to roughly screen width instead of being inflated at full size.
 */
@Composable
fun BannerImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            url.isNullOrBlank() -> Unit
            url.startsWith("data:") -> {
                val bitmap by produceState<ImageBitmap?>(initialValue = null, url) {
                    value = withContext(Dispatchers.Default) { decodeDataUri(url, TARGET_WIDTH_PX) }
                }
                bitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = contentDescription,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = contentScale
                    )
                }
            }
            else -> AsyncImage(
                model = url,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        }
    }
}

private const val TARGET_WIDTH_PX = 1080

private fun decodeDataUri(uri: String, targetWidthPx: Int): ImageBitmap? = try {
    val bytes = Base64.decode(uri.substringAfter(','), Base64.DEFAULT)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

    var sampleSize = 1
    while (bounds.outWidth / (sampleSize * 2) >= targetWidthPx) sampleSize *= 2

    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
} catch (e: IllegalArgumentException) {
    null // not valid base64
}
