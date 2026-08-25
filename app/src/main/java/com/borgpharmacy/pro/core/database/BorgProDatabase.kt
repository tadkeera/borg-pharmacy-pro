package com.borgpharmacy.pro.core.database
import androidx.room.Database
import androidx.room.RoomDatabase
import com.borgpharmacy.pro.core.database.entity.*
import com.borgpharmacy.pro.core.database.dao.*
@Database(entities=[FacilityProfileEntity::class,CompanyEntity::class,RepresentativeEntity::class,VisitEntity::class,PrintLogEntity::class,UserEntity::class,OutboxEntity::class,ConflictEntity::class,SyncLogEntity::class,SyncMetadataEntity::class,AuditLogEntity::class,FacilityVisitAuthorizationEntity::class,EntryPermitEntity::class,EmployeeEntity::class],version=6,exportSchema=false)
abstract class BorgProDatabase:RoomDatabase(){ abstract fun facilityDao():FacilityDao; abstract fun companyDao():CompanyDao; abstract fun representativeDao():RepresentativeDao; abstract fun visitDao():VisitDao; abstract fun printLogDao():PrintLogDao; abstract fun userDao():UserDao; abstract fun syncDao():SyncDao; abstract fun syncMetadataDao():SyncMetadataDao; abstract fun auditLogDao():AuditLogDao; abstract fun authorizationDao():FacilityVisitAuthorizationDao; abstract fun permitDao():EntryPermitDao; abstract fun employeeDao():EmployeeDao
 companion object { const val DATABASE_NAME="borg_pharmacy_pro.db" }
}
