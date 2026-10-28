package com.maralyrics.laitei.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.laitei.domain.model.CreditsExtras
import com.maralyrics.laitei.domain.repository.CreditsExtrasRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Sponsors, partners and editors from the website; empty until loaded, and when offline with nothing saved. */
@HiltViewModel
class CreditsExtrasViewModel @Inject constructor(
    private val repository: CreditsExtrasRepository
) : ViewModel() {

    private val _extras = MutableStateFlow(CreditsExtras())
    val extras: StateFlow<CreditsExtras> = _extras.asStateFlow()

    init {
        viewModelScope.launch { _extras.value = repository.getExtras() }
    }
}
