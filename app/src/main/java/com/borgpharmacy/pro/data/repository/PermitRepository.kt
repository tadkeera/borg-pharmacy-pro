package com.borgpharmacy.pro.data.repository
import com.borgpharmacy.pro.core.database.dao.EntryPermitDao
import com.borgpharmacy.pro.core.database.entity.EntryPermitEntity
import com.borgpharmacy.pro.domain.model.Shift
import java.time.LocalDate
import java.util.UUID
class PermitRepository(private val dao:EntryPermitDao){suspend fun issue(tenant:String,facility:String,company:String,rep:String,employee:String,date:LocalDate,shift:Shift):EntryPermitEntity{require(tenant.isNotBlank());val number="RV-${UUID.randomUUID().toString().take(8).uppercase()}";val permit=EntryPermitEntity(tenantId=tenant,facilityId=facility,companyId=company,representativeId=rep,permitNumber=number,qrCode=number,visitDate=date.toEpochDay(),shift=shift.name,issuedByEmployeeId=employee);dao.upsert(permit);return permit}}
