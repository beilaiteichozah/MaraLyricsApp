package com.maralyrics.laitei.presentation.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject

// A release's Play Console-assigned priority (0-5) at or above this is treated
// as critical and uses an Immediate (blocking) update instead of a Flexible one.
private const val HIGH_PRIORITY_THRESHOLD = 4

sealed interface PlayUpdateState {
    object Idle : PlayUpdateState
    data class Available(val isImmediate: Boolean) : PlayUpdateState
    object Downloaded : PlayUpdateState
}

/**
 * Google Play In-App Updates (updates the app itself via Google Play).
 * Deliberately separate from [com.maralyrics.laitei.domain.usecase.SyncDatabaseUseCase],
 * which checks for new song data, not new app releases.
 */
@HiltViewModel
class PlayUpdateViewModel @Inject constructor(
    private val appUpdateManager: AppUpdateManager
) : ViewModel() {

    private val _state = MutableStateFlow<PlayUpdateState>(PlayUpdateState.Idle)
    val state: StateFlow<PlayUpdateState> = _state.asStateFlow()

    // Only nag the user with the dismissible "Update available" prompt once per session.
    // A required (immediate) update is never gated by this, since the user can't opt out.
    private var hasPromptedThisSession = false

    private val installStateListener = InstallStateUpdatedListener { installState ->
        when (installState.installStatus()) {
            InstallStatus.DOWNLOADED -> _state.value = PlayUpdateState.Downloaded
            InstallStatus.INSTALLED -> _state.value = PlayUpdateState.Idle
            else -> Unit
        }
    }

    init {
        appUpdateManager.registerListener(installStateListener)
    }

    /** Call on app start and again on every onResume (Play recommends re-checking on resume). */
    fun checkForUpdate() {
        viewModelScope.launch {
            val info = runCatching { appUpdateManager.awaitAppUpdateInfo() }.getOrNull()
                // Not installed via Google Play (e.g. sideloaded), or Play services
                // unavailable — fail gracefully with no error shown to the user.
                ?: return@launch

            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                _state.value = PlayUpdateState.Downloaded
                return@launch
            }

            when (info.updateAvailability()) {
                UpdateAvailability.UPDATE_AVAILABLE -> {
                    val isImmediate = info.updatePriority() >= HIGH_PRIORITY_THRESHOLD &&
                        info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                    when {
                        isImmediate -> _state.value = PlayUpdateState.Available(isImmediate = true)
                        info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) && !hasPromptedThisSession ->
                            _state.value = PlayUpdateState.Available(isImmediate = false)
                    }
                }
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    // A required update was interrupted (e.g. app backgrounded) — resume it.
                    _state.value = PlayUpdateState.Available(isImmediate = true)
                }
                else -> Unit
            }
        }
    }

    fun startUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        val current = _state.value
        if (current !is PlayUpdateState.Available) return

        viewModelScope.launch {
            val info = runCatching { appUpdateManager.awaitAppUpdateInfo() }.getOrNull() ?: return@launch
            val type = if (current.isImmediate) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE
            if (!info.isUpdateTypeAllowed(type)) return@launch

            runCatching {
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    launcher,
                    AppUpdateOptions.newBuilder(type).build()
                )
            }

            if (!current.isImmediate) {
                hasPromptedThisSession = true
                _state.value = PlayUpdateState.Idle
            }
        }
    }

    /** User dismissed the "Update available" prompt — don't show it again this session. */
    fun dismissPrompt() {
        hasPromptedThisSession = true
        _state.value = PlayUpdateState.Idle
    }

    /** User tapped "Restart" after a flexible update finished downloading. */
    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }

    override fun onCleared() {
        super.onCleared()
        appUpdateManager.unregisterListener(installStateListener)
    }
}

private suspend fun AppUpdateManager.awaitAppUpdateInfo(): AppUpdateInfo =
    suspendCancellableCoroutine { continuation ->
        appUpdateInfo
            .addOnSuccessListener { info -> continuation.resumeWith(Result.success(info)) }
            .addOnFailureListener { error -> continuation.cancel(error) }
    }
