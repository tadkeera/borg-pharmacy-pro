package com.borgpharmacy.pro.data.local

import com.borgpharmacy.pro.core.database.dao.CompanyDao
import com.borgpharmacy.pro.core.database.dao.SyncQueueDao
import com.borgpharmacy.pro.core.database.dao.VisitDao
import com.borgpharmacy.pro.core.database.entity.CompanyEntity
import com.borgpharmacy.pro.core.database.entity.SyncQueueEntity
import com.borgpharmacy.pro.core.database.entity.VisitEntity
import kotlinx.coroutines.flow.Flow

interface BorgLocalDataSource {
    fun observeCompanies(tenantId: String): Flow<List<CompanyEntity>>
    fun observeVisits(tenantId: String): Flow<List<VisitEntity>>
    suspend fun listVisits(tenantId: String, cycleStart: Long): List<VisitEntity>
    suspend fun upsertVisits(visits: List<VisitEntity>)
    suspend fun enqueue(operation: SyncQueueEntity)
}

class RoomBorgLocalDataSource(
    private val companyDao: CompanyDao,
    private val visitDao: VisitDao,
    private val syncQueueDao: SyncQueueDao,
) : BorgLocalDataSource {
    override fun observeCompanies(tenantId: String): Flow<List<CompanyEntity>> = companyDao.observe(tenantId)

    override fun observeVisits(tenantId: String): Flow<List<VisitEntity>> = visitDao.observe(tenantId)

    override suspend fun listVisits(tenantId: String, cycleStart: Long): List<VisitEntity> =
        visitDao.list(tenantId, cycleStart)

    override suspend fun upsertVisits(visits: List<VisitEntity>) = visitDao.upsertAll(visits)

    override suspend fun enqueue(operation: SyncQueueEntity) = syncQueueDao.enqueue(operation)
}
