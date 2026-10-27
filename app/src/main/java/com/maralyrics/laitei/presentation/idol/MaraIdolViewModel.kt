package com.maralyrics.laitei.presentation.idol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.laitei.domain.model.IdolSeason
import com.maralyrics.laitei.domain.repository.MaraIdolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MaraIdolUiState {
    data object Loading : MaraIdolUiState
    data class Success(val seasons: List<IdolSeason>, val isFromCache: Boolean) : MaraIdolUiState
    data object Error : MaraIdolUiState
}

@HiltViewModel
class MaraIdolViewModel @Inject constructor(
    private val maraIdolRepository: MaraIdolRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MaraIdolUiState>(MaraIdolUiState.Loading)
    val uiState: StateFlow<MaraIdolUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = MaraIdolUiState.Loading
            maraIdolRepository.getSeasons()
                .onSuccess { _uiState.value = MaraIdolUiState.Success(it.data, it.isFromCache) }
                .onFailure { _uiState.value = MaraIdolUiState.Error }
        }
    }
}
