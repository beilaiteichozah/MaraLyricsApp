package com.maralyrics.presentation.common.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.maralyrics.R
import com.maralyrics.domain.model.SocialLink

@Composable
fun DynamicSocialLinks(
    links: List<SocialLink>,
    modifier: Modifier = Modifier
) {
    if (links.isEmpty()) return

    val uriHandler = LocalUriHandler.current

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(links) { link ->
            SocialIconButton(
                link = link,
                onClick = { uriHandler.openUri(link.url) }
            )
        }
    }
}

@Composable
private fun SocialIconButton(
    link: SocialLink,
    onClick: () -> Unit
) {
    val platform = link.platform.lowercase()
    
    val icon: Any = when {
        platform.contains("facebook") -> painterResource(id = R.drawable.ic_facebook)
        platform.contains("youtube") -> painterResource(id = R.drawable.ic_youtube)
        platform.contains("instagram") -> painterResource(id = R.drawable.ic_instagram)
        platform.contains("tiktok") -> painterResource(id = R.drawable.ic_tiktok)
        platform.contains("spotify") -> painterResource(id = R.drawable.ic_spotify)
        platform.contains("soundcloud") -> painterResource(id = R.drawable.ic_soundcloud)
        platform.contains("telegram") -> painterResource(id = R.drawable.ic_telegram)
        platform.contains("whatsapp") -> painterResource(id = R.drawable.ic_whatsapp)
        platform.contains("twitter") || platform == "x" -> painterResource(id = R.drawable.ic_x)
        platform.contains("website") -> Icons.Default.Language
        else -> Icons.Default.Link
    }

    val contentDesc = when {
        platform.contains("facebook") -> stringResource(R.string.platform_facebook)
        platform.contains("youtube") -> stringResource(R.string.platform_youtube)
        platform.contains("instagram") -> stringResource(R.string.platform_instagram)
        platform.contains("tiktok") -> stringResource(R.string.platform_tiktok)
        platform.contains("spotify") -> stringResource(R.string.platform_spotify)
        platform.contains("soundcloud") -> stringResource(R.string.platform_soundcloud)
        platform.contains("telegram") -> stringResource(R.string.platform_telegram)
        platform.contains("whatsapp") -> stringResource(R.string.platform_whatsapp)
        platform.contains("twitter") || platform == "x" -> stringResource(R.string.platform_twitter)
        platform.contains("website") -> stringResource(R.string.platform_website)
        else -> stringResource(R.string.platform_link)
    }

    IconButton(onClick = onClick) {
        when (icon) {
            is Painter -> {
                Icon(
                    painter = icon,
                    contentDescription = contentDesc,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            is androidx.compose.ui.graphics.vector.ImageVector -> {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDesc,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
