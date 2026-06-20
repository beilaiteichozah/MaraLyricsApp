package com.maralyrics.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.maralyrics.domain.model.*
import com.maralyrics.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

object PreferencesKeys {
    val LANGUAGE = stringPreferencesKey("language")
    val THEME = stringPreferencesKey("theme")
    val DEFAULT_CATEGORY = stringPreferencesKey("default_category")
    val LAST_SYNC = longPreferencesKey("last_sync")
    val SETUP_COMPLETE = booleanPreferencesKey("setup_complete")
    val DATABASE_VERSION = intPreferencesKey("database_version")
    val AUTO_SYNC = booleanPreferencesKey("auto_sync")
    val WIFI_ONLY = booleanPreferencesKey("wifi_only")
    val DEFAULT_FONT_SIZE = intPreferencesKey("default_font_size")
    val LINE_SPACING = floatPreferencesKey("line_spacing")
    val COLOR_THEME = stringPreferencesKey("color_theme")
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override fun getSettings(): Flow<AppSettings> =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences())
                else throw exception
            }
            .map { preferences ->
                AppSettings(
                    language = AppLanguage.fromCode(
                        preferences[PreferencesKeys.LANGUAGE] ?: AppLanguage.ENGLISH.code
                    ),
                    theme = AppTheme.valueOf(
                        preferences[PreferencesKeys.THEME] ?: AppTheme.SYSTEM.name
                    ),
                    defaultCategory = preferences[PreferencesKeys.DEFAULT_CATEGORY] ?: "Gospel",
                    lastSyncTimestamp = preferences[PreferencesKeys.LAST_SYNC] ?: 0L,
                    isSetupComplete = preferences[PreferencesKeys.SETUP_COMPLETE] ?: false,
                    databaseVersion = preferences[PreferencesKeys.DATABASE_VERSION] ?: 0,
                    autoSyncEnabled = preferences[PreferencesKeys.AUTO_SYNC] ?: true,
                    wifiOnlySync = preferences[PreferencesKeys.WIFI_ONLY] ?: true,
                    defaultFontSize = preferences[PreferencesKeys.DEFAULT_FONT_SIZE] ?: 18,
                    lineSpacing = preferences[PreferencesKeys.LINE_SPACING] ?: 1.5f,
                    colorTheme = AppColorTheme.valueOf(
                        preferences[PreferencesKeys.COLOR_THEME] ?: AppColorTheme.MARA.name
                    )
                )
            }

    override suspend fun updateLanguage(language: AppLanguage) {
        dataStore.edit { it[PreferencesKeys.LANGUAGE] = language.code }
    }

    override suspend fun updateTheme(theme: AppTheme) {
        dataStore.edit { it[PreferencesKeys.THEME] = theme.name }
    }

    override suspend fun updateDefaultCategory(category: String) {
        dataStore.edit { it[PreferencesKeys.DEFAULT_CATEGORY] = category }
    }

    override suspend fun updateLastSync(timestamp: Long) {
        dataStore.edit { it[PreferencesKeys.LAST_SYNC] = timestamp }
    }

    override suspend fun markSetupComplete() {
        dataStore.edit { it[PreferencesKeys.SETUP_COMPLETE] = true }
    }

    override suspend fun updateDatabaseVersion(version: Int) {
        dataStore.edit { it[PreferencesKeys.DATABASE_VERSION] = version }
    }

    override suspend fun getLocalDatabaseVersion(): Int =
        dataStore.data.first()[PreferencesKeys.DATABASE_VERSION] ?: 0

    override suspend fun updateAutoSync(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.AUTO_SYNC] = enabled }
    }

    override suspend fun updateWifiOnly(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.WIFI_ONLY] = enabled }
    }

    override suspend fun updateDefaultFontSize(size: Int) {
        dataStore.edit { it[PreferencesKeys.DEFAULT_FONT_SIZE] = size }
    }

    override suspend fun updateLineSpacing(spacing: Float) {
        dataStore.edit { it[PreferencesKeys.LINE_SPACING] = spacing }
    }

    override suspend fun updateColorTheme(theme: AppColorTheme) {
        dataStore.edit { it[PreferencesKeys.COLOR_THEME] = theme.name }
    }
}

// Exposed for SyncRepository
class SettingsDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    suspend fun getDatabaseVersion(): Int =
        dataStore.data.first()[PreferencesKeys.DATABASE_VERSION] ?: 0
}