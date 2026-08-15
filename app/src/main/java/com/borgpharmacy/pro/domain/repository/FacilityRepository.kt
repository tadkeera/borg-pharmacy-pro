package com.borgpharmacy.pro.domain.repository
import com.borgpharmacy.pro.domain.model.FacilityProfile
import kotlinx.coroutines.flow.Flow
interface FacilityRepository { fun observe():Flow<FacilityProfile?>; suspend fun save(profile:FacilityProfile) }
