package com.maralyrics.laitei.presentation.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.laitei.domain.model.Profile
import com.maralyrics.laitei.domain.model.ProfileType
import com.maralyrics.laitei.domain.model.Song
import com.maralyrics.laitei.domain.usecase.GetProfileDetailUseCase
import com.maralyrics.laitei.domain.usecase.SyncDatabaseUseCase
import com.maralyrics.laitei.domain.usecase.InsufficientStorageException
import com.maralyrics.laitei.utils.StorageUtils
import com.maralyrics.laitei.presentation.common.notification.NotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileDetailViewModel @Inject constructor(
    private val getProfileDetailUseCase: GetProfileDetailUseCase,
    private val syncDatabaseUseCase: SyncDatabaseUseCase,
    private val notificationManager: NotificationManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val slug: String = checkNotNull(savedStateHandle["slug"])
    private val type: ProfileType = ProfileType.valueOf(checkNotNull(savedStateHandle["type"]))

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            val result = getProfileDetailUseCase(slug, type)
            if (result != null) {
                result.songs.collectLatest { songs ->
                    _uiState.value = ProfileUiState.Success(result.profile, songs)
                }
            } else {
                _uiState.value = ProfileUiState.Error(type)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            syncDatabaseUseCase.checkAndSync().onFailure { exception ->
                if (exception is InsufficientStorageException) {
                    notificationManager.showStorageWarning(StorageUtils.getUsedStoragePercentage())
                }
            }
            _isRefreshing.value = false
            loadProfile()
        }
    }
}

sealed interface ProfileUiState {
    object Loading : ProfileUiState
    data class Success(val profile: Profile, val songs: List<Song>) : ProfileUiState
    data class Error(val type: ProfileType) : ProfileUiState
}
