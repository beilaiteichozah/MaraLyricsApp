package com.maralyrics.presentation.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.domain.model.Profile
import com.maralyrics.domain.model.ProfileType
import com.maralyrics.domain.model.Song
import com.maralyrics.domain.usecase.GetProfileDetailUseCase
import com.maralyrics.domain.usecase.SyncDatabaseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ProfileDetailViewModel @Inject constructor(
    private val getProfileDetailUseCase: GetProfileDetailUseCase,
    private val syncDatabaseUseCase: SyncDatabaseUseCase,
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
                val typeName = type.name.lowercase(Locale.ROOT)
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                _uiState.value = ProfileUiState.Error("$typeName not found")
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            syncDatabaseUseCase.checkAndSync()
            _isRefreshing.value = false
            loadProfile()
        }
    }
}

sealed interface ProfileUiState {
    object Loading : ProfileUiState
    data class Success(val profile: Profile, val songs: List<Song>) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}
