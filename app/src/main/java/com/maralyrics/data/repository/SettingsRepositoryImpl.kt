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
                    defaultCategory = SongCategory.fromKey(
                        preferences[PreferencesKeys.DEFAULT_CATEGORY] ?: SongCategory.GOSPEL.key
                    ),
                    lastSyncTimestamp = preferences[PreferencesKeys.LAST_SYNC] ?: 0L,
                    isSetupComplete = preferences[PreferencesKeys.SETUP_COMPLETE] ?: false,
                    databaseVersion = preferences[PreferencesKeys.DATABASE_VERSION] ?: 0
                )
            }

    override suspend fun updateLanguage(language: AppLanguage) {
        dataStore.edit { it[PreferencesKeys.LANGUAGE] = language.code }
    }

    override suspend fun updateTheme(theme: AppTheme) {
        dataStore.edit { it[PreferencesKeys.THEME] = theme.name }
    }

    override suspend fun updateDefaultCategory(category: SongCategory) {
        dataStore.edit { it[PreferencesKeys.DEFAULT_CATEGORY] = category.key }
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
}

// Exposed for SyncRepository
class SettingsDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    suspend fun getDatabaseVersion(): Int =
        dataStore.data.first()[PreferencesKeys.DATABASE_VERSION] ?: 0
}