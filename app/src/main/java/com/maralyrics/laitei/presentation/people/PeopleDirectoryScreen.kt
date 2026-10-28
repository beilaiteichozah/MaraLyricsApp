package com.maralyrics.laitei.presentation.people

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.maralyrics.laitei.R
import com.maralyrics.laitei.domain.model.DirectoryPerson
import com.maralyrics.laitei.domain.model.PeopleRole
import com.maralyrics.laitei.domain.model.PeopleSort
import com.maralyrics.laitei.domain.model.ProfileType
import com.maralyrics.laitei.presentation.common.components.EmptyState
import com.maralyrics.laitei.presentation.common.components.GreenMark
import com.maralyrics.laitei.presentation.common.components.ListSkeleton
import com.maralyrics.laitei.presentation.common.components.ProfileImage
import com.maralyrics.laitei.presentation.common.components.badgeLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleDirectoryScreen(
    onBackClick: () -> Unit,
    onPersonClick: (slug: String, type: ProfileType) -> Unit,
    viewModel: PeopleDirectoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.people_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text(stringResource(R.string.people_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.btn_clear))
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            RoleAndSortRow(
                role = state.role,
                sort = state.sort,
                hasBadges = state.hasBadges,
                count = state.people.size.takeIf { !state.isLoading && it > 0 },
                onRoleChange = viewModel::onRoleChange,
                onSortChange = viewModel::onSortChange
            )

            when {
                state.isLoading -> ListSkeleton()
                state.people.isEmpty() -> EmptyState(
                    icon = Icons.Default.Groups,
                    message = stringResource(R.string.people_empty_title),
                    description = stringResource(R.string.people_empty_desc),
                    actionText = stringResource(R.string.people_reset),
                    onActionClick = viewModel::resetFilters
                )
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.people, key = { "${it.type.name}:${it.slug}" }) { person ->
                            PersonRow(person = person, onClick = { onPersonClick(person.slug, person.type) })
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleAndSortRow(
    role: PeopleRole,
    sort: PeopleSort,
    hasBadges: Boolean,
    count: Int?,
    onRoleChange: (PeopleRole) -> Unit,
    onSortChange: (PeopleSort) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoleChip(PeopleRole.ALL, role, R.string.people_role_all, onRoleChange)
            RoleChip(PeopleRole.ARTISTS, role, R.string.people_role_artists, onRoleChange)
            RoleChip(PeopleRole.COMPOSERS, role, R.string.people_role_composers, onRoleChange)
        }

        // The count and the sort control share a row, so the sort label always has room.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (count != null) stringResource(R.string.people_count_format, count) else "",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SortMenu(sort = sort, hasBadges = hasBadges, onSortChange = onSortChange)
        }
    }
}

@Composable
private fun SortMenu(sort: PeopleSort, hasBadges: Boolean, onSortChange: (PeopleSort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(contentAlignment = Alignment.CenterEnd) {
        AssistChip(
            onClick = { open = true },
            label = { Text(stringResource(sort.labelRes()), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = {
                Icon(
                    Icons.AutoMirrored.Filled.Sort,
                    contentDescription = stringResource(R.string.people_sort_label),
                    modifier = Modifier.size(AssistChipDefaults.IconSize)
                )
            }
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            PeopleSort.entries
                .filter { it != PeopleSort.BADGES_DESC || hasBadges }
                .forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(option.labelRes())) },
                        onClick = {
                            open = false
                            onSortChange(option)
                        }
                    )
                }
        }
    }
}

@Composable
private fun RoleChip(value: PeopleRole, selected: PeopleRole, labelRes: Int, onSelect: (PeopleRole) -> Unit) {
    FilterChip(
        selected = value == selected,
        onClick = { onSelect(value) },
        label = { Text(stringResource(labelRes), maxLines = 1) }
    )
}

@Composable
private fun PersonRow(person: DirectoryPerson, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ProfileImage(imageUrl = person.imageUrl, name = person.name, size = 48.dp)

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = person.name,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (person.isGreen) GreenMark(size = 18.dp)
            }
            val role = stringResource(if (person.type == ProfileType.ARTIST) R.string.artist else R.string.composer)
            Text(
                text = "$role · ${pluralStringResource(R.plurals.people_songs_format, person.songCount, person.songCount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            person.badges.firstOrNull()?.let { first ->
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    val more = person.badges.size - 1
                    val label = badgeLabel(first, person.type)
                    Text(
                        text = if (more > 0) stringResource(R.string.badge_more_format, label, more) else label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun PeopleSort.labelRes(): Int = when (this) {
    PeopleSort.NAME_ASC -> R.string.people_sort_name_asc
    PeopleSort.NAME_DESC -> R.string.people_sort_name_desc
    PeopleSort.SONGS_DESC -> R.string.people_sort_songs_desc
    PeopleSort.SONGS_ASC -> R.string.people_sort_songs_asc
    PeopleSort.BADGES_DESC -> R.string.people_sort_badges_desc
}
