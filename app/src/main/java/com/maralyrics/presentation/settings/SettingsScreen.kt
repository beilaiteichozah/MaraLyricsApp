package com.maralyrics.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.tooling.preview.Preview
import com.maralyrics.BuildConfig
import com.maralyrics.R
import com.maralyrics.domain.model.*
import com.maralyrics.presentation.theme.MaraColorSchemes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onCreditsClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val availableCategories by viewModel.availableCategories.collectAsState()
    val songCount by viewModel.songCount.collectAsState()
    val dbSize by viewModel.dbSize.collectAsState()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    var showAboutDialog by remember { mutableStateOf(false) }
    var showCreditsDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
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
                // 1. App Theme (Mode & Color)
                SettingsSection(title = stringResource(R.string.set_theme)) {
                    ThemeSelector(
                        selected = currentSettings.theme,
                        onSelected = viewModel::updateTheme
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = stringResource(R.string.color_palette),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    ColorThemeSelector(
                        selected = currentSettings.colorTheme,
                        onSelected = viewModel::updateColorTheme
                    )
                }

                // 2. App Language
                SettingsSection(title = stringResource(R.string.set_language)) {
                    LanguageSelector(
                        selected = currentSettings.language,
                        onSelected = viewModel::updateLanguage
                    )
                }

                // 3. Default Category
                SettingsSection(title = stringResource(R.string.set_default_cat)) {
                    CategorySelector(
                        selectedCategories = currentSettings.defaultCategories,
                        availableCategories = availableCategories,
                        onSelected = viewModel::updateCategory
                    )
                }

                // 4. Accessibility & Display
                SettingsSection(title = stringResource(R.string.accessibility_display)) {
                    Text(
                        text = stringResource(R.string.default_font_size) + ": ${currentSettings.defaultFontSize}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = currentSettings.defaultFontSize.toFloat(),
                        onValueChange = { viewModel.updateDefaultFontSize(it.toInt()) },
                        valueRange = 12f..32f,
                        steps = 10
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = stringResource(R.string.line_spacing) + ": ${String.format(java.util.Locale.US, "%.1f", currentSettings.lineSpacing)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = currentSettings.lineSpacing,
                        onValueChange = viewModel::updateLineSpacing,
                        valueRange = 1.0f..2.5f,
                        steps = 15
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Preview Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.preview_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.preview_text),
                                fontSize = currentSettings.defaultFontSize.sp,
                                lineHeight = (currentSettings.defaultFontSize * currentSettings.lineSpacing).sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // 5. Data & Sync
                SettingsSection(title = stringResource(R.string.data_sync)) {
                    SwitchItem(
                        title = stringResource(R.string.auto_sync),
                        description = stringResource(R.string.auto_sync_desc),
                        checked = currentSettings.autoSyncEnabled,
                        onCheckedChange = viewModel::updateAutoSync
                    )
                    SwitchItem(
                        title = stringResource(R.string.wifi_only),
                        description = stringResource(R.string.wifi_only_desc),
                        checked = currentSettings.wifiOnlySync,
                        onCheckedChange = viewModel::updateWifiOnly
                    )
                    SwitchItem(
                        title = stringResource(R.string.resume_session),
                        description = stringResource(R.string.resume_session_desc),
                        checked = currentSettings.resumeSessionEnabled,
                        onCheckedChange = viewModel::updateResumeSession
                    )
                    SettingsItem(
                        title = stringResource(R.string.backup_favorites),
                        icon = Icons.Default.Backup,
                        onClick = viewModel::backupFavorites
                    )
                    SettingsItem(
                        title = stringResource(R.string.restore_favorites),
                        icon = Icons.Default.Restore,
                        onClick = viewModel::restoreFavorites
                    )
                    SettingsItem(
                        title = stringResource(R.string.clear_cache),
                        description = stringResource(R.string.clear_cache_desc),
                        icon = Icons.Default.DeleteSweep,
                        onClick = viewModel::clearCache
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OfflineDataInfo(
                        lastSync = currentSettings.lastSyncTimestamp,
                        dbVersion = currentSettings.databaseVersion,
                        songCount = songCount,
                        dbSize = dbSize,
                        onSyncClick = viewModel::syncNow,
                        onRedownloadClick = viewModel::redownloadDatabase
                    )
                }

                // 6. Legal
                SettingsSection(title = stringResource(R.string.legal)) {
                    SettingsItem(
                        title = stringResource(R.string.privacy_policy),
                        icon = Icons.Default.PrivacyTip,
                        onClick = { uriHandler.openUri("https://maralyrics.com/privacy") }
                    )
                    SettingsItem(
                        title = stringResource(R.string.terms_of_use),
                        icon = Icons.Default.Gavel,
                        onClick = { uriHandler.openUri("https://maralyrics.com/terms") }
                    )
                }

                // 7. About App
                SettingsSection(title = stringResource(R.string.about_app)) {
                    SettingsItem(
                        title = stringResource(R.string.about_app),
                        icon = Icons.Default.Info,
                        onClick = { showAboutDialog = true }
                    )
                    SettingsItem(
                        title = stringResource(R.string.view_tutorial_again),
                        icon = Icons.Default.PlayCircle,
                        onClick = {
                            viewModel.resetOnboarding()
                            onBackClick() // Go back to Home which will trigger NavHost to show Onboarding
                        }
                    )
                    SettingsItem(
                        title = stringResource(R.string.credits),
                        icon = Icons.Default.People,
                        onClick = onCreditsClick
                    )
                    SettingsItem(
                        title = stringResource(R.string.licenses),
                        icon = Icons.Default.Description,
                        onClick = { showLicensesDialog = true }
                    )
                }

                // 8. Support & Feedback
                SettingsSection(title = stringResource(R.string.support_feedback)) {
                    SettingsItem(
                        title = stringResource(R.string.help_faq),
                        icon = Icons.Default.Help,
                        onClick = { uriHandler.openUri("https://maralyrics.com/faq") }
                    )
                    SettingsItem(
                        title = stringResource(R.string.contact_us),
                        icon = Icons.Default.Email,
                        onClick = { uriHandler.openUri("https://maralyrics.com/contact") }
                    )
                    SettingsItem(
                        title = stringResource(R.string.send_feedback),
                        icon = Icons.Default.Feedback,
                        onClick = { uriHandler.openUri("https://maralyrics.com/report") }
                    )
                    SettingsItem(
                        title = stringResource(R.string.rate_app),
                        icon = Icons.Default.Star,
                        onClick = { 
                            uriHandler.openUri("https://play.google.com/store/apps/details?id=${context.packageName}")
                        }
                    )
                    SettingsItem(
                        title = stringResource(R.string.share_app),
                        icon = Icons.Default.Share,
                        onClick = {
                            val intent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, context.getString(R.string.share_app_text) + " https://play.google.com/store/apps/details?id=${context.packageName}")
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, null))
                        }
                    )
                }
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(stringResource(R.string.about_app)) },
            text = {
                Column {
                    Text(stringResource(R.string.about_app_desc))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.app_version) + ": ${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.build_date) + ": ${BuildConfig.BUILD_DATE}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.btn_ok))
                }
            }
        )
    }

    if (showCreditsDialog) {
        AlertDialog(
            onDismissRequest = { showCreditsDialog = false },
            title = { Text(stringResource(R.string.credits)) },
            text = { Text(stringResource(R.string.credits_desc)) },
            confirmButton = {
                TextButton(onClick = { showCreditsDialog = false }) {
                    Text(stringResource(R.string.btn_ok))
                }
            }
        )
    }

    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            title = { Text(stringResource(R.string.licenses)) },
            text = { 
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = stringResource(R.string.mit_license_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.mit_license_text),
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    
                    Text(
                        text = stringResource(R.string.open_source_libs),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LicenseLinkItem(
                        name = "Jetpack Compose",
                        url = "https://developer.android.com/jetpack/compose",
                        uriHandler = uriHandler
                    )
                    LicenseLinkItem(
                        name = "Kotlin Coroutines",
                        url = "https://github.com/Kotlin/kotlinx.coroutines",
                        uriHandler = uriHandler
                    )
                    LicenseLinkItem(
                        name = "Retrofit & OkHttp",
                        url = "https://square.github.io/retrofit/",
                        uriHandler = uriHandler
                    )
                    LicenseLinkItem(
                        name = "Room Database",
                        url = "https://developer.android.com/training/data-storage/room",
                        uriHandler = uriHandler
                    )
                    LicenseLinkItem(
                        name = "Hilt DI",
                        url = "https://developer.android.com/training/dependency-injection/hilt-android",
                        uriHandler = uriHandler
                    )
                    LicenseLinkItem(
                        name = "Coil Image Loading",
                        url = "https://coil-kt.github.io/coil/",
                        uriHandler = uriHandler
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicensesDialog = false }) {
                    Text(stringResource(R.string.btn_ok))
                }
            }
        )
    }
}

@Composable
fun ColorThemeSelector(
    selected: AppColorTheme,
    onSelected: (AppColorTheme) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(AppColorTheme.entries) { theme ->
            val colorScheme = MaraColorSchemes.getColorScheme(theme, false)
            val isSelected = theme == selected
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelected(theme) }
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colorScheme.primary)
                        .then(
                            if (isSelected) Modifier.border(4.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            else Modifier
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(when(theme) {
                        AppColorTheme.MARA -> R.string.color_mara
                        AppColorTheme.OCEAN -> R.string.color_ocean
                        AppColorTheme.EMERALD -> R.string.color_emerald
                        AppColorTheme.SUNSET -> R.string.color_sunset
                        AppColorTheme.PURPLE -> R.string.color_purple
                        AppColorTheme.ROSE -> R.string.color_rose
                        AppColorTheme.SLATE -> R.string.color_slate
                    }),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LicenseLinkItem(name: String, url: String, uriHandler: androidx.compose.ui.platform.UriHandler) {
    Text(
        text = name,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(url) }
            .padding(vertical = 4.dp),
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
fun SettingsItem(
    title: String,
    description: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SwitchItem(
    title: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        content()
        HorizontalDivider(modifier = Modifier.padding(top = 16.dp), thickness = 0.5.dp)
    }
}

@Composable
fun LanguageSelector(selected: AppLanguage, onSelected: (AppLanguage) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppLanguage.entries.forEach { language ->
            FilterChip(
                selected = language == selected,
                onClick = { onSelected(language) },
                label = { 
                    Text(
                        when(language) {
                            AppLanguage.MARA -> stringResource(R.string.lang_mara)
                            AppLanguage.BURMESE -> stringResource(R.string.lang_burmese)
                            else -> stringResource(R.string.lang_english)
                        }
                    ) 
                },
                shape = RoundedCornerShape(12.dp)
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
                        AppTheme.LIGHT -> stringResource(R.string.theme_light)
                        AppTheme.DARK -> stringResource(R.string.theme_dark)
                        AppTheme.SYSTEM -> stringResource(R.string.theme_system)
                    })
                },
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun CategorySelector(
    selectedCategories: List<String>,
    availableCategories: List<SongCategory>,
    onSelected: (String) -> Unit
) {
    val allOptions = listOf(SongCategory("All", stringResource(R.string.cat_all))) + availableCategories
    
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        allOptions.forEach { category ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelected(category.key) }
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayName = when (category.key) {
                    "All" -> stringResource(R.string.cat_all)
                    "Gospel" -> stringResource(R.string.cat_gospel)
                    "Love" -> stringResource(R.string.cat_love)
                    "Patriotic" -> stringResource(R.string.cat_patriotic)
                    "Traditional" -> stringResource(R.string.cat_traditional)
                    "Uncategorized" -> stringResource(R.string.cat_uncategorized)
                    else -> category.displayName
                }
                Text(displayName, style = MaterialTheme.typography.bodyLarge)
                Checkbox(
                    checked = selectedCategories.contains(category.key),
                    onCheckedChange = null
                )
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.last_sync), style = MaterialTheme.typography.bodySmall)
                Text(if (lastSync > 0) java.text.DateFormat.getDateTimeInstance().format(java.util.Date(lastSync)) else stringResource(R.string.never), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.db_version), style = MaterialTheme.typography.bodySmall)
                Text(dbVersion.toString(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.total_songs), style = MaterialTheme.typography.bodySmall)
                Text(songCount.toString(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.db_size), style = MaterialTheme.typography.bodySmall)
                Text("${dbSize / 1024} ${stringResource(R.string.kb)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSyncClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.sync_now))
                }
                OutlinedButton(
                    onClick = onRedownloadClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.redownload_db))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsSectionPreview() {
    com.maralyrics.presentation.theme.MaraLyricsTheme {
        Column {
            SettingsSection(title = stringResource(R.string.about_app)) {
                SettingsItem(
                    title = stringResource(R.string.about_app),
                    icon = Icons.Default.Info,
                    onClick = {}
                )
                SwitchItem(
                    title = stringResource(R.string.auto_sync),
                    description = stringResource(R.string.auto_sync_desc),
                    checked = true,
                    onCheckedChange = {}
                )
            }
        }
    }
}
