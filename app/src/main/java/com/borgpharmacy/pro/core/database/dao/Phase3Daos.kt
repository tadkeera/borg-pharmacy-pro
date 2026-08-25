package com.borgpharmacy.pro.core.database.dao
import androidx.room.*
import com.borgpharmacy.pro.core.database.entity.*
import kotlinx.coroutines.flow.Flow
@Dao interface FacilityVisitAuthorizationDao{@Query("SELECT * FROM facility_visit_authorizations WHERE tenantId=:tenant AND visitDate=:date AND status='AUTHORIZED' ORDER BY shift") fun observeToday(tenant:String,date:Long):Flow<List<FacilityVisitAuthorizationEntity>>;@Insert(onConflict=OnConflictStrategy.REPLACE)suspend fun upsert(v:FacilityVisitAuthorizationEntity)}
@Dao interface EntryPermitDao{@Query("SELECT * FROM entry_permits WHERE tenantId=:tenant AND permitNumber=:number LIMIT 1")suspend fun find(tenant:String,number:String):EntryPermitEntity?;@Insert(onConflict=OnConflictStrategy.REPLACE)suspend fun upsert(v:EntryPermitEntity);@Query("UPDATE entry_permits SET status=:status WHERE id=:id AND tenantId=:tenant")suspend fun updateStatus(tenant:String,id:String,status:String)}
@Dao interface EmployeeDao{@Query("SELECT * FROM employees WHERE tenantId=:tenant AND active=1 ORDER BY name")fun observe(tenant:String):Flow<List<EmployeeEntity>>;@Insert(onConflict=OnConflictStrategy.REPLACE)suspend fun upsert(v:EmployeeEntity)}
