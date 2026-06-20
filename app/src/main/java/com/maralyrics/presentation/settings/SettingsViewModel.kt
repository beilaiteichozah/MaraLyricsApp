package com.maralyrics.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.domain.model.*
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
    private val notificationManager: NotificationManager,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = getSettingsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val availableCategories: StateFlow<List<SongCategory>> = getAvailableCategoriesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            notificationManager.showLanguageChanged(if (language == AppLanguage.MARA) "Mara" else "English")
        }
    }

    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch {
            updateSettingsUseCase.updateTheme(theme)
            notificationManager.showThemeChanged(when(theme) {
                AppTheme.LIGHT -> "Light mode"
                AppTheme.DARK -> "Dark mode"
                AppTheme.SYSTEM -> "System theme"
            })
        }
    }

    fun updateCategory(category: String) {
        viewModelScope.launch {
            updateSettingsUseCase.updateCategory(category)
            notificationManager.showSuccess("Default category updated to $category")
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
            notificationManager.showSuccess("Color theme updated")
        }
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun clearCache() {
        viewModelScope.launch {
            val imageLoader = coil.ImageLoader(context)
            imageLoader.diskCache?.clear()
            imageLoader.memoryCache?.clear()
            notificationManager.showSuccess("Cache cleared successfully")
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
                notificationManager.showSuccess("Favorites backed up locally")
            } catch (e: Exception) {
                notificationManager.showError("Backup failed: ${e.message}")
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
                    notificationManager.showSuccess("${favoriteIds.size} favorites restored")
                } else {
                    notificationManager.showError("No backup file found")
                }
            } catch (e: Exception) {
                notificationManager.showError("Restore failed: ${e.message}")
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
                    message = "Redownloading database...",
                    type = com.maralyrics.presentation.common.notification.NotificationType.SYNCING,
                    showProgress = true
                )
            )
            syncDatabaseUseCase.fullDownload { }
                .onSuccess { notificationManager.showNotification(
                    com.maralyrics.presentation.common.notification.NotificationData(
                        message = "Songs downloaded successfully.",
                        type = com.maralyrics.presentation.common.notification.NotificationType.DOWNLOAD_COMPLETE
                    )
                ) }
                .onFailure { notificationManager.showNotification(
                    com.maralyrics.presentation.common.notification.NotificationData(
                        message = "Failed to download songs.",
                        type = com.maralyrics.presentation.common.notification.NotificationType.ERROR
                    )
                ) }
            refreshInfo()
        }
    }
}
