package com.maralyrics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.domain.model.*
import com.maralyrics.domain.repository.SettingsRepository
import com.maralyrics.domain.usecase.GetSettingsUseCase
import com.maralyrics.domain.usecase.SyncDatabaseUseCase
import com.maralyrics.presentation.common.notification.NotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val settingsRepository: SettingsRepository,
    private val syncDatabaseUseCase: SyncDatabaseUseCase,
    private val notificationManager: NotificationManager
) : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _syncAvailable = MutableStateFlow(false)
    val syncAvailable: StateFlow<Boolean> = _syncAvailable.asStateFlow()

    private val _lastRoute = MutableStateFlow<String?>(null)
    val lastRoute: StateFlow<String?> = _lastRoute.asStateFlow()

    private val settings = getSettingsUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val theme: StateFlow<AppTheme> = settings
        .map { it?.theme ?: AppTheme.SYSTEM }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.SYSTEM)

    val colorTheme: StateFlow<com.maralyrics.domain.model.AppColorTheme> = settings
        .map { it?.colorTheme ?: com.maralyrics.domain.model.AppColorTheme.MARA }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.maralyrics.domain.model.AppColorTheme.MARA)

    val isSetupComplete: StateFlow<Boolean> = settings
        .map { it?.isSetupComplete ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val language: StateFlow<AppLanguage> = settings
        .map { it?.language ?: AppLanguage.ENGLISH }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.ENGLISH)

    val hasCompletedOnboarding: StateFlow<Boolean> = settings
        .map { it?.hasCompletedOnboarding ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        settings.onEach {
            if (it != null) {
                if (it.isSetupComplete && it.hasCompletedOnboarding && it.resumeSessionEnabled) {
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
                if (it.isSetupComplete) {
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
            syncDatabaseUseCase.checkAndSync(isAutomatic = true).onSuccess { result ->
                if (result.updatedSongs > 0) {
                    notificationManager.showNotification(
                        com.maralyrics.presentation.common.notification.NotificationData(
                            message = "notif_sync_success",
                            type = com.maralyrics.presentation.common.notification.NotificationType.UPDATE_AVAILABLE
                        )
                    )
                }
            }
        }
    }
}
