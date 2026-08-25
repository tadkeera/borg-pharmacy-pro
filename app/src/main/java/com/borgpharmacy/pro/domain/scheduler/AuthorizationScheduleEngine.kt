package com.borgpharmacy.pro.domain.scheduler
import com.borgpharmacy.pro.domain.model.*
import java.time.LocalDate
class AuthorizationScheduleEngine(private val rotation:DynamicCycleCalculator=DynamicCycleCalculator()){fun generate(tenant:String,facilityId:String,company:Company,rep:Representative,cycleStart:LocalDate,policy:CyclePolicy)=rotation.visits(company.baseDay,company.baseShift,policy).mapIndexed{i,(day,shift)->AuthorizationSlot("$tenant:${rep.id}:$i",tenant,facilityId,company.id,rep.id,cycleStart.plusDays((i*7+day).toLong()),shift,i+1)}}
data class AuthorizationSlot(val id:String,val tenantId:String,val facilityId:String,val companyId:String,val representativeId:String,val visitDate:LocalDate,val shift:Shift,val week:Int)
