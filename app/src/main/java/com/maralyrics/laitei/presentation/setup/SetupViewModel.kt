package com.maralyrics.laitei.presentation.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.laitei.domain.model.AppLanguage
import com.maralyrics.laitei.domain.model.DownloadProgress
import com.maralyrics.laitei.domain.usecase.GetSettingsUseCase
import com.maralyrics.laitei.domain.usecase.SyncDatabaseUseCase
import com.maralyrics.laitei.domain.usecase.UpdateSettingsUseCase
import com.maralyrics.laitei.domain.usecase.InsufficientStorageException
import com.maralyrics.laitei.utils.StorageUtils
import com.maralyrics.laitei.presentation.common.notification.NotificationManager
import com.maralyrics.laitei.utils.ConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    private val syncDatabaseUseCase: SyncDatabaseUseCase,
    private val notificationManager: NotificationManager,
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getSettingsUseCase().collect { settings ->
                _uiState.update { it.copy(selectedLanguage = settings.language) }
            }
        }

        viewModelScope.launch {
            connectivityObserver.observe().collect { status ->
                _uiState.update { it.copy(isOnline = status == ConnectivityObserver.Status.Available) }
            }
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
        if (nextStep == 1 && _uiState.value.isOnline) {
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
                com.maralyrics.laitei.presentation.common.notification.NotificationData(
                    message = "sync_downloading_db",
                    type = com.maralyrics.laitei.presentation.common.notification.NotificationType.SYNCING,
                    showProgress = true
                )
            )
            syncDatabaseUseCase.fullDownload { progress ->
                _uiState.update { it.copy(downloadProgress = progress) }
            }.onSuccess {
                updateSettingsUseCase.markSetupComplete()
                _uiState.update { it.copy(isDownloadComplete = true) }
                notificationManager.showNotification(
                    com.maralyrics.laitei.presentation.common.notification.NotificationData(
                        message = "sync_download_success",
                        type = com.maralyrics.laitei.presentation.common.notification.NotificationType.DOWNLOAD_COMPLETE
                    )
                )
            }.onFailure { exception ->
                _uiState.update { it.copy(error = exception.message) }
                
                if (exception is InsufficientStorageException) {
                    notificationManager.showStorageWarning(StorageUtils.getUsedStoragePercentage())
                } else {
                    notificationManager.showNotification(
                        com.maralyrics.laitei.presentation.common.notification.NotificationData(
                            message = "sync_download_failed",
                            type = com.maralyrics.laitei.presentation.common.notification.NotificationType.ERROR
                        )
                    )
                }
            }
        }
    }
}

data class SetupUiState(
    val currentStep: Int = 0, // 0: Category, 1: Download
    val selectedLanguage: AppLanguage = AppLanguage.ENGLISH,
    val selectedCategories: List<String> = listOf("Gospel"),
    val downloadProgress: DownloadProgress? = null,
    val isDownloadComplete: Boolean = false,
    val isOnline: Boolean = true,
    val error: String? = null
)
