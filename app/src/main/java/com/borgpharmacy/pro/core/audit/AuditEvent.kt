package com.borgpharmacy.pro.core.audit
data class AuditEvent(val tenantId:String,val actorId:String,val role:String,val action:String,val entityType:String,val entityId:String?,val timestamp:Long,val metadata:String){init{require(tenantId.isNotBlank());require(actorId.isNotBlank());require(role.isNotBlank());require(action.isNotBlank());require(entityType.isNotBlank());require(timestamp>0)}}
