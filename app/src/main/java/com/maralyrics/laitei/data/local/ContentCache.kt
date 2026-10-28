package com.maralyrics.laitei.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Small on-disk JSON cache for the "extras" fetched from the website API (articles, Mara Idol,
 * calendar, song of the day). These payloads are cheap to refetch, so they live in `cacheDir`
 * (excluded from backups) instead of Room — no schema change, and the OS may reclaim the space.
 * Each entry is written to a temp file and renamed so a crash mid-write never leaves a
 * half-written file that would fail to decode on the next read.
 */
@Singleton
class ContentCache @Inject constructor(
    @ApplicationContext private val context: Context,
    @PublishedApi internal val json: Json
) {
    private val dir: File by lazy { File(context.cacheDir, "content").apply { mkdirs() } }

    suspend fun read(key: String): String? = withContext(Dispatchers.IO) {
        runCatching { fileFor(key).takeIf { it.isFile }?.readText() }.getOrNull()
    }

    suspend fun write(key: String, text: String) {
        withContext(Dispatchers.IO) {
            runCatching {
                val target = fileFor(key)
                val temp = File(dir, target.name + ".tmp")
                temp.writeText(text)
                if (!temp.renameTo(target)) {
                    target.writeText(text)
                    temp.delete()
                }
            }
        }
    }

    /** How long ago [key] was last written, or null if it never was. */
    suspend fun ageMillis(key: String): Long? = withContext(Dispatchers.IO) {
        runCatching {
            fileFor(key).takeIf { it.isFile }?.let { System.currentTimeMillis() - it.lastModified() }
        }.getOrNull()
    }

    suspend inline fun <reified T> readJson(key: String): T? =
        read(key)?.let { text -> runCatching { json.decodeFromString<T>(text) }.getOrNull() }

    suspend inline fun <reified T> writeJson(key: String, value: T) =
        write(key, json.encodeToString(value))

    private fun fileFor(key: String) = File(dir, key.replace(UNSAFE_FILENAME_CHARS, "_") + ".json")

    private companion object {
        val UNSAFE_FILENAME_CHARS = Regex("[^A-Za-z0-9._-]")
    }
}
