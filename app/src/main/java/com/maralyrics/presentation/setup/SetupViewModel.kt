package com.maralyrics.presentation.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.domain.model.AppLanguage
import com.maralyrics.domain.model.DownloadProgress
import com.maralyrics.domain.usecase.SyncDatabaseUseCase
import com.maralyrics.domain.usecase.UpdateSettingsUseCase
import com.maralyrics.presentation.common.notification.NotificationManager
import com.maralyrics.utils.ConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    private val syncDatabaseUseCase: SyncDatabaseUseCase,
    private val notificationManager: NotificationManager,
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            connectivityObserver.observe().collect { status ->
                _uiState.update { it.copy(isOnline = status == ConnectivityObserver.Status.Available) }
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            updateSettingsUseCase.updateLanguage(language)
            _uiState.update { it.copy(selectedLanguage = language) }
            notificationManager.showLanguageChanged(if (language == AppLanguage.MARA) "Mara" else "English")
        }
    }

    fun setCategory(category: String) {
        val currentCategories = _uiState.value.selectedCategories.toMutableList()
        if (category == "All") {
            currentCategories.clear()
            currentCategories.add("All")
        } else {
            currentCategories.remove("All")
            if (currentCategories.contains(category)) {
                currentCategories.remove(category)
                if (currentCategories.isEmpty()) currentCategories.add("All")
            } else {
                currentCategories.add(category)
            }
        }
        
        viewModelScope.launch {
            updateSettingsUseCase.updateCategories(currentCategories)
            _uiState.update { it.copy(selectedCategories = currentCategories) }
        }
    }

    fun continueOffline() {
        viewModelScope.launch {
            updateSettingsUseCase.markSetupComplete()
            _uiState.update { it.copy(isDownloadComplete = true) }
        }
    }

    fun nextStep() {
        val nextStep = _uiState.value.currentStep + 1
        _uiState.update { it.copy(currentStep = nextStep) }
        if (nextStep == 2 && _uiState.value.isOnline) {
            startDownload()
        }
    }

    fun previousStep() {
        if (_uiState.value.currentStep > 0) {
            _uiState.update { it.copy(currentStep = _uiState.value.currentStep - 1) }
        }
    }

    fun retryDownload() {
        _uiState.value = _uiState.value.copy(error = null, downloadProgress = null)
        startDownload()
    }

    private fun startDownload() {
        viewModelScope.launch {
            notificationManager.showNotification(
                com.maralyrics.presentation.common.notification.NotificationData(
                    message = "sync_downloading_db",
                    type = com.maralyrics.presentation.common.notification.NotificationType.SYNCING,
                    showProgress = true
                )
            )
            syncDatabaseUseCase.fullDownload { progress ->
                _uiState.update { it.copy(downloadProgress = progress) }
            }.onSuccess {
                updateSettingsUseCase.markSetupComplete()
                _uiState.update { it.copy(isDownloadComplete = true) }
                notificationManager.showNotification(
                    com.maralyrics.presentation.common.notification.NotificationData(
                        message = "sync_download_success",
                        type = com.maralyrics.presentation.common.notification.NotificationType.DOWNLOAD_COMPLETE
                    )
                )
            }.onFailure { exception ->
                _uiState.update { it.copy(error = exception.message) }
                notificationManager.showNotification(
                    com.maralyrics.presentation.common.notification.NotificationData(
                        message = "sync_download_failed",
                        type = com.maralyrics.presentation.common.notification.NotificationType.ERROR
                    )
                )
            }
        }
    }
}

data class SetupUiState(
    val currentStep: Int = 0, // 0: Language, 1: Category, 2: Download
    val selectedLanguage: AppLanguage = AppLanguage.MARA,
    val selectedCategories: List<String> = listOf("Gospel"),
    val downloadProgress: DownloadProgress? = null,
    val isDownloadComplete: Boolean = false,
    val isOnline: Boolean = true,
    val error: String? = null
)
