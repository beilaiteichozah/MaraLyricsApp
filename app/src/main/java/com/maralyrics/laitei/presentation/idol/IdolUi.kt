package com.maralyrics.laitei.presentation.idol

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.maralyrics.laitei.R
import com.maralyrics.laitei.domain.model.EventMoment
import com.maralyrics.laitei.domain.model.IdolResult
import com.maralyrics.laitei.domain.model.IdolSeason
import com.maralyrics.laitei.utils.formatMediumDate

/** "2024 · Siaha" — whichever of year and venue the server provided. */
internal fun IdolSeason.metaLine(): String =
    listOfNotNull(year?.toString(), venue).joinToString(" · ")

/** "Oct 23, 2024 – Dec 3, 2024", or a single date when the season has no distinct end. */
internal fun IdolSeason.dateRange(): String? {
    val start = EventMoment.parse(startDate)
    val end = EventMoment.parse(endDate)
    return when {
        start != null && end != null && end > start -> "${formatMediumDate(start)} – ${formatMediumDate(end)}"
        start != null -> formatMediumDate(start)
        else -> null
    }
}

/** Null for a plain contestant — only placements get a badge. */
@Composable
internal fun IdolResult.label(): String? = when (this) {
    IdolResult.WINNER -> stringResource(R.string.idol_result_winner)
    IdolResult.RUNNER_UP -> stringResource(R.string.idol_result_runner_up)
    IdolResult.SECOND_RUNNER_UP -> stringResource(R.string.idol_result_second_runner_up)
    IdolResult.FINALIST -> stringResource(R.string.idol_result_finalist)
    IdolResult.SEMI_FINALIST -> stringResource(R.string.idol_result_semi_finalist)
    IdolResult.CONTESTANT -> null
}

@Composable
internal fun ResultBadge(result: IdolResult, modifier: Modifier = Modifier) {
    val label = result.label() ?: return
    val isWinner = result == IdolResult.WINNER
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = if (isWinner) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (isWinner) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isWinner) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(14.dp))
                Text(text = label, modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelMedium)
            } else {
                Text(text = label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
