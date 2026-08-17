package com.borgpharmacy.pro.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.borgpharmacy.pro.BorgAppApplication

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as BorgAppApplication).container
        val session = container.secureSessionStore.read() ?: return Result.success()
        return when (container.syncManager.sync(session.tenantId)) {
            is SyncResult.Succeeded -> Result.success()
            is SyncResult.AuthRequired -> Result.success()
            is SyncResult.RetryScheduled -> if (runAttemptCount < 5) Result.retry() else Result.failure()
        }
    }
}
