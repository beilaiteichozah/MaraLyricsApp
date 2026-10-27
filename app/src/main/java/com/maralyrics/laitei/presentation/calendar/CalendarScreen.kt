package com.maralyrics.laitei.presentation.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.maralyrics.laitei.R
import com.maralyrics.laitei.domain.model.CommunityCalendar
import com.maralyrics.laitei.domain.model.CommunityEvent
import com.maralyrics.laitei.domain.model.EventMoment
import com.maralyrics.laitei.presentation.common.components.EmptyState
import com.maralyrics.laitei.presentation.common.components.ListSkeleton
import com.maralyrics.laitei.presentation.common.components.LoadErrorState
import com.maralyrics.laitei.presentation.common.components.OfflineNotice
import com.maralyrics.laitei.utils.formatMediumDate
import com.maralyrics.laitei.utils.formatMonthShort
import com.maralyrics.laitei.utils.formatMonthYear
import com.maralyrics.laitei.utils.formatTimeOfDay

/** Flattened rows of the events list: a month heading, then that month's events. */
private sealed interface CalendarRow {
    val key: String

    data class Month(val year: Int, val month: Int) : CalendarRow {
        override val key = "month_${year}_$month"
    }

    data class Event(val event: CommunityEvent) : CalendarRow {
        override val key = "event_${event.id}"
    }
}

private fun List<CommunityEvent>.toRows(): List<CalendarRow> = buildList {
    var current: Pair<Int, Int>? = null
    for (event in this@toRows) {
        val month = event.start.year to event.start.month
        if (month != current) {
            current = month
            add(CalendarRow.Month(month.first, month.second))
        }
        add(CalendarRow.Event(event))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBackClick: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calendar_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.selected == null) {
                // No calendar to work with yet: still loading the list, failed, or none exist.
                when {
                    state.isLoading -> ListSkeleton()
                    state.loadFailed -> LoadErrorState(onRetry = viewModel::retry, icon = Icons.Default.CalendarMonth)
                    else -> EmptyState(
                        icon = Icons.Default.CalendarMonth,
                        message = stringResource(R.string.calendar_no_calendars)
                    )
                }
                return@Column
            }

            if (state.calendars.size > 1) {
                CalendarChips(state = state, onSelect = viewModel::selectCalendar)
            }
            YearSelector(year = state.year, onChange = viewModel::changeYear)
            if (state.isFromCache && !state.isLoading) OfflineNotice()

            when {
                state.isLoading -> ListSkeleton()
                state.loadFailed -> LoadErrorState(onRetry = viewModel::retry, icon = Icons.Default.CalendarMonth)
                state.events.isEmpty() -> EmptyState(
                    icon = Icons.Default.CalendarMonth,
                    message = stringResource(R.string.calendar_empty_for, state.selected?.name.orEmpty(), state.year)
                )
                else -> EventsList(events = state.events, year = state.year)
            }
        }
    }
}

@Composable
private fun CalendarChips(state: CalendarUiState, onSelect: (CommunityCalendar) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        state.calendars.forEach { calendar ->
            FilterChip(
                selected = calendar.slug == state.selected?.slug,
                onClick = { onSelect(calendar) },
                label = { Text(calendar.name, maxLines = 1) },
                shape = MaterialTheme.shapes.medium
            )
        }
    }
}

@Composable
private fun YearSelector(year: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = { onChange(-1) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.calendar_prev_year))
        }
        Text(
            text = year.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = { onChange(1) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.calendar_next_year))
        }
    }
}

@Composable
private fun EventsList(events: List<CommunityEvent>, year: Int) {
    val today = remember { EventMoment.today() }
    val rows = remember(events) { events.toRows() }
    val listState = rememberLazyListState()

    // Open on what's coming up rather than on January — only meaningful for the current year,
    // where earlier months are already behind us.
    LaunchedEffect(rows) {
        if (year != today.year) return@LaunchedEffect
        val firstUpcoming = rows.indexOfFirst { it is CalendarRow.Event && !it.event.isOver(today) }
        if (firstUpcoming > 0) {
            // Land on the month heading above it so the event keeps its context.
            val heading = rows.subList(0, firstUpcoming).indexOfLast { it is CalendarRow.Month }
            listState.scrollToItem(if (heading >= 0) heading else firstUpcoming)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(rows, key = { it.key }) { row ->
            when (row) {
                is CalendarRow.Month -> Text(
                    text = formatMonthYear(row.year, row.month),
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                is CalendarRow.Event -> EventRow(event = row.event, today = today)
            }
        }
    }
}

@Composable
private fun EventRow(event: CommunityEvent, today: EventMoment) {
    var expanded by remember { mutableStateOf(false) }
    val isOver = event.isOver(today)
    val statusText = eventStatus(event, today)
    val whenText = eventWhen(event)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isOver) 0.6f else 1f)
            .clickable(enabled = event.description != null) { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        DateBadge(event = event, highlighted = event.isOn(today))
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (statusText != null) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (whenText != null) {
                Text(
                    text = whenText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            event.description?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            event.location?.let {
                Row(modifier = Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = it,
                        modifier = Modifier.padding(start = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DateBadge(event: CommunityEvent, highlighted: Boolean) {
    val start = event.start
    // "1–3" when a multi-day event stays within one month; otherwise just the first day.
    val dayText = if (event.isMultiDay && event.end.year == start.year && event.end.month == start.month) {
        "${start.day}–${event.end.day}"
    } else {
        start.day.toString()
    }
    Surface(
        modifier = Modifier.width(56.dp),
        shape = MaterialTheme.shapes.medium,
        color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (highlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dayText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Text(
                text = formatMonthShort(start.year, start.month).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

/** "Happening today", "Tomorrow", "In 5 days" — nothing for past events or ones far off. */
@Composable
private fun eventStatus(event: CommunityEvent, today: EventMoment): String? {
    if (event.isOn(today)) return stringResource(R.string.event_status_today)
    val daysAway = today.daysUntil(event.start)
    return when {
        daysAway == 1 -> stringResource(R.string.event_status_tomorrow)
        daysAway in 2..SOON_WINDOW_DAYS -> pluralStringResource(R.plurals.event_status_soon, daysAway, daysAway)
        else -> null
    }
}

/** Date range for multi-day events, clock times for timed ones, null for a plain single day. */
@Composable
private fun eventWhen(event: CommunityEvent): String? {
    val start = event.start
    val end = event.end
    return when {
        event.isMultiDay && start.isAllDay -> {
            val days = start.daysUntil(end) + 1
            "${formatMediumDate(start)} – ${formatMediumDate(end)} · ${pluralStringResource(R.plurals.event_day_count, days, days)}"
        }
        event.isMultiDay -> "${formatMediumDate(start)} ${formatTimeOfDay(start)} – ${formatMediumDate(end)} ${formatTimeOfDay(end)}"
        !start.isAllDay -> {
            val from = formatTimeOfDay(start)
            val to = formatTimeOfDay(end).takeIf { !end.isAllDay && it != from }
            if (to != null) "$from – $to" else from
        }
        else -> null
    }
}

private const val SOON_WINDOW_DAYS = 30
