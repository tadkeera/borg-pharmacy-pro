package com.borgpharmacy.pro.domain.scheduler
import com.borgpharmacy.pro.domain.model.*
class DynamicCycleCalculator {
 fun visits(baseDay:Int,baseShift:Shift,policy:CyclePolicy):List<Pair<Int,Shift>> = (0 until policy.visits).map { week ->
  val shift=when(policy){ CyclePolicy.FOUR -> if(week%2==0) baseShift else if(baseShift==Shift.MORNING) Shift.EVENING else Shift.MORNING; CyclePolicy.THREE -> if(week==0) baseShift else if(baseShift==Shift.MORNING) Shift.EVENING else Shift.MORNING; CyclePolicy.TWO -> if(week==0) baseShift else if(baseShift==Shift.MORNING) Shift.EVENING else Shift.MORNING; CyclePolicy.ONE -> baseShift }
  (baseDay+week)%5 to shift
 }
}
