package com.maralyrics.laitei

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.maralyrics.laitei.R
import com.maralyrics.laitei.domain.model.AppLanguage
import com.maralyrics.laitei.domain.model.SyncStatus
import com.maralyrics.laitei.domain.model.DownloadProgress
import com.maralyrics.laitei.presentation.MainViewModel
import com.maralyrics.laitei.presentation.common.notification.NotificationHost
import com.maralyrics.laitei.presentation.common.notification.NotificationManager
import com.maralyrics.laitei.utils.LocalizedContextWrapper
import com.maralyrics.laitei.utils.StorageUtils
import com.maralyrics.laitei.utils.findActivity
import com.maralyrics.laitei.presentation.navigation.MaraLyricsNavHost
import com.maralyrics.laitei.presentation.theme.MaraLyricsTheme
import com.maralyrics.laitei.utils.ConnectivityObserver
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var connectivityObserver: ConnectivityObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val isReady by viewModel.isReady.collectAsState()
            val theme by viewModel.theme.collectAsState()
            val language by viewModel.language.collectAsState()
            val colorTheme by viewModel.colorTheme.collectAsState()
            val isSetupComplete by viewModel.isSetupComplete.collectAsState()
            val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsState()
            val privacyAccepted by viewModel.privacyAccepted.collectAsState()
            val lastRoute by viewModel.lastRoute.collectAsState()
            val syncAvailable by viewModel.syncAvailable.collectAsState()
            val updateStatus by viewModel.updateStatus.collectAsState()
            val isDownloading by viewModel.isDownloading.collectAsState()
            val downloadProgress by viewModel.downloadProgress.collectAsState()
            
            val networkStatus by connectivityObserver.observe().collectAsState(initial = ConnectivityObserver.Status.Available)
            
            var showSyncDialog by remember { mutableStateOf(false) }
            
            LaunchedEffect(syncAvailable) {
                if (syncAvailable) showSyncDialog = true
            }

            LaunchedEffect(networkStatus) {
                when (networkStatus) {
                    ConnectivityObserver.Status.Available -> notificationManager.showOnline()
                    ConnectivityObserver.Status.Lost, ConnectivityObserver.Status.Unavailable -> {
                        if (isSetupComplete) {
                            notificationManager.showOffline()
                        }
                    }
                    else -> {}
                }
            }

            splashScreen.setKeepOnScreenCondition { !isReady }

            val context = LocalContext.current
            val locale = remember(language) {
                java.util.Locale.forLanguageTag(language.code)
            }
            val configuration = remember(locale) {
                android.content.res.Configuration(context.resources.configuration).apply {
                    setLocale(locale)
                    setLayoutDirection(locale)
                }
            }
            val localizedContext = remember(configuration) {
                context.createConfigurationContext(configuration)
            }
            val wrappedContext = remember(localizedContext) {
                LocalizedContextWrapper(context, localizedContext)
            }

            CompositionLocalProvider(
                LocalContext provides wrappedContext,
                LocalConfiguration provides configuration
            ) {
                MaraLyricsTheme(theme = theme, colorTheme = colorTheme) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        if (isReady) {
                            MaraLyricsNavHost(
                                isSetupComplete = isSetupComplete,
                                hasCompletedOnboarding = hasCompletedOnboarding,
                                privacyAccepted = privacyAccepted,
                                initialRoute = lastRoute,
                                onRouteChanged = viewModel::onRouteChanged
                            )
                            NotificationHost(manager = notificationManager)

                            val showStorageWarning by viewModel.showStorageWarning.collectAsState()
                            val insufficientStorage by viewModel.insufficientStorage.collectAsState()

                            if (updateStatus != null || isDownloading) {
                                UpdateNotificationDialog(
                                    status = updateStatus,
                                    isDownloading = isDownloading,
                                    progress = downloadProgress,
                                    onDownload = viewModel::startUpdateDownload,
                                    onDismiss = viewModel::dismissUpdate
                                )
                            }

                            if (showStorageWarning != null) {
                                StorageWarningDialog(
                                    info = showStorageWarning!!,
                                    onConfirm = viewModel::confirmDownloadWithStorageWarning,
                                    onCancel = viewModel::cancelDownloadWithStorageWarning,
                                    onManageStorage = viewModel::openStorageSettings
                                )
                            }

                            if (insufficientStorage != null) {
                                InsufficientStorageDialog(
                                    info = insufficientStorage!!,
                                    onDismiss = viewModel::dismissInsufficientStorage,
                                    onManageStorage = viewModel::openStorageSettings
                                )
                            }

                            if (showSyncDialog) {
                                AlertDialog(
                                    onDismissRequest = { showSyncDialog = false },
                                    title = { Text(stringResource(R.string.sync_available_title)) },
                                    text = { Text(stringResource(R.string.sync_available_msg)) },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            // Trigger sync - in a real app we might show a separate progress UI
                                            showSyncDialog = false
                                        }) {
                                            Text(stringResource(R.string.sync_download))
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showSyncDialog = false }) {
                                            Text(stringResource(R.string.sync_later))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateNotificationDialog(
    status: SyncStatus?,
    isDownloading: Boolean,
    progress: DownloadProgress?,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isDownloading,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.update_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (isDownloading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LinearProgressIndicator(
                            progress = { progress?.percentage?.div(100f) ?: 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "${progress?.percentage?.toInt() ?: 0}%",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.sync_status_prefix, progress?.currentEntity ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (status != null) {
                    val updateItems = mutableListOf<String>()
                    if (status.newSongs > 0) {
                        updateItems.add(pluralStringResource(R.plurals.update_item_songs, status.newSongs, status.newSongs))
                    }
                    if (status.newArtists > 0) {
                        updateItems.add(pluralStringResource(R.plurals.update_item_artists, status.newArtists, status.newArtists))
                    }
                    if (status.newComposers > 0) {
                        updateItems.add(pluralStringResource(R.plurals.update_item_composers, status.newComposers, status.newComposers))
                    }

                    val combinedText = when (updateItems.size) {
                        0 -> ""
                        1 -> updateItems[0]
                        2 -> updateItems[0] + stringResource(R.string.update_conjunction_and) + updateItems[1]
                        else -> {
                            val allButLast = updateItems.dropLast(1).joinToString(stringResource(R.string.update_conjunction_comma))
                            allButLast + stringResource(R.string.update_conjunction_and) + updateItems.last()
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (combinedText.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.update_msg_combined, combinedText),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 24.sp
                                    ),
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                        }
                        
                        if (!status.requiresFullRefresh) {
                            Text(
                                text = stringResource(R.string.update_only_new),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = stringResource(R.string.update_ask_download),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (!isDownloading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.btn_later),
                                maxLines = 1,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Button(
                            onClick = onDownload,
                            modifier = Modifier.weight(1f).height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.btn_download_now),
                                maxLines = 1,
                                softWrap = false,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StorageWarningDialog(
    info: StorageUtils.SpaceInfo,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onManageStorage: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.update_title)) },
        text = {
            Text(stringResource(R.string.storage_low_warning_msg, info.usedPercentage))
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.btn_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onManageStorage) {
                Text(stringResource(R.string.btn_manage_storage))
            }
        }
    )
}

@Composable
fun InsufficientStorageDialog(
    info: StorageUtils.SpaceInfo,
    onDismiss: () -> Unit,
    onManageStorage: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.storage_insufficient_title)) },
        text = {
            Text(
                stringResource(
                    R.string.storage_insufficient_msg,
                    StorageUtils.formatSize(info.requiredBytes),
                    StorageUtils.formatSize(info.availableBytes),
                    StorageUtils.formatSize(info.additionalNeededBytes)
                )
            )
        },
        confirmButton = {
            Button(onClick = onManageStorage) {
                Text(stringResource(R.string.btn_manage_storage))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_ok))
            }
        }
    )
}
