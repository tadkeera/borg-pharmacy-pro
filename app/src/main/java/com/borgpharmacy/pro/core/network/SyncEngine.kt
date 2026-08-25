package com.borgpharmacy.pro.core.network

import com.borgpharmacy.pro.core.database.dao.SyncDao
import com.borgpharmacy.pro.core.database.entity.*
import com.borgpharmacy.pro.domain.sync.SyncPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

interface SyncTransport {
    suspend fun push(tenantId: String, operation: OutboxEntity): RemoteAck
    suspend fun pull(tenantId: String, cursor: String?, pageSize: Int): PullPage
}
data class RemoteAck(val accepted: Boolean, val conflict: Boolean = false, val serverVersion: Long = 0L, val serverPayload: String = "", val error: String? = null)
data class PullPage(val cursor: Long?, val operations: List<RemoteChange> = emptyList())

class SupabaseSyncTransport : SyncTransport {
    override suspend fun push(tenantId: String, operation: OutboxEntity): RemoteAck {
        SupabaseClientProvider.client.postgrest.rpc("sync_push", buildJsonObject {
            put("tenant_id", tenantId); put("operation_id", operation.operationId); put("idempotency_key", operation.idempotencyKey)
            put("entity_type", operation.entityType); put("entity_id", operation.entityId); put("operation", operation.operation); put("payload", operation.payload)
        })
        return RemoteAck(accepted = true)
    }
    override suspend fun pull(tenantId: String, cursor: Long?, pageSize: Int): PullPage {
        SupabaseClientProvider.client.postgrest.rpc("sync_pull", buildJsonObject { put("tenant_id", tenantId); put("cursor", cursor ?: 0L); put("page_size", pageSize) })
        return PullPage(cursor)
    }
}

class SyncEngine(private val dao: SyncDao, private val metadata: com.borgpharmacy.pro.core.database.dao.SyncMetadataDao, private val reconciler: PullReconciler, private val audit: com.borgpharmacy.pro.core.audit.AuditLogRepository? = null, private val transport: SyncTransport = SupabaseSyncTransport()) {
    suspend fun syncTenant(tenantId: String) = withContext(Dispatchers.IO) {
        require(tenantId.isNotBlank()) { "tenantId is required" }
        var pushed = 0; var conflicts = 0; var error: String? = null
        val started = System.currentTimeMillis()
        dao.readyOutbox(tenantId, started, 50).forEach { operation ->
            val attempt = operation.attempts + 1
            dao.updateState(operation.operationId, tenantId, SyncState.SYNCING.name, attempt, operation.nextAttemptAt, null)
            runCatching { transport.push(tenantId, operation) }.onSuccess { ack ->
                if (ack.conflict) {
                    audit?.sync(tenantId,"system","SYNC","conflict_created",operation.entityType)
                    conflicts++
                    dao.conflict(ConflictEntity(tenantId=tenantId,operationId=operation.operationId,entityType=operation.entityType,entityId=operation.entityId,localVersion=operation.updatedAt,serverVersion=ack.serverVersion,localPayload=operation.payload,serverPayload=ack.serverPayload))
                    dao.updateState(operation.operationId, tenantId, SyncState.CONFLICT.name, attempt, Long.MAX_VALUE, ack.error)
                } else if (ack.accepted) {
                    pushed++; audit?.sync(tenantId,"system","SYNC","sync_push",operation.entityType); dao.updateState(operation.operationId, tenantId, SyncState.SUCCESS.name, attempt, Long.MAX_VALUE, null)
                } else dao.updateState(operation.operationId, tenantId, SyncPolicy.stateForFailure(attempt).name, attempt, SyncPolicy.nextAttempt(attempt), ack.error)
            }.onFailure { cause ->
                error = cause.message
                audit?.sync(tenantId,"system","SYNC","sync_failed",cause.message ?: "unknown")
                dao.updateState(operation.operationId, tenantId, SyncPolicy.stateForFailure(attempt).name, attempt, SyncPolicy.nextAttempt(attempt), cause.message)
            }
        }
        runCatching { val cursor=metadata.get(tenantId)?.lastSuccessfulCursor ?: 0L; val page=transport.pull(tenantId,cursor,100); reconciler.apply(tenantId,page.operations,page.cursor ?: cursor); audit?.sync(tenantId,"system","SYNC","sync_pull",(page.operations.size).toString()) }.onFailure { error=it.message }
        dao.log(SyncLogEntity(tenantId=tenantId,finishedAt=System.currentTimeMillis(),pushed=pushed,conflicts=conflicts,error=error))
    }
}
