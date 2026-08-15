package com.borgpharmacy.pro.domain.scheduler
import com.borgpharmacy.pro.domain.model.*
import java.time.LocalDate
import java.util.UUID
/** Pure reconciliation: retained weeks keep their IDs and coordinates; only added or removed weeks change. */
class DynamicScheduleEngine(private val calculator:DynamicCycleCalculator=DynamicCycleCalculator()) {
 fun generate(company:Company,cycleStart:LocalDate,policy:CyclePolicy,existing:List<Visit> = emptyList()):List<Visit>{
  val desired=calculator.visits(company.baseDay,company.baseShift,policy).mapIndexed { i,(day,shift)->
   val old=existing.firstOrNull{it.week==i+1 && !it.deleted}
   old ?: Visit(UUID.randomUUID().toString(),company.id,cycleStart,i+1,workingDate(cycleStart,i,day),shift,i)
  }.toMutableList()
  return desired + existing.filter { it.week>policy.visits && !it.deleted }.map{it.copy(deleted=true)}
 }
 private fun workingDate(start:LocalDate,week:Int,day:Int):LocalDate { var d=start.plusDays((week*7).toLong()); var count=0; while(count<day){d=d.plusDays(1);if(d.dayOfWeek.value in 1..5)count++}; return d }
}
