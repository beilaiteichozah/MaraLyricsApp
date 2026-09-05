package com.maralyrics.laitei.presentation.update

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.maralyrics.laitei.R

/**
 * "Update available" prompt for Google Play In-App Updates. Dismissible for a normal
 * (Flexible) update; non-dismissible for a high-priority (Immediate) update, since Play
 * itself will take over with a blocking screen once [onUpdate] is triggered.
 */
@Composable
fun PlayUpdateAvailableDialog(
    isImmediate: Boolean,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    // Captured here (outside AlertDialog's own window boundary) and re-provided below,
    // since its Dialog can otherwise lose the app's in-app locale override for its content.
    val localizedContext = LocalContext.current
    val localizedConfiguration = LocalConfiguration.current
    fun localized(content: @Composable () -> Unit): @Composable () -> Unit = {
        CompositionLocalProvider(
            LocalContext provides localizedContext,
            LocalConfiguration provides localizedConfiguration
        ) { content() }
    }

    AlertDialog(
        onDismissRequest = { if (!isImmediate) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isImmediate,
            dismissOnClickOutside = !isImmediate
        ),
        icon = localized {
            Icon(imageVector = Icons.Default.SystemUpdate, contentDescription = null)
        },
        title = localized { Text(stringResource(R.string.play_update_available_title)) },
        text = localized { Text(stringResource(R.string.play_update_available_desc)) },
        confirmButton = localized {
            Button(onClick = onUpdate) {
                Text(stringResource(R.string.play_update_action_update))
            }
        },
        dismissButton = if (isImmediate) null else localized {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.play_update_action_later))
            }
        }
    )
}

/**
 * Shows the "Update ready — Restart to install" snackbar once, with an indefinite
 * duration, per Google's own guidance for the post-Flexible-download state.
 */
@Composable
fun PlayUpdateReadySnackbar(
    hostState: SnackbarHostState,
    onRestart: () -> Unit
) {
    val message = stringResource(R.string.play_update_ready_message)
    val actionLabel = stringResource(R.string.play_update_action_restart)

    LaunchedEffect(hostState) {
        val result = hostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = SnackbarDuration.Indefinite
        )
        if (result == SnackbarResult.ActionPerformed) {
            onRestart()
        }
    }
}
