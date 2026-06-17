package com.maralyrics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.maralyrics.R
import com.maralyrics.presentation.MainViewModel
import com.maralyrics.presentation.navigation.MaraLyricsNavHost
import com.maralyrics.presentation.theme.MaraLyricsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val isReady by viewModel.isReady.collectAsState()
            val theme by viewModel.theme.collectAsState()
            val isSetupComplete by viewModel.isSetupComplete.collectAsState()
            val syncAvailable by viewModel.syncAvailable.collectAsState()
            
            var showSyncDialog by remember { mutableStateOf(false) }
            
            LaunchedEffect(syncAvailable) {
                if (syncAvailable) showSyncDialog = true
            }

            splashScreen.setKeepOnScreenCondition { !isReady }

            MaraLyricsTheme(theme = theme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (isReady) {
                        MaraLyricsNavHost(isSetupComplete = isSetupComplete)
                        
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