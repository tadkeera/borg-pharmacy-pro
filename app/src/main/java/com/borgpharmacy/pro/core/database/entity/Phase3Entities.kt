package com.borgpharmacy.pro.core.database.entity
import androidx.room.*
import java.util.UUID
@Entity(tableName="facility_visit_authorizations",indices=[Index("tenantId"),Index("visitDate")])
data class FacilityVisitAuthorizationEntity(@PrimaryKey val id:String=UUID.randomUUID().toString(),val tenantId:String,val facilityId:String,val companyId:String,val representativeId:String,val visitDate:Long,val shift:String,val status:String="AUTHORIZED",val createdAt:Long=System.currentTimeMillis(),val updatedAt:Long=System.currentTimeMillis())
@Entity(tableName="entry_permits",indices=[Index(value=["tenantId","permitNumber"],unique=true),Index("visitDate")])
data class EntryPermitEntity(@PrimaryKey val id:String=UUID.randomUUID().toString(),val tenantId:String,val facilityId:String,val companyId:String,val representativeId:String,val permitNumber:String,val qrCode:String,val visitDate:Long,val shift:String,val issuedByEmployeeId:String,val printedAt:Long=System.currentTimeMillis(),val status:String="ACTIVE")
@Entity(tableName="employees",indices=[Index(value=["tenantId","username"],unique=true)])
data class EmployeeEntity(@PrimaryKey val id:String=UUID.randomUUID().toString(),val tenantId:String,val username:String,val name:String,val role:String,val active:Boolean=true,val createdAt:Long=System.currentTimeMillis(),val updatedAt:Long=System.currentTimeMillis())
