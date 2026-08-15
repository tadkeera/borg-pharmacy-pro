package com.borgpharmacy.pro.core.database.entity
import androidx.room.*
import com.borgpharmacy.pro.domain.model.*
import java.util.UUID
@Entity(tableName="facility_profiles") data class FacilityProfileEntity(@PrimaryKey val id:String=UUID.randomUUID().toString(),val tenantId:String=id,val arabicName:String,val englishName:String,val logoPath:String?,val policy:Int,val adminUsername:String="admin",val adminPasswordHash:String="",val updatedAt:Long=System.currentTimeMillis())
@Entity(tableName="companies",indices=[Index("tenantId")]) data class CompanyEntity(@PrimaryKey val id:String=UUID.randomUUID().toString(),val tenantId:String,val name:String,val baseDay:Int,val baseShift:String,val isDeleted:Boolean=false)
@Entity(tableName="representatives",indices=[Index("companyId")]) data class RepresentativeEntity(@PrimaryKey val id:String=UUID.randomUUID().toString(),val tenantId:String,val companyId:String,val name:String,val phone:String,val isDeleted:Boolean=false)
@Entity(tableName="visits",indices=[Index("tenantId"),Index("cycleStart"),Index("date")]) data class VisitEntity(@PrimaryKey val id:String=UUID.randomUUID().toString(),val tenantId:String,val companyId:String,val cycleStart:Long,val week:Int,val date:Long,val shift:String,val slotIndex:Int,val isDeleted:Boolean=false)
@Entity(tableName="print_logs") data class PrintLogEntity(@PrimaryKey val id:String=UUID.randomUUID().toString(),val tenantId:String,val visitId:String,val printedAt:Long=System.currentTimeMillis())
@Entity(tableName="users",indices=[Index(value=["tenantId","username"],unique=true)]) data class UserEntity(@PrimaryKey val id:String=UUID.randomUUID().toString(),val tenantId:String,val username:String,val passwordHash:String,val salt:String,val isAdmin:Boolean=true)
