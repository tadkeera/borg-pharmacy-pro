package com.borgpharmacy.pro.core.database.entity
import androidx.room.*
import java.util.UUID
@Entity(tableName="audit_logs",indices=[Index("tenantId"),Index("occurredAt"),Index("actorUserId")])
data class AuditLogEntity(@PrimaryKey val id:String=UUID.randomUUID().toString(),val tenantId:String,val actorUserId:String,val role:String,val action:String,val entityType:String,val entityId:String?,val metadata:String="",val occurredAt:Long=System.currentTimeMillis())
