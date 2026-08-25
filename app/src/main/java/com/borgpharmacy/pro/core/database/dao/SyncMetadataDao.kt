package com.borgpharmacy.pro.core.database.dao
import androidx.room.*
import com.borgpharmacy.pro.core.database.entity.SyncMetadataEntity
@Dao interface SyncMetadataDao { @Query("SELECT * FROM sync_metadata WHERE tenantId=:tenant LIMIT 1") suspend fun get(tenant:String):SyncMetadataEntity?; @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun upsert(value:SyncMetadataEntity) }
