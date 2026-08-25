package com.borgpharmacy.pro.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class SyncState { PENDING, SYNCING, SUCCESS, FAILED, CONFLICT }

@Entity(tableName = "sync_outbox", indices = [Index(value = ["tenantId", "state", "nextAttemptAt"]), Index(value = ["tenantId", "idempotencyKey"], unique = true)])
data class OutboxEntity(
    @PrimaryKey val operationId: String = UUID.randomUUID().toString(),
    val tenantId: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payload: String,
    val idempotencyKey: String = operationId,
    val state: String = SyncState.PENDING.name,
    val attempts: Int = 0,
    val nextAttemptAt: Long = System.currentTimeMillis(),
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_conflicts", indices = [Index("tenantId"), Index(value = ["tenantId", "operationId"], unique = true)])
data class ConflictEntity(
    @PrimaryKey val conflictId: String = UUID.randomUUID().toString(),
    val tenantId: String,
    val operationId: String,
    val entityType: String,
    val entityId: String,
    val localVersion: Long,
    val serverVersion: Long,
    val localPayload: String,
    val serverPayload: String,
    val resolutionStatus: String = "OPEN",
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null
)

@Entity(tableName = "sync_logs", indices = [Index("tenantId"), Index("startedAt")])
data class SyncLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tenantId: String,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null,
    val pushed: Int = 0,
    val pulled: Int = 0,
    val conflicts: Int = 0,
    val error: String? = null
)
