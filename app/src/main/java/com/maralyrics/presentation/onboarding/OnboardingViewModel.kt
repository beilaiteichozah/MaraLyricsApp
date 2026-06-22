package com.maralyrics.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.domain.usecase.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val updateSettingsUseCase: UpdateSettingsUseCase
) : ViewModel() {

    fun completeOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            updateSettingsUseCase.markOnboardingComplete()
            onComplete()
        }
    }
}
