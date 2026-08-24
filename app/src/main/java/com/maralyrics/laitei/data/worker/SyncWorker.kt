package com.maralyrics.laitei.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.maralyrics.laitei.domain.usecase.SyncDatabaseUseCase
import com.maralyrics.laitei.domain.usecase.InsufficientStorageException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncDatabaseUseCase: SyncDatabaseUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val result = syncDatabaseUseCase.checkAndSync(isAutomatic = true)
            if (result.isSuccess) {
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
