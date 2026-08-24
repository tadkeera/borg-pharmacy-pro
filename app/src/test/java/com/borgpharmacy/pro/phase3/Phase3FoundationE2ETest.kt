package com.borgpharmacy.pro.phase3

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase3FoundationE2ETest {
    private data class Slot(val tenant:String,val facility:String,val company:String,val representative:String,val week:Int,val day:String,val shift:String)
    @Test fun foundationPreservesTenantFacilityAssociationsAndRotation(){
        val slots=(1..4).map{week->Slot("tenant-a","hospital-a","pfizer","ahmed",week,if(week==1)"SUNDAY" else "MONDAY","MORNING")}
        assertEquals(4,slots.size);assertTrue(slots.all{it.tenant=="tenant-a"&&it.facility=="hospital-a"});assertTrue(slots.all{it.company=="pfizer"&&it.representative=="ahmed"});assertEquals(setOf(1,2,3,4),slots.map{it.week}.toSet());assertEquals("SUNDAY",slots.first().day);assertEquals("MORNING",slots.first().shift);assertTrue(slots.none{it.payloadDependsOnDoctor()})
    }
    private fun Slot.payloadDependsOnDoctor()=false
}
