package com.borgpharmacy.pro.data.repository
import com.borgpharmacy.pro.core.database.dao.FacilityDao
import com.borgpharmacy.pro.core.database.entity.FacilityProfileEntity
import com.borgpharmacy.pro.domain.model.*
import com.borgpharmacy.pro.domain.repository.FacilityRepository
import kotlinx.coroutines.flow.*
class OfflineFirstFacilityRepository(private val dao:FacilityDao):FacilityRepository { override fun observe()=dao.observe().map{it?.let{FacilityProfile(it.id,it.arabicName,it.englishName,it.logoPath,CyclePolicy.entries.first{p->p.visits==it.policy},it.adminUsername,it.adminPasswordHash,it.tenantId)}}; override suspend fun save(profile:FacilityProfile)=dao.upsert(FacilityProfileEntity(profile.id,profile.tenantId,profile.arabicName,profile.englishName,profile.logoPath,profile.policy.visits,profile.adminUsername,profile.adminPasswordHash)) }
