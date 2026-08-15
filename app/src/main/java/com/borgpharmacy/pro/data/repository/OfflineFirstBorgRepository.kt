package com.borgpharmacy.pro.data.repository
import com.borgpharmacy.pro.core.database.dao.*
import com.borgpharmacy.pro.core.database.entity.*
import com.borgpharmacy.pro.domain.model.*
import com.borgpharmacy.pro.domain.repository.BorgRepository
import com.borgpharmacy.pro.domain.scheduler.DynamicScheduleEngine
import kotlinx.coroutines.flow.*
import java.time.LocalDate
class OfflineFirstBorgRepository(private val companiesDao:CompanyDao,private val visitsDao:VisitDao,private val engine:DynamicScheduleEngine=DynamicScheduleEngine()):BorgRepository { override fun companies(tenant:String)=companiesDao.observe(tenant).map{it.map{c->Company(c.id,c.name,c.baseDay,Shift.valueOf(c.baseShift))}}; override fun visits(tenant:String)=visitsDao.observe(tenant).map{it.map{v->Visit(v.id,v.companyId,LocalDate.ofEpochDay(v.cycleStart),v.week,LocalDate.ofEpochDay(v.date),Shift.valueOf(v.shift),v.slotIndex,v.isDeleted)}}; override suspend fun reconcile(company:Company,cycle:LocalDate,policy:CyclePolicy){val existing=visitsDao.list("",cycle.toEpochDay()).map{Visit(it.id,it.companyId,cycle,it.week,LocalDate.ofEpochDay(it.date),Shift.valueOf(it.shift),it.slotIndex,it.isDeleted)}; visitsDao.upsertAll(engine.generate(company,cycle,policy,existing).map{VisitEntity(it.id,"",it.companyId,it.cycleStart.toEpochDay(),it.week,it.date.toEpochDay(),it.shift.name,it.slotIndex,it.deleted)})} }
