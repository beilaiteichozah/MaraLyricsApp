package com.maralyrics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.domain.model.AppSettings
import com.maralyrics.domain.model.AppTheme
import com.maralyrics.domain.model.SongCategory
import com.maralyrics.domain.usecase.GetSettingsUseCase
import com.maralyrics.domain.usecase.SyncDatabaseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val syncDatabaseUseCase: SyncDatabaseUseCase
) : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _syncAvailable = MutableStateFlow(false)
    val syncAvailable: StateFlow<Boolean> = _syncAvailable.asStateFlow()

    private val settings = getSettingsUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val theme: StateFlow<AppTheme> = settings
        .map { it?.theme ?: AppTheme.SYSTEM }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.SYSTEM)

    val isSetupComplete: StateFlow<Boolean> = settings
        .map { it?.isSetupComplete ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        settings.onEach {
            if (it != null) {
                _isReady.value = true
                if (it.isSetupComplete) {
                    checkForUpdates()
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun checkForUpdates() {
        viewModelScope.launch {
            // Check if updates are available but don't download yet
            // This would normally be handled by SyncRepository.checkForUpdates()
            // For now just a simple call
        }
    }
}
