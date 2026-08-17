package com.borgpharmacy.pro.core.sync

import com.borgpharmacy.data.remote.SecureSyncOperationDto
import com.borgpharmacy.data.remote.SupabaseSyncService
import com.borgpharmacy.pro.core.database.dao.SyncQueueDao
import com.borgpharmacy.pro.core.database.entity.SyncQueueEntity
import com.borgpharmacy.pro.core.security.SecureSessionStore
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class SyncManager(
    private val queueDao: SyncQueueDao,
    private val sessionStore: SecureSessionStore,
    private val syncService: SupabaseSyncService,
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
            val accepted = response.accepted.map { it.idempotencyKey }.toSet()
            pending.forEach { entry ->
                if (entry.idempotencyKey in accepted) {
                    queueDao.delete(tenantId, entry.id)
                } else {
                    scheduleRetry(entry, tenantId, nowMillis, "operation was not accepted")
                }
            }
            SyncResult.Succeeded(accepted.size)
        } catch (error: Throwable) {
            pending.forEach { scheduleRetry(it, tenantId, nowMillis, error.message ?: error.javaClass.simpleName) }
            SyncResult.RetryScheduled(pending.size)
        }
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

    private fun SyncQueueEntity.toRemote(json: Json) = SecureSyncOperationDto(
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
