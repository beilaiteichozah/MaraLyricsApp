package com.maralyrics.laitei.data.sync

import android.content.Context
import com.maralyrics.laitei.data.local.dao.FavoriteDao
import com.maralyrics.laitei.data.local.dao.SongDao
import com.maralyrics.laitei.data.local.entity.FavoriteEntity
import com.maralyrics.laitei.domain.model.AppLanguage
import com.maralyrics.laitei.domain.model.AppTheme
import com.maralyrics.laitei.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** The sync's memory, kept out of Android's cloud backup (it belongs to an account signed in on this device). */
@Singleton
class FileSyncStateStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) : SyncStateStore {

    private val file: File get() = File(context.noBackupFilesDir, FILE_NAME)

    override suspend fun read(): SyncState? = withContext(Dispatchers.IO) {
        runCatching { file.takeIf { it.isFile }?.readText()?.let { json.decodeFromString<SyncState>(it) } }.getOrNull()
    }

    override suspend fun write(state: SyncState) {
        withContext(Dispatchers.IO) {
            runCatching {
                val text = json.encodeToString(state)
                val temp = File(file.parentFile, "$FILE_NAME.tmp")
                temp.writeText(text)
                if (!temp.renameTo(file)) {
                    file.writeText(text)
                    temp.delete()
                }
            }
        }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) { runCatching { file.delete() } }
    }

    private companion object {
        const val FILE_NAME = "account_sync_state.json"
    }
}

/** The downloaded library's favorites, found by slug (the website knows songs by slug, the app by id). */
@Singleton
class RoomFavoriteStore @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val songDao: SongDao
) : FavoriteStore {

    override suspend fun favorites(): List<LocalFavorite> =
        favoriteDao.getFavoriteSlugs().map { LocalFavorite(it.slug, it.favoritedAt) }

    override suspend fun library(): Set<String> = songDao.getAllSlugs().toSet()

    override suspend fun add(slug: String, at: Long) {
        val songId = songDao.getIdBySlug(slug) ?: return
        favoriteDao.addFavorite(FavoriteEntity(songId = songId, favoritedAt = at))
    }

    override suspend fun remove(slug: String) {
        val songId = songDao.getIdBySlug(slug) ?: return
        favoriteDao.removeFavorite(songId)
    }
}

/** Theme and language, written straight to the settings (not through the use case that records changes for the sync). */
@Singleton
class SettingsPreferenceStore @Inject constructor(
    private val settings: SettingsRepository
) : PreferenceStore {
    override suspend fun theme(): AppTheme = settings.getSettings().first().theme
    override suspend fun language(): AppLanguage = settings.getSettings().first().language
    override suspend fun setTheme(theme: AppTheme) = settings.updateTheme(theme)
    override suspend fun setLanguage(language: AppLanguage) = settings.updateLanguage(language)
}
