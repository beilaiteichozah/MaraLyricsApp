package com.maralyrics.laitei.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.laitei.domain.model.*
import com.maralyrics.laitei.domain.repository.SettingsRepository
import com.maralyrics.laitei.domain.usecase.GetSettingsUseCase
import com.maralyrics.laitei.domain.usecase.SyncDatabaseUseCase
import com.maralyrics.laitei.domain.usecase.InsufficientStorageException
import com.maralyrics.laitei.utils.StorageUtils
import com.maralyrics.laitei.presentation.common.notification.NotificationManager
import com.maralyrics.laitei.presentation.common.notification.SongUpdateNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    getSettingsUseCase: GetSettingsUseCase,
    private val settingsRepository: SettingsRepository,
    private val syncDatabaseUseCase: SyncDatabaseUseCase,
    private val notificationManager: NotificationManager,
    private val songUpdateNotifier: SongUpdateNotifier
) : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _syncAvailable = MutableStateFlow(false)
    val syncAvailable: StateFlow<Boolean> = _syncAvailable.asStateFlow()

    private val _lastRoute = MutableStateFlow<String?>(null)
    val lastRoute: StateFlow<String?> = _lastRoute.asStateFlow()

    // One-shot navigation requests coming from outside the Compose tree — a notification
    // tap or a home screen widget tap. Cleared by the consumer once handled.
    private val _pendingSongDeepLink = MutableStateFlow<Long?>(null)
    val pendingSongDeepLink: StateFlow<Long?> = _pendingSongDeepLink.asStateFlow()

    private val _pendingSearchDeepLink = MutableStateFlow(false)
    val pendingSearchDeepLink: StateFlow<Boolean> = _pendingSearchDeepLink.asStateFlow()

    fun handleDeepLink(songId: Long?, openSearch: Boolean) {
        if (songId != null) _pendingSongDeepLink.value = songId
        if (openSearch) _pendingSearchDeepLink.value = true
    }

    fun consumeSongDeepLink() {
        _pendingSongDeepLink.value = null
    }

    fun consumeSearchDeepLink() {
        _pendingSearchDeepLink.value = false
    }

    private var hasCheckedForUpdatesThisSession = false

    private val settings = getSettingsUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val theme: StateFlow<AppTheme> = settings
        .map { it?.theme ?: AppTheme.SYSTEM }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.SYSTEM)

    val colorTheme: StateFlow<AppColorTheme> = settings
        .map { it?.colorTheme ?: AppColorTheme.MARA }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppColorTheme.MARA)

    val isSetupComplete: StateFlow<Boolean> = settings
        .map { it?.isSetupComplete ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val language: StateFlow<AppLanguage> = settings
        .map { it?.language ?: AppLanguage.ENGLISH }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.ENGLISH)

    val hasCompletedOnboarding: StateFlow<Boolean> = settings
        .map { it?.hasCompletedOnboarding ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val privacyAccepted: StateFlow<Boolean> = settings
        .map { (it?.privacyPolicyAccepted ?: false) && it?.privacyPolicyVersion == CURRENT_PRIVACY_VERSION }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        settings.onEach {
            if (it != null) {
                val isPrivacyValid = it.privacyPolicyAccepted && it.privacyPolicyVersion == CURRENT_PRIVACY_VERSION
                if (it.isSetupComplete && it.hasCompletedOnboarding && isPrivacyValid && it.resumeSessionEnabled) {
                    val route = settingsRepository.getLastRoute().first()
                    // Only restore if it's not a template route (doesn't contain '{')
                    // and it's not empty
                    if (!route.isNullOrBlank() && !route.contains("{") && route != "onboarding") {
                        _lastRoute.value = route
                    } else {
                        _lastRoute.value = "home"
                    }
                } else {
                    _lastRoute.value = "home"
                }
                _isReady.value = true
                if (it.isSetupComplete && isPrivacyValid && !hasCheckedForUpdatesThisSession) {
                    hasCheckedForUpdatesThisSession = true
                    checkForUpdates()
                }
            }
        }.launchIn(viewModelScope)
    }

    fun onRouteChanged(route: String?) {
        viewModelScope.launch {
            val s = settings.value ?: return@launch
            if (s.resumeSessionEnabled) {
                settingsRepository.saveLastRoute(route)
            }
        }
    }

    // Song data (including small lyric corrections) is lightweight, so updates are
    // fetched and applied silently in the background rather than prompting the user —
    // see WorkManager's periodic SyncWorker for the same behavior when the app is closed.
    private fun checkForUpdates() {
        viewModelScope.launch {
            syncDatabaseUseCase.checkAndSync(isAutomatic = true).onSuccess { result ->
                if (result.newSongs > 0) {
                    songUpdateNotifier.notifyNewSongs(result.newSongs)
                } else if (result.updatedSongs > 0) {
                    notificationManager.showSyncSuccess()
                }
            }.onFailure { exception ->
                if (exception is InsufficientStorageException) {
                    notificationManager.showStorageWarning(StorageUtils.getUsedStoragePercentage())
                }
                // Any other failure (offline, server error, etc.) is silently retried
                // on the next automatic check — no need to interrupt the user.
            }
        }
    }

    companion object {
        const val CURRENT_PRIVACY_VERSION = "1.0.1"
    }
}
