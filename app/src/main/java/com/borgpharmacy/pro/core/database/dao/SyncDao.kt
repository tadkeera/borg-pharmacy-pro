package com.borgpharmacy.pro.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.borgpharmacy.pro.core.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncDao {
    @Query("SELECT * FROM sync_outbox WHERE tenantId=:tenant AND state IN ('PENDING','FAILED') AND nextAttemptAt <= :now ORDER BY createdAt LIMIT :limit")
    suspend fun readyOutbox(tenant: String, now: Long, limit: Int): List<OutboxEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun enqueue(operation: OutboxEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun enqueueAll(operations: List<OutboxEntity>)
    @Query("UPDATE sync_outbox SET state=:state, attempts=:attempts, nextAttemptAt=:nextAttemptAt, lastError=:error, updatedAt=:updatedAt WHERE operationId=:operationId AND tenantId=:tenant")
    suspend fun updateState(operationId: String, tenant: String, state: String, attempts: Int, nextAttemptAt: Long, error: String?, updatedAt: Long = System.currentTimeMillis())
    @Query("SELECT * FROM sync_outbox WHERE operationId=:operationId AND tenantId=:tenant LIMIT 1") suspend fun find(operationId: String, tenant: String): OutboxEntity?
    @Query("SELECT * FROM sync_outbox WHERE tenantId=:tenant ORDER BY createdAt DESC") fun observeOutbox(tenant: String): Flow<List<OutboxEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun conflict(value: ConflictEntity)
    @Query("SELECT * FROM sync_conflicts WHERE tenantId=:tenant AND resolutionStatus='OPEN' ORDER BY createdAt") fun observeConflicts(tenant: String): Flow<List<ConflictEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun log(value: SyncLogEntity)
}
