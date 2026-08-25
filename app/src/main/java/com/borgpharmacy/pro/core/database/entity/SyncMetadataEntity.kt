package com.borgpharmacy.pro.core.database.entity
import androidx.room.Entity
@Entity(tableName="sync_metadata", primaryKeys=["tenantId"])
data class SyncMetadataEntity(val tenantId:String,val lastSuccessfulCursor:Long=0,val lastSyncAt:Long?=null)
