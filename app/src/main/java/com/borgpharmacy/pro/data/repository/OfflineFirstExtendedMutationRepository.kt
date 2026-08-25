package com.borgpharmacy.pro.data.repository

import androidx.room.withTransaction
import com.borgpharmacy.pro.AppContainer
import com.borgpharmacy.pro.core.database.entity.*
import com.borgpharmacy.pro.domain.model.FacilityProfile
import com.borgpharmacy.pro.domain.sync.OutboxWriter
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

/** Transactional mutations for settings and print audit records. */
class OfflineFirstExtendedMutationRepository(private val container: AppContainer, private val outbox: OutboxWriter) {
    suspend fun saveFacilityProfile(profile: FacilityProfile) {
        require(profile.tenantId.isNotBlank())
        val payload = buildJsonObject { put("arabic_name", profile.arabicName); put("english_name", profile.englishName); put("logo_path", profile.logoPath ?: ""); put("policy", profile.policy.visits) }.toString()
        container.database.withTransaction {
            container.database.facilityDao().upsert(FacilityProfileEntity(profile.id, profile.tenantId, profile.arabicName, profile.englishName, profile.logoPath, profile.policy.visits, profile.adminUsername, profile.adminPasswordHash))
            outbox.enqueue(profile.tenantId, "FACILITY_PROFILE", profile.id, "UPDATE", payload, "FACILITY:${profile.tenantId}:${profile.id}:${profile.updatedKey()}"); container.auditLogRepository.record(profile.tenantId, "local", "FACILITY_ADMIN", "facility_profile_update", "FACILITY_PROFILE", profile.id)
        }
    }
    suspend fun changeVisitPolicy(profile: FacilityProfile, visits: Int) {
        require(visits in 1..4)
        saveFacilityProfile(profile.copy(policy = com.borgpharmacy.pro.domain.model.CyclePolicy.entries.first { it.visits == visits }))
    }
    suspend fun reprint(tenantId: String, visitId: String, actor: String, reason: String) { recordPrint(tenantId, visitId); container.auditLogRepository.record(tenantId, actor, "RECEPTIONIST", "permit_reprinted", "PRINT_LOG", visitId, reason) }
    suspend fun recordPrint(tenantId: String, visitId: String) {
        require(tenantId.isNotBlank())
        val id = UUID.randomUUID().toString()
        val payload = buildJsonObject { put("visit_id", visitId); put("printed_at", System.currentTimeMillis()) }.toString()
        container.database.withTransaction {
            container.database.printLogDao().insert(PrintLogEntity(id, tenantId, visitId))
            outbox.enqueue(tenantId, "PRINT_LOG", id, "CREATE", payload, "PRINT:$tenantId:$id"); container.auditLogRepository.record(tenantId, "local", "RECEPTIONIST", "permit_print", "PRINT_LOG", id)
        }
    }
    suspend fun delete(tenantId: String, entityType: String, entityId: String, payload: String = "{}") {
        require(tenantId.isNotBlank())
        outbox.enqueue(tenantId, entityType, entityId, "DELETE", payload, "DELETE:$tenantId:$entityType:$entityId")
    }
    suspend fun recordScheduleChange(tenant:String,actor:String,visitId:String,action:String="schedule_change")=container.auditLogRepository.record(tenant,actor,"FACILITY_ADMIN",action,"VISIT",visitId)
    private fun FacilityProfile.updatedKey() = "${policy.visits}"
}
