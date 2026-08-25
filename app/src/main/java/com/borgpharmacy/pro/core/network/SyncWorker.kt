package com.borgpharmacy.pro.core.network

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching {
        val tenant = inputData.getString(KEY_TENANT) ?: return Result.failure()
        val app = applicationContext as com.borgpharmacy.pro.BorgAppApplication
        app.container.authRepository.restoreSession()
        app.container.syncEngine.syncTenant(tenant)
    }.fold(onSuccess={Result.success()}, onFailure={if (runAttemptCount >= 8) Result.failure() else Result.retry()})
    companion object { const val KEY_TENANT="tenant_id" }
}
