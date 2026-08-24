package com.maralyrics.laitei.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.laitei.domain.model.AppLanguage
import com.maralyrics.laitei.domain.repository.SettingsRepository
import com.maralyrics.laitei.domain.usecase.GetSettingsUseCase
import com.maralyrics.laitei.domain.usecase.UpdateSettingsUseCase
import com.maralyrics.laitei.presentation.MainViewModel
import com.maralyrics.laitei.presentation.common.notification.NotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    private val settingsRepository: SettingsRepository,
    private val notificationManager: NotificationManager
) : ViewModel() {

    private val _selectedLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val selectedLanguage: StateFlow<AppLanguage> = _selectedLanguage.asStateFlow()

    private val _privacyAccepted = MutableStateFlow(false)
    val privacyAccepted: StateFlow<Boolean> = _privacyAccepted.asStateFlow()

    init {
        viewModelScope.launch {
            getSettingsUseCase().collect { settings ->
                _selectedLanguage.value = settings.language
                _privacyAccepted.value = settings.privacyPolicyAccepted && settings.privacyPolicyVersion == MainViewModel.CURRENT_PRIVACY_VERSION
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        _selectedLanguage.value = language
        viewModelScope.launch {
            updateSettingsUseCase.updateLanguage(language)
            val langKey = when(language) {
                AppLanguage.MARA -> "lang_mara"
                AppLanguage.BURMESE -> "lang_burmese"
                else -> "lang_english"
            }
            notificationManager.showLanguageChanged(langKey)
        }
    }

    // Waits until the persisted settings reflect the currently selected language, so
    // screens that read locale-dependent resources (e.g. the Privacy Policy dialog)
    // don't render under the stale locale while the DataStore write is still in flight.
    suspend fun awaitLanguagePersisted() {
        getSettingsUseCase().first { it.language == _selectedLanguage.value }
    }

    fun acceptPrivacyPolicy() {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            settingsRepository.updatePrivacyPolicyAcceptance(true, MainViewModel.CURRENT_PRIVACY_VERSION, timestamp)
            _privacyAccepted.value = true
        }
    }

    fun completeOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            if (_privacyAccepted.value) {
                updateSettingsUseCase.markOnboardingComplete()
                onComplete()
            }
        }
    }
}
