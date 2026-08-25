package com.borgpharmacy.pro.core.audit
import com.borgpharmacy.pro.core.database.dao.AuditLogDao
import com.borgpharmacy.pro.core.database.entity.AuditLogEntity
class AuditLogRepository(private val dao:AuditLogDao){
 suspend fun record(tenant:String,actor:String,role:String,action:String,type:String,id:String?=null,metadata:String=""){require(tenant.isNotBlank());dao.insert(AuditLogEntity(tenantId=tenant,actorUserId=actor,role=role,action=action,entityType=type,entityId=id,metadata=metadata))}
 suspend fun userCreated(t:String,a:String,r:String,id:String,m:String="")=record(t,a,r,AuditEvents.USER_CREATED,"USER",id,m)
 suspend fun userUpdated(t:String,a:String,r:String,id:String,m:String="")=record(t,a,r,AuditEvents.USER_UPDATED,"USER",id,m)
 suspend fun passwordChanged(t:String,a:String,r:String,id:String)=record(t,a,r,AuditEvents.PASSWORD_CHANGED,"USER",id)
 suspend fun roleChanged(t:String,a:String,r:String,id:String,m:String)=record(t,a,r,AuditEvents.ROLE_CHANGED,"USER",id,m)
 suspend fun sync(t:String,a:String,r:String,action:String,m:String="")=record(t,a,r,action,"SYNC",null,m)
 suspend fun conflict(t:String,a:String,r:String,id:String,resolved:Boolean,m:String="")=record(t,a,r,if(resolved) AuditEvents.CONFLICT_RESOLVED else AuditEvents.CONFLICT_CREATED,"CONFLICT",id,m)
}
