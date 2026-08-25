package com.borgpharmacy.pro.domain.repository

import com.borgpharmacy.pro.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface BorgRepository {
    fun companies(tenant: String): Flow<List<Company>>
    fun visits(tenant: String): Flow<List<Visit>>
    suspend fun reconcile(company: Company, cycle: LocalDate, policy: CyclePolicy, tenant: String)
}
