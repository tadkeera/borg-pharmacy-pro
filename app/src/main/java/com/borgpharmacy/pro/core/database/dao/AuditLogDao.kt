package com.borgpharmacy.pro.core.database.dao
import androidx.room.*
import com.borgpharmacy.pro.core.database.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow
@Dao interface AuditLogDao { @Insert suspend fun insert(log:AuditLogEntity); @Query("SELECT * FROM audit_logs WHERE tenantId=:tenant ORDER BY occurredAt DESC LIMIT :limit") fun observe(tenant:String,limit:Int=200):Flow<List<AuditLogEntity>> }
