package com.borgpharmacy.pro.data.repository

import com.borgpharmacy.pro.core.database.dao.CompanyDao
import com.borgpharmacy.pro.core.database.dao.VisitDao
import com.borgpharmacy.pro.core.database.entity.VisitEntity
import com.borgpharmacy.pro.domain.model.*
import com.borgpharmacy.pro.domain.repository.BorgRepository
import com.borgpharmacy.pro.domain.scheduler.DynamicScheduleEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class OfflineFirstBorgRepository(
    private val companiesDao: CompanyDao,
    private val visitsDao: VisitDao,
    private val engine: DynamicScheduleEngine = DynamicScheduleEngine()
) : BorgRepository {
    override fun companies(tenant: String): Flow<List<Company>> = companiesDao.observe(tenant).map { rows ->
        rows.map { Company(it.id, it.name, it.baseDay, Shift.valueOf(it.baseShift)) }
    }

    override fun visits(tenant: String): Flow<List<Visit>> = visitsDao.observe(tenant).map { rows ->
        rows.map { Visit(it.id, it.companyId, LocalDate.ofEpochDay(it.cycleStart), it.week, LocalDate.ofEpochDay(it.date), Shift.valueOf(it.shift), it.slotIndex, it.isDeleted) }
    }

    override suspend fun reconcile(company: Company, cycle: LocalDate, policy: CyclePolicy, tenant: String) {
        require(tenant.isNotBlank()) { "tenant is required for schedule reconciliation" }
        val existing = visitsDao.list(tenant, cycle.toEpochDay()).filter { it.companyId == company.id }.map {
            Visit(it.id, it.companyId, cycle, it.week, LocalDate.ofEpochDay(it.date), Shift.valueOf(it.shift), it.slotIndex, it.isDeleted)
        }
        val reconciled = engine.generate(company, cycle, policy, existing)
        visitsDao.upsertAll(reconciled.map {
            VisitEntity(it.id, tenant, it.companyId, it.cycleStart.toEpochDay(), it.week, it.date.toEpochDay(), it.shift.name, it.slotIndex, it.deleted)
        })
    }
}
