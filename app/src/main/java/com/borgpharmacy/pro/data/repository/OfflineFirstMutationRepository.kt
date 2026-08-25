package com.borgpharmacy.pro.data.repository
import androidx.room.withTransaction
import com.borgpharmacy.pro.AppContainer
import com.borgpharmacy.pro.core.database.entity.*
import com.borgpharmacy.pro.domain.model.*
import com.borgpharmacy.pro.domain.sync.OutboxWriter
import com.borgpharmacy.pro.domain.validation.BusinessValidators
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
class OfflineFirstMutationRepository(private val container:AppContainer,private val outbox:OutboxWriter){
 suspend fun saveCompany(tenant:String,company:Company,operation:String="UPDATE"){BusinessValidators.company(company,tenant);val p=buildJsonObject{put("name",company.name);put("base_day_index",company.baseDay);put("base_shift",company.baseShift.name)}.toString();container.database.withTransaction{container.database.companyDao().upsert(CompanyEntity(company.id,tenant,company.name,company.baseDay,company.baseShift.name,false));outbox.enqueue(tenant,"COMPANY",company.id,operation,p,"COMPANY:$tenant:${company.id}:$operation:${System.currentTimeMillis()}");container.auditLogRepository.record(tenant,"local","FACILITY_ADMIN","company_$operation","COMPANY",company.id)}}
 suspend fun saveRepresentative(tenant:String,rep:Representative,operation:String="UPDATE"){BusinessValidators.representative(rep,tenant);val p=buildJsonObject{put("company_id",rep.companyId);put("name",rep.name);put("phone",rep.phone)}.toString();container.database.withTransaction{container.database.representativeDao().upsert(RepresentativeEntity(rep.id,tenant,rep.companyId,rep.name,rep.phone,false));outbox.enqueue(tenant,"REPRESENTATIVE",rep.id,operation,p,"REP:$tenant:${rep.id}:$operation:${System.currentTimeMillis()}");container.auditLogRepository.record(tenant,"local","FACILITY_ADMIN","representative_$operation","REPRESENTATIVE",rep.id)}}
 suspend fun saveVisit(tenant:String,visit:Visit,operation:String="UPDATE"){BusinessValidators.visit(visit,tenant);val p=buildJsonObject{put("company_id",visit.companyId);put("date_epoch_day",visit.date.toEpochDay());put("week_of_cycle",visit.week);put("shift",visit.shift.name);put("slot_index",visit.slotIndex)}.toString();container.database.withTransaction{container.database.visitDao().upsert(VisitEntity(visit.id,tenant,visit.companyId,visit.cycleStart.toEpochDay(),visit.week,visit.date.toEpochDay(),visit.shift.name,visit.slotIndex,visit.deleted));outbox.enqueue(tenant,"VISIT",visit.id,operation,p,"VISIT:$tenant:${visit.id}:$operation:${System.currentTimeMillis()}");container.auditLogRepository.record(tenant,"local","FACILITY_ADMIN","visit_$operation","VISIT",visit.id)}}
}
