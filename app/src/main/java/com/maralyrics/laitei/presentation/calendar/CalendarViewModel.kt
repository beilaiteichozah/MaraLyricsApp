package com.maralyrics.laitei.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.laitei.domain.model.CommunityCalendar
import com.maralyrics.laitei.domain.model.CommunityEvent
import com.maralyrics.laitei.domain.model.EventMoment
import com.maralyrics.laitei.domain.repository.CalendarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CalendarUiState(
    val calendars: List<CommunityCalendar> = emptyList(),
    val selected: CommunityCalendar? = null,
    val year: Int = EventMoment.today().year,
    val events: List<CommunityEvent> = emptyList(),
    /** True while the calendar list or the selected year's events are loading. */
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val isFromCache: Boolean = false
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    // Only the latest events request may write state — flipping quickly through years or
    // calendars would otherwise let a slow earlier response overwrite the one on screen.
    private var eventsJob: Job? = null

    init {
        loadCalendars()
    }

    fun retry() {
        if (_state.value.selected == null) loadCalendars() else loadEvents()
    }

    fun selectCalendar(calendar: CommunityCalendar) {
        if (calendar.slug == _state.value.selected?.slug) return
        calendarRepository.setSelectedCalendarSlug(calendar.slug)
        _state.update { it.copy(selected = calendar) }
        loadEvents()
    }

    fun changeYear(delta: Int) {
        _state.update { it.copy(year = it.year + delta) }
        loadEvents()
    }

    private fun loadCalendars() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadFailed = false) }
            calendarRepository.getCalendars()
                .onSuccess { result ->
                    val calendars = result.data
                    val remembered = calendarRepository.getSelectedCalendarSlug()
                    val selected = calendars.firstOrNull { it.slug == remembered } ?: calendars.firstOrNull()
                    _state.update {
                        it.copy(
                            calendars = calendars,
                            selected = selected,
                            isLoading = selected != null,
                            isFromCache = result.isFromCache
                        )
                    }
                    if (selected != null) loadEvents()
                }
                .onFailure {
                    _state.update { it.copy(isLoading = false, loadFailed = true) }
                }
        }
    }

    private fun loadEvents() {
        val calendar = _state.value.selected ?: return
        val year = _state.value.year
        eventsJob?.cancel()
        eventsJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadFailed = false, events = emptyList()) }
            calendarRepository.getEvents(calendar.slug, year)
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            events = result.data,
                            isLoading = false,
                            isFromCache = result.isFromCache
                        )
                    }
                }
                .onFailure {
                    _state.update { it.copy(isLoading = false, loadFailed = true) }
                }
        }
    }
}
