package com.maralyrics.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.domain.model.*
import com.maralyrics.domain.repository.SongRepository
import com.maralyrics.domain.usecase.GetSettingsUseCase
import com.maralyrics.domain.usecase.SyncDatabaseUseCase
import com.maralyrics.domain.usecase.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    private val syncDatabaseUseCase: SyncDatabaseUseCase,
    private val songRepository: SongRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = getSettingsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _songCount = MutableStateFlow(0)
    val songCount: StateFlow<Int> = _songCount.asStateFlow()

    private val _dbSize = MutableStateFlow(0L)
    val dbSize: StateFlow<Long> = _dbSize.asStateFlow()

    init {
        refreshInfo()
    }

    private fun refreshInfo() {
        viewModelScope.launch {
            _songCount.value = songRepository.getLocalSongCount()
            _dbSize.value = songRepository.getDatabaseSizeBytes()
        }
    }

    fun updateLanguage(language: AppLanguage) {
        viewModelScope.launch {
            updateSettingsUseCase.updateLanguage(language)
        }
    }

    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch {
            updateSettingsUseCase.updateTheme(theme)
        }
    }

    fun updateCategory(category: SongCategory) {
        viewModelScope.launch {
            updateSettingsUseCase.updateCategory(category)
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            syncDatabaseUseCase.checkAndSync()
            refreshInfo()
        }
    }

    fun redownloadDatabase() {
        viewModelScope.launch {
            syncDatabaseUseCase.fullDownload { }
            refreshInfo()
        }
    }
}
