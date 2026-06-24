package com.maralyrics.domain.model

import kotlinx.serialization.Serializable

data class Song(
    val id: Long,
    val slug: String,
    val title: String,
    val lyrics: String,
    val category: String,
    val createdAt: Long,
    val artistId: Long? = null,
    val artistName: String? = null,
    val artistSlug: String? = null,
    val composerId: Long? = null,
    val composerName: String? = null,
    val composerSlug: String? = null,
    val copyrightOwnerId: Long? = null,
    val copyrightOwnerName: String? = null,
    val isFavorite: Boolean = false,
    val views: Int = 0
)

enum class SuggestionType {
    SONG, ARTIST, COMPOSER
}

data class SearchSuggestion(
    val id: Long,
    val text: String,
    val type: SuggestionType,
    val score: Double,
    val slug: String = ""
)

sealed class SearchResponse {
    data class Results(val songs: List<Song>) : SearchResponse()
    data class Suggestions(val grouped: Map<SuggestionType, List<SearchSuggestion>>) : SearchResponse()
    object Empty : SearchResponse()
}

data class SongCategory(
    val key: String,
    val displayName: String,
    val songCount: Int = 0
)

data class Artist(
    val id: Long,
    val slug: String,
    val name: String,
    val bio: String?,
    val imageUrl: String?,
    val socialLinks: List<SocialLink>,
    val songCount: Int = 0
)

data class Composer(
    val id: Long,
    val slug: String,
    val name: String,
    val bio: String?,
    val imageUrl: String?,
    val socialLinks: List<SocialLink>,
    val songCount: Int = 0
)

data class SocialLink(
    val platform: String,
    val url: String
)

data class AppSettings(
    val language: AppLanguage,
    val theme: AppTheme,
    val colorTheme: AppColorTheme = AppColorTheme.MARA,
    val defaultCategories: List<String>,
    val lastSyncTimestamp: Long,
    val isSetupComplete: Boolean,
    val hasCompletedOnboarding: Boolean,
    val databaseVersion: Int,
    val autoSyncEnabled: Boolean = true,
    val wifiOnlySync: Boolean = true,
    val resumeSessionEnabled: Boolean = true,
    val defaultFontSize: Int = 18,
    val lineSpacing: Float = 1.5f
)

enum class AppLanguage(val code: String) {
    ENGLISH("en"),
    MARA("mrh"),
    BURMESE("my");

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

enum class AppColorTheme {
    MARA, OCEAN, EMERALD, SUNSET, PURPLE, ROSE, SLATE
}

data class SyncStatus(
    val isAvailable: Boolean,
    val localVersion: Int,
    val serverVersion: Int
)

data class DownloadProgress(
    val totalItems: Int,
    val downloadedItems: Int,
    val percentage: Float,
    val currentEntity: String // "Songs", "Artists", "Composers"
)

data class Feedback(
    val id: Long = 0,
    val songId: Long,
    val songSlug: String = "",
    val songTitle: String = "",
    val artistName: String? = null,
    val name: String,
    val email: String?,
    val message: String,
    val isSynced: Boolean = false
)

enum class SongSortOrder {
    NUMBER_ASC, NUMBER_DESC, A_Z, Z_A, NEWEST, OLDEST, RECENTLY_ADDED
}

enum class SongLayoutType {
    NUMBER_TITLE, TITLE_BADGE
}

enum class ProfileType {
    ARTIST, COMPOSER
}

enum class SearchInitializationState {
    IDLE, LOADING_DATA, BUILDING_INDEX, READY
}

data class Profile(
    val id: Long,
    val slug: String,
    val name: String,
    val bio: String?,
    val imageUrl: String?,
    val socialLinks: List<SocialLink>,
    val songCount: Int,
    val type: ProfileType
)

data class FavoriteStats(
    val songCount: Int,
    val artistCount: Int,
    val composerCount: Int
)

@Serializable
data class Contributor(
    val name: String,
    val url: String
)

@Serializable
data class CreditsData(
    val songContributors: List<Contributor> = emptyList(),
    val translators: List<Contributor> = emptyList(),
    val supporters: List<Contributor> = emptyList(),
    val specialThanks: List<Contributor> = emptyList()
)
