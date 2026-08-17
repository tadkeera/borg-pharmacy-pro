package com.borgpharmacy.pro.data.repository

import com.borgpharmacy.pro.core.database.entity.SyncQueueEntity
import com.borgpharmacy.pro.core.database.entity.VisitEntity
import com.borgpharmacy.pro.data.local.BorgLocalDataSource
import com.borgpharmacy.pro.domain.model.Company
import com.borgpharmacy.pro.domain.model.CyclePolicy
import com.borgpharmacy.pro.domain.model.Shift
import com.borgpharmacy.pro.domain.model.Visit
import com.borgpharmacy.pro.domain.repository.BorgRepository
import com.borgpharmacy.pro.domain.scheduler.DynamicScheduleEngine
import com.borgpharmacy.pro.domain.validation.DomainValidators
import java.time.LocalDate
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class OfflineFirstBorgRepository(
    private val localDataSource: BorgLocalDataSource,
    private val engine: DynamicScheduleEngine = DynamicScheduleEngine(),
) : BorgRepository {
    override fun companies(tenant: String) = localDataSource.observeCompanies(DomainValidators.tenantId(tenant)).map { rows ->
        rows.map { company ->
            Company(company.id, company.name, company.baseDay, Shift.valueOf(company.baseShift))
        }
    }

    override fun visits(tenant: String) = localDataSource.observeVisits(DomainValidators.tenantId(tenant)).map { rows ->
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
        val safeTenant = DomainValidators.tenantId(tenantId)
        DomainValidators.company(company, policy)
        val existing = localDataSource.listVisits(safeTenant, cycle.toEpochDay()).map { visit ->
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
                tenantId = safeTenant,
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
        localDataSource.upsertVisits(generatedEntities)
        generatedEntities.forEach { entity ->
            localDataSource.enqueue(
                SyncQueueEntity(
                    tenantId = safeTenant,
                    entityType = "VISIT",
                    entityId = entity.id,
                    operation = "UPSERT",
                    payload = buildJsonObject {
                        put("id", entity.id)
                        put("tenant_id", safeTenant)
                        put("company_id", entity.companyId)
                        put("cycle_start_epoch_day", entity.cycleStart)
                        put("week_of_cycle", entity.week)
                        put("day_of_cycle", ((entity.date - entity.cycleStart).coerceIn(0L, 27L) + 1L).toInt())
                        put("date_epoch_day", entity.date)
                        put("shift", entity.shift)
                        put("slot_index", entity.slotIndex)
                        put("status", "SCHEDULED")
                        put("is_deleted", entity.isDeleted)
                        put("created_at", version)
                    }.toString(),
                    version = entity.syncVersion,
                    idempotencyKey = "$safeTenant:VISIT:${entity.id}:${entity.syncVersion}",
                ),
            )
        }
    }
}
