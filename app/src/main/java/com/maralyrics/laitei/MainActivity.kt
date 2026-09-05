package com.maralyrics.laitei

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.maralyrics.laitei.R
import com.maralyrics.laitei.presentation.MainViewModel
import com.maralyrics.laitei.presentation.common.notification.NotificationHost
import com.maralyrics.laitei.presentation.common.notification.NotificationManager
import com.maralyrics.laitei.presentation.update.PlayUpdateAvailableDialog
import com.maralyrics.laitei.presentation.update.PlayUpdateReadySnackbar
import com.maralyrics.laitei.presentation.update.PlayUpdateState
import com.maralyrics.laitei.presentation.update.PlayUpdateViewModel
import com.maralyrics.laitei.utils.LocalizedContextWrapper
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

    private var pendingIntentState by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingIntentState = intent

        setContent {
            val viewModel: MainViewModel = hiltViewModel()

            // A notification tap or home screen widget tap arrives as extras on the
            // launching intent (initial launch here, or onNewIntent while already open).
            LaunchedEffect(pendingIntentState) {
                pendingIntentState?.let { intent ->
                    val songId = intent.getLongExtra(EXTRA_OPEN_SONG_ID, -1L).takeIf { it != -1L }
                    val openSearch = intent.getBooleanExtra(EXTRA_OPEN_SEARCH, false)
                    if (songId != null || openSearch) {
                        viewModel.handleDeepLink(songId, openSearch)
                    }
                    pendingIntentState = null
                }
            }
            val playUpdateViewModel: PlayUpdateViewModel = hiltViewModel()
            val isReady by viewModel.isReady.collectAsState()
            val theme by viewModel.theme.collectAsState()
            val language by viewModel.language.collectAsState()
            val colorTheme by viewModel.colorTheme.collectAsState()
            val isSetupComplete by viewModel.isSetupComplete.collectAsState()
            val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsState()
            val privacyAccepted by viewModel.privacyAccepted.collectAsState()
            val lastRoute by viewModel.lastRoute.collectAsState()
            val syncAvailable by viewModel.syncAvailable.collectAsState()

            val networkStatus by connectivityObserver.observe().collectAsState(initial = ConnectivityObserver.Status.Available)
            
            var showSyncDialog by remember { mutableStateOf(false) }

            LaunchedEffect(syncAvailable) {
                if (syncAvailable) showSyncDialog = true
            }

            // Google Play In-App Updates — separate from the song-data sync above.
            // Checked on first composition and again on every onResume, per Play's guidance.
            val playUpdateState by playUpdateViewModel.state.collectAsState()
            val playUpdateLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartIntentSenderForResult()
            ) { playUpdateViewModel.checkForUpdate() }
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(Unit) {
                playUpdateViewModel.checkForUpdate()
            }

            // Required on Android 13+ for the "new songs" notification to ever be
            // allowed to show — asked once per install, like most apps do at launch.
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { /* Result isn't acted on directly — SongUpdateNotifier checks at post-time. */ }
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        playUpdateViewModel.checkForUpdate()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
                            val pendingSongDeepLink by viewModel.pendingSongDeepLink.collectAsState()
                            val pendingSearchDeepLink by viewModel.pendingSearchDeepLink.collectAsState()
                            MaraLyricsNavHost(
                                isSetupComplete = isSetupComplete,
                                hasCompletedOnboarding = hasCompletedOnboarding,
                                privacyAccepted = privacyAccepted,
                                initialRoute = lastRoute,
                                onRouteChanged = viewModel::onRouteChanged,
                                pendingSongDeepLink = pendingSongDeepLink,
                                onSongDeepLinkConsumed = viewModel::consumeSongDeepLink,
                                pendingSearchDeepLink = pendingSearchDeepLink,
                                onSearchDeepLinkConsumed = viewModel::consumeSearchDeepLink
                            )
                            NotificationHost(manager = notificationManager)

                            if (showSyncDialog) {
                                // Re-provided inside each lambda since AlertDialog's own Dialog
                                // window can otherwise lose the app's in-app locale override.
                                fun localized(content: @Composable () -> Unit): @Composable () -> Unit = {
                                    CompositionLocalProvider(
                                        LocalContext provides wrappedContext,
                                        LocalConfiguration provides configuration
                                    ) { content() }
                                }
                                AlertDialog(
                                    onDismissRequest = { showSyncDialog = false },
                                    title = localized { Text(stringResource(R.string.sync_available_title)) },
                                    text = localized { Text(stringResource(R.string.sync_available_msg)) },
                                    confirmButton = localized {
                                        TextButton(onClick = {
                                            // Trigger sync - in a real app we might show a separate progress UI
                                            showSyncDialog = false
                                        }) {
                                            Text(stringResource(R.string.sync_download))
                                        }
                                    },
                                    dismissButton = localized {
                                        TextButton(onClick = { showSyncDialog = false }) {
                                            Text(stringResource(R.string.sync_later))
                                        }
                                    }
                                )
                            }

                            // Google Play In-App Updates — kept visually and semantically
                            // separate from the song-data sync UI above.
                            when (val state = playUpdateState) {
                                is PlayUpdateState.Available -> {
                                    PlayUpdateAvailableDialog(
                                        isImmediate = state.isImmediate,
                                        onUpdate = {
                                            playUpdateViewModel.startUpdate(playUpdateLauncher)
                                        },
                                        onDismiss = playUpdateViewModel::dismissPrompt
                                    )
                                }
                                PlayUpdateState.Downloaded -> {
                                    PlayUpdateReadySnackbar(
                                        hostState = snackbarHostState,
                                        onRestart = playUpdateViewModel::completeUpdate
                                    )
                                }
                                PlayUpdateState.Idle -> Unit
                            }

                            Box(modifier = Modifier.fillMaxSize()) {
                                SnackbarHost(
                                    hostState = snackbarHostState,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .navigationBarsPadding()
                                        .padding(bottom = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingIntentState = intent
    }

    companion object {
        const val EXTRA_OPEN_SONG_ID = "open_song_id"
        const val EXTRA_OPEN_SEARCH = "open_search"
    }
}
