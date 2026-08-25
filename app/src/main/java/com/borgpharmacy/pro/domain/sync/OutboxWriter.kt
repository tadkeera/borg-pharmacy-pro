package com.borgpharmacy.pro.domain.sync

import android.content.Context
import com.borgpharmacy.pro.core.database.dao.SyncDao
import com.borgpharmacy.pro.core.database.entity.OutboxEntity
import com.borgpharmacy.pro.core.network.SyncScheduler
import java.util.UUID

class OutboxWriter(private val dao: SyncDao, private val context: Context) {
    suspend fun enqueue(tenantId: String, entityType: String, entityId: String, operation: String, payload: String, idempotencyKey: String = UUID.randomUUID().toString()): String {
        require(tenantId.isNotBlank())
        val item = OutboxEntity(tenantId=tenantId,entityType=entityType,entityId=entityId,operation=operation,payload=payload,idempotencyKey=idempotencyKey)
        dao.enqueue(item)
        SyncScheduler.enqueue(context, tenantId)
        return item.operationId
    }
}
