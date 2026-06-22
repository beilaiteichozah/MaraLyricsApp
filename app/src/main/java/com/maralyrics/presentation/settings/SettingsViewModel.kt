package com.maralyrics.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.domain.model.*
import com.maralyrics.domain.repository.CreditsRepository
import com.maralyrics.domain.repository.SongRepository
import com.maralyrics.domain.usecase.*
import com.maralyrics.presentation.common.notification.NotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    private val syncDatabaseUseCase: SyncDatabaseUseCase,
    private val songRepository: SongRepository,
    private val getAvailableCategoriesUseCase: GetAvailableCategoriesUseCase,
    private val creditsRepository: CreditsRepository,
    private val notificationManager: NotificationManager,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = getSettingsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val availableCategories: StateFlow<List<SongCategory>> = getAvailableCategoriesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val credits: StateFlow<CreditsData> = creditsRepository.getCredits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CreditsData())

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
            val langStr = if (language == AppLanguage.MARA) "Mara" else "English"
            notificationManager.showLanguageChanged(langStr)
        }
    }

    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch {
            updateSettingsUseCase.updateTheme(theme)
            val themeKey = when(theme) {
                AppTheme.LIGHT -> "notif_mode_light"
                AppTheme.DARK -> "notif_mode_dark"
                AppTheme.SYSTEM -> "notif_mode_system"
            }
            notificationManager.showThemeChanged(themeKey)
        }
    }

    fun updateCategory(category: String) {
        viewModelScope.launch {
            val currentSettings = settings.value ?: return@launch
            val currentCategories = currentSettings.defaultCategories.toMutableList()
            
            if (category == "All") {
                currentCategories.clear()
                currentCategories.add("All")
            } else {
                currentCategories.remove("All")
                if (currentCategories.contains(category)) {
                    currentCategories.remove(category)
                    if (currentCategories.isEmpty()) currentCategories.add("All")
                } else {
                    currentCategories.add(category)
                }
            }
            
            updateSettingsUseCase.updateCategories(currentCategories)
            
            val message = if (currentCategories.contains("All")) "All" else currentCategories.joinToString(", ")
            notificationManager.showNotification(
                com.maralyrics.presentation.common.notification.NotificationData(
                    message = "notif_default_cat_updated|$message",
                    type = com.maralyrics.presentation.common.notification.NotificationType.SUCCESS
                )
            )
        }
    }

    fun updateAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            updateSettingsUseCase.updateAutoSync(enabled)
        }
    }

    fun updateWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            updateSettingsUseCase.updateWifiOnly(enabled)
        }
    }

    fun updateResumeSession(enabled: Boolean) {
        viewModelScope.launch {
            updateSettingsUseCase.updateResumeSession(enabled)
        }
    }

    fun updateDefaultFontSize(size: Int) {
        viewModelScope.launch {
            updateSettingsUseCase.updateDefaultFontSize(size)
        }
    }

    fun updateLineSpacing(spacing: Float) {
        viewModelScope.launch {
            updateSettingsUseCase.updateLineSpacing(spacing)
        }
    }

    fun updateColorTheme(theme: AppColorTheme) {
        viewModelScope.launch {
            updateSettingsUseCase.updateColorTheme(theme)
            val themeName = theme.name.lowercase(java.util.Locale.ROOT)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() }
            notificationManager.showNotification(
                com.maralyrics.presentation.common.notification.NotificationData(
                    message = "notif_color_theme_updated|$themeName",
                    type = com.maralyrics.presentation.common.notification.NotificationType.SUCCESS
                )
            )
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            updateSettingsUseCase.resetOnboarding()
        }
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun clearCache() {
        viewModelScope.launch {
            val imageLoader = coil.ImageLoader(context)
            imageLoader.diskCache?.clear()
            imageLoader.memoryCache?.clear()
            notificationManager.showNotification(
                com.maralyrics.presentation.common.notification.NotificationData(
                    message = "notif_cache_cleared",
                    type = com.maralyrics.presentation.common.notification.NotificationType.SUCCESS
                )
            )
        }
    }

    fun backupFavorites() {
        viewModelScope.launch {
            try {
                val favoriteIds = songRepository.getFavoriteSongIds()
                val json = Json.encodeToString(favoriteIds)
                val fileName = "maralyrics_favorites_backup.json"
                context.openFileOutput(fileName, android.content.Context.MODE_PRIVATE).use {
                    it.write(json.toByteArray())
                }
                notificationManager.showNotification(
                    com.maralyrics.presentation.common.notification.NotificationData(
                        message = "notif_fav_backup_success",
                        type = com.maralyrics.presentation.common.notification.NotificationType.SUCCESS
                    )
                )
            } catch (e: Exception) {
                notificationManager.showNotification(
                    com.maralyrics.presentation.common.notification.NotificationData(
                        message = "notif_fav_backup_failed|${e.message}",
                        type = com.maralyrics.presentation.common.notification.NotificationType.ERROR
                    )
                )
            }
        }
    }

    fun restoreFavorites() {
        viewModelScope.launch {
            try {
                val fileName = "maralyrics_favorites_backup.json"
                val file = context.getFileStreamPath(fileName)
                if (file.exists()) {
                    val json = file.readText()
                    val favoriteIds = Json.decodeFromString<List<Long>>(json)
                    songRepository.restoreFavorites(favoriteIds)
                    notificationManager.showNotification(
                        com.maralyrics.presentation.common.notification.NotificationData(
                            message = "notif_fav_restore_success|${favoriteIds.size}",
                            type = com.maralyrics.presentation.common.notification.NotificationType.SUCCESS
                        )
                    )
                } else {
                    notificationManager.showNotification(
                        com.maralyrics.presentation.common.notification.NotificationData(
                            message = "notif_fav_restore_no_file",
                            type = com.maralyrics.presentation.common.notification.NotificationType.ERROR
                        )
                    )
                }
            } catch (e: Exception) {
                notificationManager.showNotification(
                    com.maralyrics.presentation.common.notification.NotificationData(
                        message = "notif_fav_restore_failed|${e.message}",
                        type = com.maralyrics.presentation.common.notification.NotificationType.ERROR
                    )
                )
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            notificationManager.showSyncStarted()
            syncDatabaseUseCase.checkAndSync()
                .onSuccess { notificationManager.showSyncSuccess() }
                .onFailure { notificationManager.showSyncFailed() }
            refreshInfo()
        }
    }

    fun redownloadDatabase() {
        viewModelScope.launch {
            notificationManager.showNotification(
                com.maralyrics.presentation.common.notification.NotificationData(
                    message = "sync_redownloading",
                    type = com.maralyrics.presentation.common.notification.NotificationType.SYNCING,
                    showProgress = true
                )
            )
            syncDatabaseUseCase.fullDownload { }
                .onSuccess { notificationManager.showNotification(
                    com.maralyrics.presentation.common.notification.NotificationData(
                        message = "sync_download_success",
                        type = com.maralyrics.presentation.common.notification.NotificationType.DOWNLOAD_COMPLETE
                    )
                ) }
                .onFailure { notificationManager.showNotification(
                    com.maralyrics.presentation.common.notification.NotificationData(
                        message = "sync_download_failed",
                        type = com.maralyrics.presentation.common.notification.NotificationType.ERROR
                    )
                ) }
            refreshInfo()
        }
    }
}
