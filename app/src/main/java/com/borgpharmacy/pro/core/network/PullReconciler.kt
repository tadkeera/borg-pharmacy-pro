package com.borgpharmacy.pro.core.network

import androidx.room.withTransaction
import com.borgpharmacy.pro.core.database.BorgProDatabase
import com.borgpharmacy.pro.core.database.entity.*
import com.borgpharmacy.pro.domain.model.Shift
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable data class RemoteChange(val sequence_id:Long,val entity_type:String,val entity_id:String,val server_version:Long,val payload:JsonObject=buildJsonObject{},val deleted:Boolean=false)
class PullReconciler(private val db:BorgProDatabase) {
    suspend fun apply(tenant:String, changes:List<RemoteChange>, cursor:Long) {
        require(tenant.isNotBlank())
        db.withTransaction {
            changes.forEach { change ->
                val p=change.payload
                when(change.entity_type.uppercase()) {
                    "COMPANY" -> db.companyDao().upsert(CompanyEntity(change.entity_id,tenant,p["name"]?.jsonPrimitive?.content.orEmpty(),p["base_day_index"]?.jsonPrimitive?.intOrNull ?: 0,p["base_shift"]?.jsonPrimitive?.content ?: Shift.MORNING.name,change.deleted))
                    "REPRESENTATIVE" -> db.representativeDao().upsert(RepresentativeEntity(change.entity_id,tenant,p["company_id"]?.jsonPrimitive?.content.orEmpty(),p["name"]?.jsonPrimitive?.content.orEmpty(),p["phone"]?.jsonPrimitive?.content.orEmpty(),change.deleted))
                    "VISIT" -> db.visitDao().upsert(VisitEntity(change.entity_id,tenant,p["company_id"]?.jsonPrimitive?.content.orEmpty(),p["cycle_start_epoch_day"]?.jsonPrimitive?.longOrNull ?: 0,p["week_of_cycle"]?.jsonPrimitive?.intOrNull ?: 0,p["date_epoch_day"]?.jsonPrimitive?.longOrNull ?: 0,p["shift"]?.jsonPrimitive?.content ?: Shift.MORNING.name,p["slot_index"]?.jsonPrimitive?.intOrNull ?: 0,change.deleted))
                }
            }
            db.syncMetadataDao().upsert(SyncMetadataEntity(tenant,cursor,System.currentTimeMillis()))
        }
    }
}
