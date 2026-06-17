package com.maralyrics.domain.model

data class Song(
    val id: Long,
    val title: String,
    val lyrics: String,
    val category: SongCategory,
    val songNumber: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val isFavorite: Boolean = false
)

enum class SongCategory(val key: String) {
    GOSPEL("gospel"),
    LOVE("love"),
    PATRIOTIC("patriotic"),
    TRADITIONAL("traditional");

    companion object {
        fun fromKey(key: String): SongCategory =
            entries.firstOrNull { it.key == key } ?: GOSPEL
    }
}

data class AppSettings(
    val language: AppLanguage,
    val theme: AppTheme,
    val defaultCategory: SongCategory,
    val lastSyncTimestamp: Long,
    val isSetupComplete: Boolean,
    val databaseVersion: Int
)

enum class AppLanguage(val code: String) {
    ENGLISH("en"),
    MARA("mrh");

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

data class SyncStatus(
    val isAvailable: Boolean,
    val localVersion: Int,
    val serverVersion: Int
)

data class DownloadProgress(
    val totalSongs: Int,
    val downloadedSongs: Int,
    val percentage: Float,
    val estimatedSecondsRemaining: Long
)