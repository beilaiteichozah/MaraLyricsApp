package com.maralyrics.presentation.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.domain.model.AppLanguage
import com.maralyrics.domain.model.DownloadProgress
import com.maralyrics.domain.model.SongCategory
import com.maralyrics.domain.usecase.SyncDatabaseUseCase
import com.maralyrics.domain.usecase.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    private val syncDatabaseUseCase: SyncDatabaseUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            updateSettingsUseCase.updateLanguage(language)
            _uiState.value = _uiState.value.copy(selectedLanguage = language)
        }
    }

    fun setCategory(category: SongCategory) {
        viewModelScope.launch {
            updateSettingsUseCase.updateCategory(category)
            _uiState.value = _uiState.value.copy(selectedCategory = category)
        }
    }

    fun nextStep() {
        val nextStep = _uiState.value.currentStep + 1
        _uiState.value = _uiState.value.copy(currentStep = nextStep)
        if (nextStep == 2) {
            startDownload()
        }
    }

    fun previousStep() {
        if (_uiState.value.currentStep > 0) {
            _uiState.value = _uiState.value.copy(currentStep = _uiState.value.currentStep - 1)
        }
    }

    private fun startDownload() {
        viewModelScope.launch {
            syncDatabaseUseCase.fullDownload { progress ->
                _uiState.value = _uiState.value.copy(downloadProgress = progress)
            }.onSuccess {
                updateSettingsUseCase.markSetupComplete()
                _uiState.value = _uiState.value.copy(isDownloadComplete = true)
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = it.message)
            }
        }
    }
}

data class SetupUiState(
    val currentStep: Int = 0, // 0: Language, 1: Category, 2: Download
    val selectedLanguage: AppLanguage = AppLanguage.MARA,
    val selectedCategory: SongCategory = SongCategory.GOSPEL,
    val downloadProgress: DownloadProgress? = null,
    val isDownloadComplete: Boolean = false,
    val error: String? = null
)
