package com.maralyrics.laitei.presentation

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.laitei.domain.model.*
import com.maralyrics.laitei.domain.repository.SettingsRepository
import com.maralyrics.laitei.domain.usecase.GetSettingsUseCase
import com.maralyrics.laitei.domain.usecase.SyncDatabaseUseCase
import com.maralyrics.laitei.domain.usecase.InsufficientStorageException
import com.maralyrics.laitei.utils.StorageUtils
import com.maralyrics.laitei.presentation.common.notification.NotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    getSettingsUseCase: GetSettingsUseCase,
    private val settingsRepository: SettingsRepository,
    private val syncDatabaseUseCase: SyncDatabaseUseCase,
    private val notificationManager: NotificationManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _syncAvailable = MutableStateFlow(false)
    val syncAvailable: StateFlow<Boolean> = _syncAvailable.asStateFlow()

    private val _updateStatus = MutableStateFlow<SyncStatus?>(null)
    val updateStatus: StateFlow<SyncStatus?> = _updateStatus.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress.asStateFlow()

    private val _showStorageWarning = MutableStateFlow<StorageUtils.SpaceInfo?>(null)
    val showStorageWarning: StateFlow<StorageUtils.SpaceInfo?> = _showStorageWarning.asStateFlow()

    private val _insufficientStorage = MutableStateFlow<StorageUtils.SpaceInfo?>(null)
    val insufficientStorage: StateFlow<StorageUtils.SpaceInfo?> = _insufficientStorage.asStateFlow()

    private val _lastRoute = MutableStateFlow<String?>(null)
    val lastRoute: StateFlow<String?> = _lastRoute.asStateFlow()

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
        viewModelScope.launch {
            syncDatabaseUseCase.updateStatus.collect { status ->
                if (status != null && status.isAvailable) {
                    _updateStatus.value = status
                } else if (status == null || !status.isAvailable) {
                    _updateStatus.value = null
                }
            }
        }

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

    private fun checkForUpdates() {
        viewModelScope.launch {
            syncDatabaseUseCase.checkForUpdates().onSuccess { status ->
                if (status.isAvailable) {
                    _updateStatus.value = status
                }
            }
        }
    }

    fun startUpdateDownload() {
        viewModelScope.launch {
            // Check if there are actually items to download
            val status = _updateStatus.value
            if (status == null || !status.isAvailable) {
                _updateStatus.value = null
                return@launch
            }

            _isDownloading.value = true
            syncDatabaseUseCase.checkAndSync(
                isAutomatic = false,
                onProgress = { progress ->
                    _downloadProgress.value = progress
                }
            ).onSuccess { result ->
                _isDownloading.value = false
                _updateStatus.value = null
                _downloadProgress.value = null
                if (result.updatedSongs > 0) {
                    notificationManager.showSyncSuccess()
                }
            }.onFailure { exception ->
                _isDownloading.value = false
                _downloadProgress.value = null
                
                if (exception is InsufficientStorageException) {
                    if (exception.info.isEnoughSpace) {
                        _showStorageWarning.value = exception.info
                    } else {
                        _insufficientStorage.value = exception.info
                    }
                } else {
                    notificationManager.showSyncFailed()
                }
            }
        }
    }

    fun confirmDownloadWithStorageWarning() {
        val info = _showStorageWarning.value ?: return
        _showStorageWarning.value = null
        viewModelScope.launch {
            _isDownloading.value = true
            // No need to re-check here as the use case will check again in Stage 2
            syncDatabaseUseCase.checkAndSync(
                isAutomatic = false,
                onProgress = { _downloadProgress.value = it }
            ).onSuccess { result ->
                _isDownloading.value = false
                _updateStatus.value = null
                _downloadProgress.value = null
                if (result.updatedSongs > 0) {
                    notificationManager.showSyncSuccess()
                }
            }.onFailure { exception ->
                _isDownloading.value = false
                _downloadProgress.value = null
                if (exception is InsufficientStorageException) {
                    _insufficientStorage.value = exception.info
                } else {
                    notificationManager.showSyncFailed()
                }
            }
        }
    }

    fun cancelDownloadWithStorageWarning() {
        _showStorageWarning.value = null
    }

    fun dismissInsufficientStorage() {
        _insufficientStorage.value = null
    }

    fun dismissUpdate() {
        _updateStatus.value = null
    }

    fun openStorageSettings() {
        val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    companion object {
        const val CURRENT_PRIVACY_VERSION = "1.0.1"
    }
}
