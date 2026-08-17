package com.borgpharmacy.pro.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.borgpharmacy.pro.core.database.entity.CompanyEntity
import com.borgpharmacy.pro.core.database.entity.FacilityProfileEntity
import com.borgpharmacy.pro.core.database.entity.PrintLogEntity
import com.borgpharmacy.pro.core.database.entity.RepresentativeEntity
import com.borgpharmacy.pro.core.database.entity.SyncQueueEntity
import com.borgpharmacy.pro.core.database.entity.UserEntity
import com.borgpharmacy.pro.core.database.entity.VisitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FacilityDao {
    @Query("SELECT * FROM facility_profiles LIMIT 1")
    fun observe(): Flow<FacilityProfileEntity?>

    @Query("SELECT * FROM facility_profiles LIMIT 1")
    suspend fun get(): FacilityProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(value: FacilityProfileEntity)
}

@Dao
interface CompanyDao {
    @Query("SELECT * FROM companies WHERE tenantId = :tenant AND isDeleted = 0 ORDER BY name LIMIT 100")
    fun observe(tenant: String): Flow<List<CompanyEntity>>

    @Query("SELECT * FROM companies WHERE tenantId = :tenant AND isDeleted = 0 ORDER BY name LIMIT :limit OFFSET :offset")
    suspend fun page(tenant: String, limit: Int, offset: Int): List<CompanyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(values: List<CompanyEntity>)
}

@Dao
interface RepresentativeDao {
    @Query("SELECT * FROM representatives WHERE tenantId = :tenant AND isDeleted = 0 ORDER BY name LIMIT 100")
    fun observe(tenant: String): Flow<List<RepresentativeEntity>>

    @Query("SELECT * FROM representatives WHERE tenantId = :tenant AND isDeleted = 0 ORDER BY name LIMIT :limit OFFSET :offset")
    suspend fun page(tenant: String, limit: Int, offset: Int): List<RepresentativeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(value: RepresentativeEntity)
}

@Dao
interface VisitDao {
    @Query("SELECT * FROM visits WHERE tenantId = :tenant AND isDeleted = 0 ORDER BY date, shift, slotIndex LIMIT 500")
    fun observe(tenant: String): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE tenantId = :tenant AND cycleStart = :cycle AND isDeleted = 0 ORDER BY date, shift, slotIndex LIMIT 500")
    suspend fun list(tenant: String, cycle: Long): List<VisitEntity>

    @Query("SELECT * FROM visits WHERE tenantId = :tenant AND isDeleted = 0 ORDER BY date, shift, slotIndex LIMIT :limit OFFSET :offset")
    suspend fun page(tenant: String, limit: Int, offset: Int): List<VisitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(values: List<VisitEntity>)

    @Query("UPDATE visits SET isDeleted = 1, updatedAt = :updatedAt, syncVersion = syncVersion + 1 WHERE tenantId = :tenant AND id IN (:ids)")
    suspend fun softDelete(tenant: String, ids: List<String>, updatedAt: Long = System.currentTimeMillis())
}

@Dao
interface PrintLogDao {
    @Insert
    suspend fun insert(value: PrintLogEntity)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE tenantId = :tenant AND username = :username LIMIT 1")
    suspend fun find(tenant: String, username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(value: UserEntity)
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE tenantId = :tenant AND status = 'PENDING' AND nextAttemptAt <= :now ORDER BY createdAt LIMIT :limit")
    suspend fun pending(tenant: String, now: Long, limit: Int): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(value: SyncQueueEntity)

    @Query("UPDATE sync_queue SET status = :status, attempts = :attempts, nextAttemptAt = :nextAttemptAt, lastError = :lastError WHERE tenantId = :tenant AND id = :id")
    suspend fun updateStatus(tenant: String, id: String, status: String, attempts: Int, nextAttemptAt: Long, lastError: String?)

    @Query("DELETE FROM sync_queue WHERE tenantId = :tenant AND id = :id")
    suspend fun delete(tenant: String, id: String)
}
