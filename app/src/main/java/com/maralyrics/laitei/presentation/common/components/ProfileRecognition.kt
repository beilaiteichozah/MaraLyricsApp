package com.maralyrics.laitei.presentation.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maralyrics.laitei.R
import com.maralyrics.laitei.domain.model.BadgePeriod
import com.maralyrics.laitei.domain.model.ProfileBadge
import com.maralyrics.laitei.domain.model.ProfileType

// The Green mark is green on every colour palette, as on the website.
private val GreenMarkColor = Color(0xFF2E9E4F)

/** The green tick the website shows beside the name of a paid (Green mark) profile. */
@Composable
fun GreenMark(modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Icon(
        imageVector = Icons.Default.Verified,
        contentDescription = stringResource(R.string.green_mark),
        tint = GreenMarkColor,
        modifier = modifier.size(size)
    )
}

/** Recognition awards as small pills, e.g. "Artist of the Year · 2024". Draws nothing without badges. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BadgeChips(
    badges: List<ProfileBadge>,
    type: ProfileType,
    modifier: Modifier = Modifier
) {
    if (badges.isEmpty()) return
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        badges.forEach { badge ->
            val label = badgeLabel(badge, type)
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.semantics(mergeDescendants = true) {}
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(text = label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/** The award's own name if it has one, otherwise worded from its period; then " · 2024" if dated. */
@Composable
internal fun badgeLabel(badge: ProfileBadge, type: ProfileType): String {
    val isArtist = type == ProfileType.ARTIST
    val name = badge.title ?: when (badge.period) {
        BadgePeriod.MONTH -> stringResource(if (isArtist) R.string.badge_artist_month else R.string.badge_composer_month)
        BadgePeriod.YEAR -> stringResource(if (isArtist) R.string.badge_artist_year else R.string.badge_composer_year)
        BadgePeriod.LIFETIME -> stringResource(R.string.badge_lifetime)
        BadgePeriod.OTHER -> stringResource(R.string.badge_generic)
    }
    return if (badge.periodValue != null) {
        stringResource(R.string.badge_title_period, name, badge.periodValue)
    } else {
        name
    }
}
