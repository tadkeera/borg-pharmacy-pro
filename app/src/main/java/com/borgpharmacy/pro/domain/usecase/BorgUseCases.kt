package com.borgpharmacy.pro.domain.usecase

import com.borgpharmacy.pro.domain.model.Company
import com.borgpharmacy.pro.domain.model.CyclePolicy
import com.borgpharmacy.pro.domain.model.Visit
import com.borgpharmacy.pro.domain.repository.BorgRepository
import com.borgpharmacy.pro.domain.validation.DomainValidators
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class ObserveCompaniesUseCase(
    private val repository: BorgRepository,
) {
    operator fun invoke(tenantId: String): Flow<List<Company>> =
        repository.companies(DomainValidators.tenantId(tenantId))
}

class ObserveVisitsUseCase(
    private val repository: BorgRepository,
) {
    operator fun invoke(tenantId: String): Flow<List<Visit>> =
        repository.visits(DomainValidators.tenantId(tenantId))
}

class ReconcileCompanyScheduleUseCase(
    private val repository: BorgRepository,
) {
    suspend operator fun invoke(
        tenantId: String,
        company: Company,
        cycleStart: LocalDate,
        policy: CyclePolicy,
    ) {
        val safeTenant = DomainValidators.tenantId(tenantId)
        require(!cycleStart.isBefore(LocalDate.of(2020, 1, 1))) { "cycleStart is outside supported range" }
        DomainValidators.company(company, policy)
        repository.reconcile(safeTenant, company, cycleStart, policy)
    }
}
