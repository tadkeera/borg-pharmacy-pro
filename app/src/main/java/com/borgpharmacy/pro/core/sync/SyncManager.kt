package com.borgpharmacy.pro.core.sync

import com.borgpharmacy.pro.core.security.SessionStore
import com.borgpharmacy.pro.data.remote.ProSyncOperation
import com.borgpharmacy.pro.data.remote.SyncRemoteDataSource
import com.borgpharmacy.pro.core.database.dao.SyncQueueDao
import com.borgpharmacy.pro.core.database.entity.SyncQueueEntity
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class SyncManager(
    private val queueDao: SyncQueueDao,
    private val sessionStore: SessionStore,
    private val syncService: SyncRemoteDataSource,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun sync(tenantId: String, nowMillis: Long = System.currentTimeMillis()): SyncResult {
        require(tenantId.isNotBlank()) { "tenantId is required for sync" }
        val storedSession = sessionStore.read() ?: return SyncResult.AuthRequired
        if (storedSession.tenantId != tenantId) return SyncResult.AuthRequired
        val session = if (storedSession.isExpired(nowMillis / 1000)) {
            runCatching {
                val refreshed = syncService.refreshSession(storedSession.refreshToken)
                storedSession.copy(
                    accessToken = refreshed.accessToken,
                    refreshToken = refreshed.refreshToken,
                    expiresAtEpochSeconds = refreshed.expiresAtEpochSeconds,
                ).also(sessionStore::save)
            }.getOrNull() ?: return SyncResult.AuthRequired
        } else {
            storedSession
        }

        val pending = queueDao.pending(tenantId, nowMillis, BATCH_SIZE)
        if (pending.isEmpty()) return SyncResult.Succeeded(0)

        return try {
            val operations = pending.map { it.toRemote(json) }
            val response = syncService.pushSecureOperations(session.accessToken, operations)
            val accepted = response.accepted.associateBy { it.idempotencyKey }
            val conflicts = response.conflicts.associateBy { it.idempotencyKey }
            val rejected = response.rejected.associateBy { it.idempotencyKey }
            pending.forEach { entry ->
                when {
                    entry.idempotencyKey in accepted -> queueDao.delete(tenantId, entry.id)
                    entry.idempotencyKey in conflicts -> markTerminal(entry, tenantId, "CONFLICT", conflicts.getValue(entry.idempotencyKey).reason)
                    entry.idempotencyKey in rejected -> markTerminal(entry, tenantId, "FAILED", rejected.getValue(entry.idempotencyKey).reason)
                    else -> scheduleRetry(entry, tenantId, nowMillis, "operation was not returned by server")
                }
            }
            SyncResult.Succeeded(accepted.size)
        } catch (error: Throwable) {
            pending.forEach { scheduleRetry(it, tenantId, nowMillis, error.message ?: error.javaClass.simpleName) }
            SyncResult.RetryScheduled(pending.size)
        }
    }

    private suspend fun markTerminal(
        entry: SyncQueueEntity,
        tenantId: String,
        status: String,
        reason: String?,
    ) {
        queueDao.updateStatus(
            tenant = tenantId,
            id = entry.id,
            status = status,
            attempts = entry.attempts,
            nextAttemptAt = Long.MAX_VALUE,
            lastError = (reason ?: status).take(500),
        )
    }

    private suspend fun scheduleRetry(
        entry: SyncQueueEntity,
        tenantId: String,
        nowMillis: Long,
        error: String,
    ) {
        val attempts = (entry.attempts + 1).coerceAtMost(MAX_ATTEMPTS)
        val delay = BACKOFF_BASE_MILLIS * (1L shl (attempts - 1).coerceAtMost(6))
        queueDao.updateStatus(
            tenant = tenantId,
            id = entry.id,
            status = if (attempts >= MAX_ATTEMPTS) "FAILED" else "PENDING",
            attempts = attempts,
            nextAttemptAt = nowMillis + delay.coerceAtMost(MAX_BACKOFF_MILLIS),
            lastError = error.take(500),
        )
    }

    private fun SyncQueueEntity.toRemote(json: Json) = ProSyncOperation(
        idempotencyKey = idempotencyKey,
        operation = operation,
        entityType = entityType,
        entityId = entityId,
        payload = json.parseToJsonElement(payload).jsonObject,
        version = version,
    )

    companion object {
        const val BATCH_SIZE = 100
        const val MAX_ATTEMPTS = 8
        val BACKOFF_BASE_MILLIS = TimeUnit.SECONDS.toMillis(30)
        val MAX_BACKOFF_MILLIS = TimeUnit.HOURS.toMillis(6)
    }
}

sealed interface SyncResult {
    data class Succeeded(val accepted: Int) : SyncResult
    data class RetryScheduled(val count: Int) : SyncResult
    data object AuthRequired : SyncResult
}
