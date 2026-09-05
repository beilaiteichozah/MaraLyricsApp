package com.maralyrics.laitei.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.maralyrics.laitei.domain.usecase.SyncDatabaseUseCase
import com.maralyrics.laitei.domain.usecase.InsufficientStorageException
import com.maralyrics.laitei.presentation.common.notification.SongUpdateNotifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncDatabaseUseCase: SyncDatabaseUseCase,
    private val songUpdateNotifier: SongUpdateNotifier
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val result = syncDatabaseUseCase.checkAndSync(isAutomatic = true)
            if (result.isSuccess) {
                // This is the only chance to tell the user about new songs when the
                // app isn't open — the foreground check shows the same notification.
                result.getOrNull()?.let { syncResult ->
                    if (syncResult.newSongs > 0) {
                        songUpdateNotifier.notifyNewSongs(syncResult.newSongs)
                    }
                }
                Result.success()
            } else {
                val exception = result.exceptionOrNull()
                if (exception is InsufficientStorageException) {
                    Result.failure()
                } else {
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
