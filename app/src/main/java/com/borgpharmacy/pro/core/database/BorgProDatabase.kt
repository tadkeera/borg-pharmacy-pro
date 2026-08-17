package com.borgpharmacy.pro.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.borgpharmacy.pro.core.database.dao.CompanyDao
import com.borgpharmacy.pro.core.database.dao.FacilityDao
import com.borgpharmacy.pro.core.database.dao.PrintLogDao
import com.borgpharmacy.pro.core.database.dao.RepresentativeDao
import com.borgpharmacy.pro.core.database.dao.SyncQueueDao
import com.borgpharmacy.pro.core.database.dao.UserDao
import com.borgpharmacy.pro.core.database.dao.VisitDao
import com.borgpharmacy.pro.core.database.entity.CompanyEntity
import com.borgpharmacy.pro.core.database.entity.FacilityProfileEntity
import com.borgpharmacy.pro.core.database.entity.PrintLogEntity
import com.borgpharmacy.pro.core.database.entity.RepresentativeEntity
import com.borgpharmacy.pro.core.database.entity.SyncQueueEntity
import com.borgpharmacy.pro.core.database.entity.UserEntity
import com.borgpharmacy.pro.core.database.entity.VisitEntity

@Database(
    entities = [
        FacilityProfileEntity::class,
        CompanyEntity::class,
        RepresentativeEntity::class,
        VisitEntity::class,
        PrintLogEntity::class,
        UserEntity::class,
        SyncQueueEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class BorgProDatabase : RoomDatabase() {
    abstract fun facilityDao(): FacilityDao
    abstract fun companyDao(): CompanyDao
    abstract fun representativeDao(): RepresentativeDao
    abstract fun visitDao(): VisitDao
    abstract fun printLogDao(): PrintLogDao
    abstract fun userDao(): UserDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        const val DATABASE_NAME = "borg_pharmacy_pro.db"
    }
}
