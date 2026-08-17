package com.borgpharmacy.pro

import com.borgpharmacy.pro.domain.model.Company
import com.borgpharmacy.pro.domain.model.CyclePolicy
import com.borgpharmacy.pro.domain.model.Shift
import com.borgpharmacy.pro.domain.model.Visit
import com.borgpharmacy.pro.domain.repository.BorgRepository
import com.borgpharmacy.pro.domain.usecase.ObserveCompaniesUseCase
import com.borgpharmacy.pro.domain.usecase.ReconcileCompanyScheduleUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainUseCasesTest {
    @Test
    fun rejectsBlankTenantBeforeRepositoryCall() {
        val repository = RecordingRepository()
        assertIllegalArgument { ObserveCompaniesUseCase(repository)(" ") }
        assertEquals(0, repository.companyCalls)
    }

    @Test
    fun reconcilesOnlyAfterDomainValidation() = runBlocking {
        val repository = RecordingRepository()
        val useCase = ReconcileCompanyScheduleUseCase(repository)
        val company = Company(id = "company-a", name = "Alpha", baseDay = 0, baseShift = Shift.MORNING)

        useCase("tenant-a", company, LocalDate.of(2026, 8, 17), CyclePolicy.TWO)

        assertEquals("tenant-a", repository.tenantSeen)
        assertTrue(repository.reconcileCalled)
    }

    @Test
    fun rejectsInvalidCompany() = runBlocking {
        val repository = RecordingRepository()
        val invalid = Company(id = "company-a", name = "", baseDay = 0, baseShift = Shift.MORNING)
        assertIllegalArgument { ReconcileCompanyScheduleUseCase(repository)("tenant-a", invalid, LocalDate.of(2026, 8, 17), CyclePolicy.ONE) }
        assertTrue(!repository.reconcileCalled)
    }

    private fun assertIllegalArgument(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }

    private class RecordingRepository : BorgRepository {
        var companyCalls = 0
        var tenantSeen: String? = null
        var reconcileCalled = false

        override fun companies(tenant: String): Flow<List<Company>> {
            companyCalls += 1
            return emptyFlow()
        }

        override fun visits(tenant: String): Flow<List<Visit>> = emptyFlow()

        override suspend fun reconcile(tenantId: String, company: Company, cycle: LocalDate, policy: CyclePolicy) {
            tenantSeen = tenantId
            reconcileCalled = true
        }
    }
}
