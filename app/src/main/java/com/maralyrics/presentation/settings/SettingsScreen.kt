package com.maralyrics.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.maralyrics.R
import com.maralyrics.domain.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val songCount by viewModel.songCount.collectAsState()
    val dbSize by viewModel.dbSize.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            settings?.let { currentSettings ->
                SettingsSection(title = stringResource(R.string.set_language)) {
                    LanguageSelector(
                        selected = currentSettings.language,
                        onSelected = viewModel::updateLanguage
                    )
                }

                SettingsSection(title = stringResource(R.string.set_theme)) {
                    ThemeSelector(
                        selected = currentSettings.theme,
                        onSelected = viewModel::updateTheme
                    )
                }

                SettingsSection(title = stringResource(R.string.set_default_cat)) {
                    CategorySelector(
                        selected = currentSettings.defaultCategory,
                        onSelected = viewModel::updateCategory
                    )
                }

                SettingsSection(title = stringResource(R.string.set_offline_data)) {
                    OfflineDataInfo(
                        lastSync = currentSettings.lastSyncTimestamp,
                        dbVersion = currentSettings.databaseVersion,
                        songCount = songCount,
                        dbSize = dbSize,
                        onSyncClick = viewModel::syncNow,
                        onRedownloadClick = viewModel::redownloadDatabase
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
        HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
fun LanguageSelector(selected: AppLanguage, onSelected: (AppLanguage) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppLanguage.entries.forEach { language ->
            FilterChip(
                selected = language == selected,
                onClick = { onSelected(language) },
                label = { Text(if (language == AppLanguage.MARA) "Mara" else "English") }
            )
        }
    }
}

@Composable
fun ThemeSelector(selected: AppTheme, onSelected: (AppTheme) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppTheme.entries.forEach { theme ->
            FilterChip(
                selected = theme == selected,
                onClick = { onSelected(theme) },
                label = { 
                    Text(when(theme) {
                        AppTheme.LIGHT -> stringResource(R.string.set_theme_light)
                        AppTheme.DARK -> stringResource(R.string.set_theme_dark)
                        AppTheme.SYSTEM -> stringResource(R.string.set_theme_system)
                    })
                }
            )
        }
    }
}

@Composable
fun CategorySelector(selected: SongCategory, onSelected: (SongCategory) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SongCategory.entries.forEach { category ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelected(category) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(category.name)
                RadioButton(selected = category == selected, onClick = null)
            }
        }
    }
}

@Composable
fun OfflineDataInfo(
    lastSync: Long,
    dbVersion: Int,
    songCount: Int,
    dbSize: Long,
    onSyncClick: () -> Unit,
    onRedownloadClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.last_sync) + ": " + if (lastSync > 0) java.util.Date(lastSync).toString() else "Never")
        Text("Database Version: $dbVersion")
        Text(stringResource(R.string.total_songs) + ": $songCount")
        Text(stringResource(R.string.db_size) + ": ${dbSize / 1024} KB")
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSyncClick) {
                Text(stringResource(R.string.sync_now))
            }
            OutlinedButton(onClick = onRedownloadClick) {
                Text(stringResource(R.string.redownload_db))
            }
        }
    }
}
