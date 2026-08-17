package com.borgpharmacy.pro.domain.repository
import com.borgpharmacy.pro.domain.model.*
import kotlinx.coroutines.flow.Flow
interface BorgRepository {
    fun companies(tenant: String): Flow<List<Company>>
    fun visits(tenant: String): Flow<List<Visit>>
    suspend fun reconcile(tenantId: String, company: Company, cycle: java.time.LocalDate, policy: CyclePolicy)
}
