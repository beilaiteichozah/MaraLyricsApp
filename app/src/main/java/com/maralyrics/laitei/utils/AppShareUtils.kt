package com.maralyrics.laitei.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.core.content.FileProvider
import com.maralyrics.laitei.R
import com.maralyrics.laitei.data.local.MaraLyricsDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class ImportDatabaseResult {
    object Success : ImportDatabaseResult()
    object IncompatibleVersion : ImportDatabaseResult()
    data class Failed(val message: String) : ImportDatabaseResult()
}

/**
 * Exports the running app's own APK and downloaded song database so they can be shared
 * with a nearby device with no internet access, and imports such a database back in.
 */
@Singleton
class AppShareUtils @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MaraLyricsDatabase
) {
    private val sharedDir: File
        get() = File(context.cacheDir, "shared").apply { mkdirs() }

    suspend fun exportApk(): File = withContext(Dispatchers.IO) {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val applicationInfo = packageInfo.applicationInfo!!
        // A Play-delivered install can consist of this base APK plus separate split APKs
        // (density/ABI/etc.). Sharing the base alone produces a file that fails to install
        // on another device since it's missing content the split APKs would have provided.
        if (!applicationInfo.splitSourceDirs.isNullOrEmpty()) {
            throw IllegalStateException(context.getString(R.string.share_split_apk_error))
        }
        val sourceApk = File(applicationInfo.sourceDir)
        val destApk = File(sharedDir, "MaraLyrics-${packageInfo.versionName}.apk")
        sourceApk.copyTo(destApk, overwrite = true)
        destApk
    }

    suspend fun exportDatabase(): File = withContext(Dispatchers.IO) {
        // Flush the WAL into the main file so a single file is a complete, consistent copy.
        // Cursor is lazy — must read from it (moveToFirst) or the PRAGMA never actually runs.
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
        val dbFile = context.getDatabasePath(MaraLyricsDatabase.DATABASE_NAME)
        val destDb = File(sharedDir, MaraLyricsDatabase.DATABASE_NAME)
        dbFile.copyTo(destDb, overwrite = true)
        destDb
    }

    fun uriFor(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    suspend fun importDatabase(uri: Uri): ImportDatabaseResult = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "import_temp.db")
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: return@withContext ImportDatabaseResult.Failed("Could not open file")
            input.use { stream ->
                tempFile.outputStream().use { output -> stream.copyTo(output) }
            }

            val importedVersion = try {
                SQLiteDatabase.openDatabase(
                    tempFile.path, null, SQLiteDatabase.OPEN_READONLY
                ).use { it.version }
            } catch (e: Exception) {
                tempFile.delete()
                return@withContext ImportDatabaseResult.Failed("Not a valid database file")
            }

            if (importedVersion != MaraLyricsDatabase.SCHEMA_VERSION) {
                tempFile.delete()
                return@withContext ImportDatabaseResult.IncompatibleVersion
            }

            database.close()
            val dbFile = context.getDatabasePath(MaraLyricsDatabase.DATABASE_NAME)
            listOf(dbFile, File("${dbFile.path}-wal"), File("${dbFile.path}-shm")).forEach {
                if (it.exists()) it.delete()
            }
            tempFile.copyTo(dbFile, overwrite = true)
            tempFile.delete()
            ImportDatabaseResult.Success
        } catch (e: Exception) {
            tempFile.delete()
            ImportDatabaseResult.Failed(e.message ?: "Import failed")
        }
    }

    fun restartApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
        kotlin.system.exitProcess(0)
    }
}
