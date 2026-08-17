package com.borgpharmacy.pro.data.repository

import com.borgpharmacy.pro.core.database.dao.CompanyDao
import com.borgpharmacy.pro.core.database.dao.SyncQueueDao
import com.borgpharmacy.pro.core.database.dao.VisitDao
import com.borgpharmacy.pro.core.database.entity.SyncQueueEntity
import com.borgpharmacy.pro.core.database.entity.VisitEntity
import com.borgpharmacy.pro.domain.model.Company
import com.borgpharmacy.pro.domain.model.CyclePolicy
import com.borgpharmacy.pro.domain.model.Shift
import com.borgpharmacy.pro.domain.model.Visit
import com.borgpharmacy.pro.domain.repository.BorgRepository
import com.borgpharmacy.pro.domain.scheduler.DynamicScheduleEngine
import java.time.LocalDate
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class OfflineFirstBorgRepository(
    private val companiesDao: CompanyDao,
    private val visitsDao: VisitDao,
    private val queueDao: SyncQueueDao,
    private val engine: DynamicScheduleEngine = DynamicScheduleEngine(),
) : BorgRepository {
    override fun companies(tenant: String) = companiesDao.observe(tenant).map { rows ->
        rows.map { company ->
            Company(company.id, company.name, company.baseDay, Shift.valueOf(company.baseShift))
        }
    }

    override fun visits(tenant: String) = visitsDao.observe(tenant).map { rows ->
        rows.map { visit ->
            Visit(
                visit.id,
                visit.companyId,
                LocalDate.ofEpochDay(visit.cycleStart),
                visit.week,
                LocalDate.ofEpochDay(visit.date),
                Shift.valueOf(visit.shift),
                visit.slotIndex,
                visit.isDeleted,
            )
        }
    }

    override suspend fun reconcile(
        tenantId: String,
        company: Company,
        cycle: LocalDate,
        policy: CyclePolicy,
    ) {
        require(tenantId.isNotBlank()) { "tenantId is required for reconciliation" }
        val existing = visitsDao.list(tenantId, cycle.toEpochDay()).map { visit ->
            Visit(
                visit.id,
                visit.companyId,
                cycle,
                visit.week,
                LocalDate.ofEpochDay(visit.date),
                Shift.valueOf(visit.shift),
                visit.slotIndex,
                visit.isDeleted,
            )
        }
        val version = System.currentTimeMillis()
        val generatedEntities = engine.generate(company, cycle, policy, existing).map { visit ->
            VisitEntity(
                id = visit.id,
                tenantId = tenantId,
                companyId = visit.companyId,
                cycleStart = visit.cycleStart.toEpochDay(),
                week = visit.week,
                date = visit.date.toEpochDay(),
                shift = visit.shift.name,
                slotIndex = visit.slotIndex,
                isDeleted = visit.deleted,
                updatedAt = version,
                syncVersion = version,
            )
        }
        visitsDao.upsertAll(generatedEntities)
        generatedEntities.forEach { entity ->
            queueDao.enqueue(
                SyncQueueEntity(
                    tenantId = tenantId,
                    entityType = "VISIT",
                    entityId = entity.id,
                    operation = "UPSERT",
                    payload = buildJsonObject {
                        put("id", entity.id)
                        put("tenant_id", tenantId)
                        put("company_id", entity.companyId)
                        put("cycle_start", entity.cycleStart)
                        put("week", entity.week)
                        put("date", entity.date)
                        put("shift", entity.shift)
                        put("slot_index", entity.slotIndex)
                        put("is_deleted", entity.isDeleted)
                    }.toString(),
                    version = entity.syncVersion,
                    idempotencyKey = "$tenantId:VISIT:${entity.id}:${entity.syncVersion}",
                ),
            )
        }
    }
}
