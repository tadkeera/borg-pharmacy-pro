package com.borgpharmacy.pro.domain.model
import java.time.LocalDate
import java.util.UUID
enum class Shift(val arabicName:String){ MORNING("الفترة الصباحية"), EVENING("الفترة المسائية") }
enum class CyclePolicy(val visits:Int){ ONE(1), TWO(2), THREE(3), FOUR(4) }
data class FacilityProfile(val id:String=UUID.randomUUID().toString(),val arabicName:String,val englishName:String,val logoPath:String?,val policy:CyclePolicy,val adminUsername:String="admin",val adminPasswordHash:String="",val tenantId:String=id)
data class Company(val id:String=UUID.randomUUID().toString(),val name:String,val baseDay:Int,val baseShift:Shift)
data class Representative(val id:String=UUID.randomUUID().toString(),val companyId:String,val name:String,val phone:String)
data class Visit(val id:String=UUID.randomUUID().toString(),val companyId:String,val cycleStart:LocalDate,val week:Int,val date:LocalDate,val shift:Shift,val slotIndex:Int,val deleted:Boolean=false)
