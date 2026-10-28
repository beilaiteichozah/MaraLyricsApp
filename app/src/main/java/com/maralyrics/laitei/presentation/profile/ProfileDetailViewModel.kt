package com.maralyrics.laitei.presentation.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.laitei.domain.model.Profile
import com.maralyrics.laitei.domain.model.ProfileExtras
import com.maralyrics.laitei.domain.model.ProfileType
import com.maralyrics.laitei.domain.model.Song
import com.maralyrics.laitei.domain.repository.ProfileExtrasRepository
import com.maralyrics.laitei.domain.usecase.GetProfileDetailUseCase
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
class ProfileDetailViewModel @Inject constructor(
    private val getProfileDetailUseCase: GetProfileDetailUseCase,
    private val syncDatabaseUseCase: SyncDatabaseUseCase,
    private val notificationManager: NotificationManager,
    private val songUpdateNotifier: SongUpdateNotifier,
    private val profileExtrasRepository: ProfileExtrasRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val slug: String = checkNotNull(savedStateHandle["slug"])
    private val type: ProfileType = ProfileType.valueOf(checkNotNull(savedStateHandle["type"]))

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // Badges, Green mark and rich bio from the website. Null until loaded, and stays null when
    // offline with nothing saved; the profile reads fine without it.
    private val _extras = MutableStateFlow<ProfileExtras?>(null)
    val extras: StateFlow<ProfileExtras?> = _extras.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadExtras() {
        viewModelScope.launch { _extras.value = profileExtrasRepository.getExtras(slug, type) }
    }

    private fun loadProfile() {
        loadExtras()
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
            syncDatabaseUseCase.checkAndSync()
                .onSuccess { result ->
                    if (result.newSongs > 0) {
                        songUpdateNotifier.notifyNewSongs(result.newSongs)
                    } else if (result.updatedSongs > 0) {
                        notificationManager.showSyncSuccess()
                    }
                }
                .onFailure { exception ->
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
