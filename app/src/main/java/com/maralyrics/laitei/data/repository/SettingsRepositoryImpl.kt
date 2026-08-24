package com.maralyrics.laitei.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.maralyrics.laitei.domain.model.*
import com.maralyrics.laitei.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

object PreferencesKeys {
    val LANGUAGE = stringPreferencesKey("language")
    val THEME = stringPreferencesKey("theme")
    val DEFAULT_CATEGORY = stringPreferencesKey("default_category")
    val LAST_SYNC = longPreferencesKey("last_sync")
    val SETUP_COMPLETE = booleanPreferencesKey("setup_complete")
    val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    val DATABASE_VERSION = intPreferencesKey("database_version")
    val AUTO_SYNC = booleanPreferencesKey("auto_sync")
    val WIFI_ONLY = booleanPreferencesKey("wifi_only")
    val RESUME_SESSION = booleanPreferencesKey("resume_session")
    val DEFAULT_FONT_SIZE = intPreferencesKey("default_font_size")
    val LINE_SPACING = floatPreferencesKey("line_spacing")
    val COLOR_THEME = stringPreferencesKey("color_theme")
    val LAST_SURPRISE_SONG_ID = longPreferencesKey("last_surprise_song_id")
    val SURPRISE_ME_USES = intPreferencesKey("surprise_me_uses")
    val LAST_SONG_ID = longPreferencesKey("last_song_id")
    val SONG_SCROLL_POSITION = intPreferencesKey("song_scroll_position")
    val HOME_SEARCH_QUERY = stringPreferencesKey("home_search_query")
    val HOME_SORT_ORDER = stringPreferencesKey("home_sort_order")
    val HOME_LAYOUT_TYPE = stringPreferencesKey("home_layout_type")
    val HOME_SCROLL_INDEX = intPreferencesKey("home_scroll_index")
    val HOME_SCROLL_OFFSET = intPreferencesKey("home_scroll_offset")
    val FAVORITE_SEARCH_QUERY = stringPreferencesKey("favorite_search_query")
    val FAVORITE_SORT_ORDER = stringPreferencesKey("favorite_sort_order")
    val FAVORITE_CATEGORY_FILTER = stringPreferencesKey("favorite_category_filter")
    val FAVORITE_LAYOUT_TYPE = stringPreferencesKey("favorite_layout_type")
    val FAVORITE_SCROLL_INDEX = intPreferencesKey("favorite_scroll_index")
    val FAVORITE_SCROLL_OFFSET = intPreferencesKey("favorite_scroll_offset")
    val LAST_ROUTE = stringPreferencesKey("last_route")
    val PRIVACY_POLICY_ACCEPTED = booleanPreferencesKey("privacy_policy_accepted")
    val PRIVACY_POLICY_VERSION = stringPreferencesKey("privacy_policy_version")
    val PRIVACY_POLICY_ACCEPTED_AT = longPreferencesKey("privacy_policy_accepted_at")
    val LAST_SYNC_CURSOR = stringPreferencesKey("last_sync_cursor")
    val LAST_KNOWN_COUNTS = stringPreferencesKey("last_known_counts")
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
                    defaultCategories = preferences[PreferencesKeys.DEFAULT_CATEGORY]?.split(",")?.filter { it.isNotBlank() } ?: listOf("Gospel"),
                    lastSyncTimestamp = preferences[PreferencesKeys.LAST_SYNC] ?: 0L,
                    isSetupComplete = preferences[PreferencesKeys.SETUP_COMPLETE] ?: false,
                    hasCompletedOnboarding = preferences[PreferencesKeys.ONBOARDING_COMPLETE] ?: false,
                    databaseVersion = preferences[PreferencesKeys.DATABASE_VERSION] ?: 0,
                    autoSyncEnabled = preferences[PreferencesKeys.AUTO_SYNC] ?: true,
                    wifiOnlySync = preferences[PreferencesKeys.WIFI_ONLY] ?: true,
                    resumeSessionEnabled = preferences[PreferencesKeys.RESUME_SESSION] ?: true,
                    defaultFontSize = preferences[PreferencesKeys.DEFAULT_FONT_SIZE] ?: 18,
                    lineSpacing = preferences[PreferencesKeys.LINE_SPACING] ?: 1.5f,
                    colorTheme = AppColorTheme.valueOf(
                        preferences[PreferencesKeys.COLOR_THEME] ?: AppColorTheme.MARA.name
                    ),
                    privacyPolicyAccepted = preferences[PreferencesKeys.PRIVACY_POLICY_ACCEPTED] ?: false,
                    privacyPolicyVersion = preferences[PreferencesKeys.PRIVACY_POLICY_VERSION] ?: "",
                    privacyPolicyAcceptedAt = preferences[PreferencesKeys.PRIVACY_POLICY_ACCEPTED_AT] ?: 0L
                )
            }

    override suspend fun updateLanguage(language: AppLanguage) {
        dataStore.edit { it[PreferencesKeys.LANGUAGE] = language.code }
    }

    override suspend fun updateTheme(theme: AppTheme) {
        dataStore.edit { it[PreferencesKeys.THEME] = theme.name }
    }

    override suspend fun updateDefaultCategories(categories: List<String>) {
        dataStore.edit { it[PreferencesKeys.DEFAULT_CATEGORY] = categories.joinToString(",") }
    }

    override suspend fun updateLastSync(timestamp: Long) {
        dataStore.edit { it[PreferencesKeys.LAST_SYNC] = timestamp }
    }

    override suspend fun updateLastSyncCursor(cursor: String) {
        dataStore.edit { it[PreferencesKeys.LAST_SYNC_CURSOR] = cursor }
    }

    override suspend fun getLastSyncCursor(): String {
        return dataStore.data.map { it[PreferencesKeys.LAST_SYNC_CURSOR] ?: "" }.first()
    }

    override suspend fun updateLastKnownCounts(counts: Map<String, Int>) {
        val countsJson = Json.encodeToString(
            MapSerializer(String.serializer(), Int.serializer()),
            counts
        )
        dataStore.edit { it[PreferencesKeys.LAST_KNOWN_COUNTS] = countsJson }
    }

    override suspend fun getLastKnownCounts(): Map<String, Int> {
        val countsJson = dataStore.data.map { it[PreferencesKeys.LAST_KNOWN_COUNTS] ?: "{}" }.first()
        return try {
            Json.decodeFromString(
                MapSerializer(String.serializer(), Int.serializer()),
                countsJson
            )
        } catch (e: Exception) {
            emptyMap()
        }
    }

    override suspend fun markSetupComplete() {
        dataStore.edit { it[PreferencesKeys.SETUP_COMPLETE] = true }
    }

    override suspend fun markOnboardingComplete() {
        dataStore.edit { it[PreferencesKeys.ONBOARDING_COMPLETE] = true }
    }

    override suspend fun resetOnboarding() {
        dataStore.edit { it[PreferencesKeys.ONBOARDING_COMPLETE] = false }
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

    override suspend fun updateResumeSession(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.RESUME_SESSION] = enabled }
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

    override fun getLastSurpriseSongId(): Flow<Long?> =
        dataStore.data.map { it[PreferencesKeys.LAST_SURPRISE_SONG_ID] }

    override suspend fun setLastSurpriseSongId(id: Long) {
        dataStore.edit { it[PreferencesKeys.LAST_SURPRISE_SONG_ID] = id }
    }

    override fun getSurpriseMeUses(): Flow<Int> =
        dataStore.data.map { it[PreferencesKeys.SURPRISE_ME_USES] ?: 0 }

    override suspend fun incrementSurpriseMeUses() {
        dataStore.edit { 
            val current = it[PreferencesKeys.SURPRISE_ME_USES] ?: 0
            it[PreferencesKeys.SURPRISE_ME_USES] = current + 1
        }
    }

    override suspend fun saveLastSongId(songId: Long?) {
        dataStore.edit { 
            if (songId == null) it.remove(PreferencesKeys.LAST_SONG_ID)
            else it[PreferencesKeys.LAST_SONG_ID] = songId
        }
    }

    override fun getLastSongId(): Flow<Long?> =
        dataStore.data.map { it[PreferencesKeys.LAST_SONG_ID] }

    override suspend fun saveSongScrollPosition(position: Int) {
        dataStore.edit { it[PreferencesKeys.SONG_SCROLL_POSITION] = position }
    }

    override fun getSongScrollPosition(): Flow<Int> =
        dataStore.data.map { it[PreferencesKeys.SONG_SCROLL_POSITION] ?: 0 }

    override suspend fun saveHomeSearchQuery(query: String) {
        dataStore.edit { it[PreferencesKeys.HOME_SEARCH_QUERY] = query }
    }

    override fun getHomeSearchQuery(): Flow<String> =
        dataStore.data.map { it[PreferencesKeys.HOME_SEARCH_QUERY] ?: "" }

    override suspend fun saveHomeSortOrder(order: SongSortOrder) {
        dataStore.edit { it[PreferencesKeys.HOME_SORT_ORDER] = order.name }
    }

    override fun getHomeSortOrder(): Flow<SongSortOrder> =
        dataStore.data.map { 
            val name = it[PreferencesKeys.HOME_SORT_ORDER] ?: SongSortOrder.NUMBER_ASC.name
            try { SongSortOrder.valueOf(name) } catch (e: Exception) { SongSortOrder.NUMBER_ASC }
        }

    override suspend fun saveHomeLayoutType(type: SongLayoutType) {
        dataStore.edit { it[PreferencesKeys.HOME_LAYOUT_TYPE] = type.name }
    }

    override fun getHomeLayoutType(): Flow<SongLayoutType> =
        dataStore.data.map { 
            val name = it[PreferencesKeys.HOME_LAYOUT_TYPE] ?: SongLayoutType.NUMBER_TITLE.name
            try { SongLayoutType.valueOf(name) } catch (e: Exception) { SongLayoutType.NUMBER_TITLE }
        }

    override suspend fun saveHomeScrollState(index: Int, offset: Int) {
        dataStore.edit { 
            it[PreferencesKeys.HOME_SCROLL_INDEX] = index
            it[PreferencesKeys.HOME_SCROLL_OFFSET] = offset
        }
    }

    override fun getHomeScrollState(): Flow<Pair<Int, Int>> =
        dataStore.data.map { 
            val index = it[PreferencesKeys.HOME_SCROLL_INDEX] ?: 0
            val offset = it[PreferencesKeys.HOME_SCROLL_OFFSET] ?: 0
            index to offset
        }

    override suspend fun saveFavoriteSearchQuery(query: String) {
        dataStore.edit { it[PreferencesKeys.FAVORITE_SEARCH_QUERY] = query }
    }

    override fun getFavoriteSearchQuery(): Flow<String> =
        dataStore.data.map { it[PreferencesKeys.FAVORITE_SEARCH_QUERY] ?: "" }

    override suspend fun saveFavoriteSortOrder(order: SongSortOrder) {
        dataStore.edit { it[PreferencesKeys.FAVORITE_SORT_ORDER] = order.name }
    }

    override fun getFavoriteSortOrder(): Flow<SongSortOrder> =
        dataStore.data.map { 
            val name = it[PreferencesKeys.FAVORITE_SORT_ORDER] ?: SongSortOrder.RECENTLY_ADDED.name
            try { SongSortOrder.valueOf(name) } catch (e: Exception) { SongSortOrder.RECENTLY_ADDED }
        }

    override suspend fun saveFavoriteCategoryFilter(category: String?) {
        dataStore.edit { 
            if (category == null) it.remove(PreferencesKeys.FAVORITE_CATEGORY_FILTER)
            else it[PreferencesKeys.FAVORITE_CATEGORY_FILTER] = category 
        }
    }

    override fun getFavoriteCategoryFilter(): Flow<String?> =
        dataStore.data.map { it[PreferencesKeys.FAVORITE_CATEGORY_FILTER] }

    override suspend fun saveFavoriteLayoutType(type: SongLayoutType) {
        dataStore.edit { it[PreferencesKeys.FAVORITE_LAYOUT_TYPE] = type.name }
    }

    override fun getFavoriteLayoutType(): Flow<SongLayoutType> =
        dataStore.data.map { 
            val name = it[PreferencesKeys.FAVORITE_LAYOUT_TYPE] ?: SongLayoutType.NUMBER_TITLE.name
            try { SongLayoutType.valueOf(name) } catch (e: Exception) { SongLayoutType.NUMBER_TITLE }
        }

    override suspend fun saveFavoriteScrollState(index: Int, offset: Int) {
        dataStore.edit { 
            it[PreferencesKeys.FAVORITE_SCROLL_INDEX] = index
            it[PreferencesKeys.FAVORITE_SCROLL_OFFSET] = offset
        }
    }

    override fun getFavoriteScrollState(): Flow<Pair<Int, Int>> =
        dataStore.data.map { 
            val index = it[PreferencesKeys.FAVORITE_SCROLL_INDEX] ?: 0
            val offset = it[PreferencesKeys.FAVORITE_SCROLL_OFFSET] ?: 0
            index to offset
        }

    override suspend fun saveLastRoute(route: String?) {
        dataStore.edit { 
            if (route == null) it.remove(PreferencesKeys.LAST_ROUTE)
            else it[PreferencesKeys.LAST_ROUTE] = route 
        }
    }

    override fun getLastRoute(): Flow<String?> =
        dataStore.data.map { it[PreferencesKeys.LAST_ROUTE] }

    override suspend fun updatePrivacyPolicyAcceptance(accepted: Boolean, version: String, timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIVACY_POLICY_ACCEPTED] = accepted
            preferences[PreferencesKeys.PRIVACY_POLICY_VERSION] = version
            preferences[PreferencesKeys.PRIVACY_POLICY_ACCEPTED_AT] = timestamp
        }
    }
}

// Exposed for SyncRepository
class SettingsDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    suspend fun getDatabaseVersion(): Int =
        dataStore.data.first()[PreferencesKeys.DATABASE_VERSION] ?: 0
}