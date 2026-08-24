package com.maralyrics.laitei.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.laitei.domain.model.*
import com.maralyrics.laitei.domain.repository.CreditsRepository
import com.maralyrics.laitei.domain.repository.SongRepository
import com.maralyrics.laitei.domain.repository.SyncRepository
import com.maralyrics.laitei.domain.repository.ArtistRepository
import com.maralyrics.laitei.domain.repository.ComposerRepository
import com.maralyrics.laitei.domain.usecase.*
import com.maralyrics.laitei.utils.StorageUtils
import com.maralyrics.laitei.presentation.common.notification.NotificationManager
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
    private val syncRepository: SyncRepository,
    private val songRepository: SongRepository,
    private val artistRepository: ArtistRepository,
    private val composerRepository: ComposerRepository,
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

    private val _artistCount = MutableStateFlow(0)
    val artistCount: StateFlow<Int> = _artistCount.asStateFlow()

    private val _composerCount = MutableStateFlow(0)
    val composerCount: StateFlow<Int> = _composerCount.asStateFlow()

    private val _copyrightOwnerCount = MutableStateFlow(0)
    val copyrightOwnerCount: StateFlow<Int> = _copyrightOwnerCount.asStateFlow()

    private val _dbSize = MutableStateFlow(0L)
    val dbSize: StateFlow<Long> = _dbSize.asStateFlow()

    private val _showNoUpdatesDialog = MutableStateFlow(false)
    val showNoUpdatesDialog = _showNoUpdatesDialog.asStateFlow()

    init {
        refreshInfo()
    }

    private fun refreshInfo() {
        viewModelScope.launch {
            _songCount.value = songRepository.getLocalSongCount()
            _artistCount.value = artistRepository.getLocalArtistCount()
            _composerCount.value = composerRepository.getLocalComposerCount()
            _copyrightOwnerCount.value = syncRepository.getLocalCopyrightOwnerCount()
            _dbSize.value = songRepository.getDatabaseSizeBytes()
        }
    }

    fun updateLanguage(language: AppLanguage) {
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
            
            val message = if (currentCategories.contains("All")) {
                "cat_all"
            } else {
                currentCategories.joinToString(", ") { cat ->
                    when (cat) {
                        "Gospel" -> "cat_gospel"
                        "Love" -> "cat_love"
                        "Patriotic" -> "cat_patriotic"
                        "Traditional" -> "cat_traditional"
                        "Uncategorized" -> "cat_uncategorized"
                        else -> cat
                    }
                }
            }
            notificationManager.showNotification(
                com.maralyrics.laitei.presentation.common.notification.NotificationData(
                    message = "notif_default_cat_updated|$message",
                    type = com.maralyrics.laitei.presentation.common.notification.NotificationType.SUCCESS
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
            val themeKey = when(theme) {
                AppColorTheme.MARA -> "color_mara"
                AppColorTheme.OCEAN -> "color_ocean"
                AppColorTheme.EMERALD -> "color_emerald"
                AppColorTheme.SUNSET -> "color_sunset"
                AppColorTheme.PURPLE -> "color_purple"
                AppColorTheme.ROSE -> "color_rose"
                AppColorTheme.SLATE -> "color_slate"
            }
            notificationManager.showNotification(
                com.maralyrics.laitei.presentation.common.notification.NotificationData(
                    message = "notif_color_theme_updated|$themeKey",
                    type = com.maralyrics.laitei.presentation.common.notification.NotificationType.SUCCESS
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
                com.maralyrics.laitei.presentation.common.notification.NotificationData(
                    message = "notif_cache_cleared",
                    type = com.maralyrics.laitei.presentation.common.notification.NotificationType.SUCCESS
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
                    com.maralyrics.laitei.presentation.common.notification.NotificationData(
                        message = "notif_fav_backup_success",
                        type = com.maralyrics.laitei.presentation.common.notification.NotificationType.SUCCESS
                    )
                )
            } catch (e: Exception) {
                notificationManager.showNotification(
                    com.maralyrics.laitei.presentation.common.notification.NotificationData(
                        message = "notif_fav_backup_failed|${e.message}",
                        type = com.maralyrics.laitei.presentation.common.notification.NotificationType.ERROR
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
                        com.maralyrics.laitei.presentation.common.notification.NotificationData(
                            message = "notif_fav_restore_success|${favoriteIds.size}",
                            type = com.maralyrics.laitei.presentation.common.notification.NotificationType.SUCCESS
                        )
                    )
                } else {
                    notificationManager.showNotification(
                        com.maralyrics.laitei.presentation.common.notification.NotificationData(
                            message = "notif_fav_restore_no_file",
                            type = com.maralyrics.laitei.presentation.common.notification.NotificationType.ERROR
                        )
                    )
                }
            } catch (e: Exception) {
                notificationManager.showNotification(
                    com.maralyrics.laitei.presentation.common.notification.NotificationData(
                        message = "notif_fav_restore_failed|${e.message}",
                        type = com.maralyrics.laitei.presentation.common.notification.NotificationType.ERROR
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
                .onFailure { exception ->
                    if (exception is InsufficientStorageException) {
                        notificationManager.showStorageWarning(StorageUtils.getUsedStoragePercentage())
                    } else {
                        notificationManager.showSyncFailed()
                    }
                }
            refreshInfo()
        }
    }

    fun checkNewData() {
        viewModelScope.launch {
            notificationManager.showSyncStarted()
            syncDatabaseUseCase.checkForUpdates().onSuccess { status ->
                if (!status.isAvailable) {
                    _showNoUpdatesDialog.value = true
                }
                // If isAvailable is true, MainViewModel's observer will show the update dialog
            }.onFailure {
                notificationManager.showSyncFailed()
            }
        }
    }

    fun dismissNoUpdatesDialog() {
        _showNoUpdatesDialog.value = false
    }

    fun redownloadDatabase() {
        viewModelScope.launch {
            notificationManager.showNotification(
                com.maralyrics.laitei.presentation.common.notification.NotificationData(
                    message = "sync_redownloading",
                    type = com.maralyrics.laitei.presentation.common.notification.NotificationType.SYNCING,
                    showProgress = true
                )
            )
            syncDatabaseUseCase.fullDownload { }
                .onSuccess { notificationManager.showNotification(
                    com.maralyrics.laitei.presentation.common.notification.NotificationData(
                        message = "sync_download_success",
                        type = com.maralyrics.laitei.presentation.common.notification.NotificationType.DOWNLOAD_COMPLETE
                    )
                ) }
                .onFailure { notificationManager.showNotification(
                    com.maralyrics.laitei.presentation.common.notification.NotificationData(
                        message = "sync_download_failed",
                        type = com.maralyrics.laitei.presentation.common.notification.NotificationType.ERROR
                    )
                ) }
            refreshInfo()
        }
    }
}
