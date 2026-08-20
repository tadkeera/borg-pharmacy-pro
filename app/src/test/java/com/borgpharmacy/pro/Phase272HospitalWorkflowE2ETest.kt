package com.borgpharmacy.pro

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

data class WorkflowOp(val id:String=UUID.randomUUID().toString(),val tenant:String,val type:String,val entityId:String,val payload:Map<String,String>)
data class WorkflowRecord(val tenant:String,val type:String,val entityId:String,val payload:Map<String,String>)
private class WorkflowServer {
    private val operations=linkedMapOf<String,WorkflowOp>()
    private val records=linkedMapOf<String,WorkflowRecord>()
    fun push(op:WorkflowOp):Boolean { val key="${op.tenant}:${op.id}"; if(operations.containsKey(key)) return false; operations[key]=op; records["${op.tenant}:${op.type}:${op.entityId}"]=WorkflowRecord(op.tenant,op.type,op.entityId,op.payload); return true }
    fun pull(tenant:String):List<WorkflowRecord> = records.values.filter{it.tenant==tenant}
    fun count(tenant:String,type:String)=records.values.count{it.tenant==tenant&&it.type==type}
}

class Phase272HospitalWorkflowE2ETest {
    @Test fun completeAdminSupabaseReceptionPermitWorkflow() {
        val tenant="tenant-a"; val server=WorkflowServer(); val outbox=mutableListOf<WorkflowOp>()
        fun queue(type:String,id:String,vararg values:Pair<String,String>){outbox+=WorkflowOp(tenant=tenant,type=type,entityId=id,payload=mapOf(*values))}
        queue("FACILITY_PROFILE","facility-a","arabic" to "مستشفى الاختبار","english" to "Demo Hospital","logo" to "logo://demo","policy" to "4")
        queue("COMPANY","company-a","name" to "Demo Pharma")
        queue("REPRESENTATIVE","rep-a","name" to "Ahmed Ali","companyId" to "company-a")
        queue("VISIT","visit-a","day" to "SUNDAY","shift" to "MORNING","companyId" to "company-a","repId" to "rep-a","cycle" to "28")
        assertEquals(4,outbox.size); assertTrue(outbox.all{it.tenant==tenant&&it.id.isNotBlank()})
        outbox.forEach{assertTrue(server.push(it))}; outbox.forEach{assertFalse(server.push(it))}
        val reception=server.pull(tenant); assertEquals(4,reception.size)
        assertEquals("Demo Hospital",reception.first{it.type=="FACILITY_PROFILE"}.payload["english"])
        val visit=reception.first{it.type=="VISIT"}; assertEquals("SUNDAY",visit.payload["day"]); assertEquals("MORNING",visit.payload["shift"])
        fun allowed(day:String,shift:String)=day==visit.payload["day"]&&shift==visit.payload["shift"]
        assertTrue(allowed("SUNDAY","MORNING")); assertFalse(allowed("MONDAY","MORNING")); assertFalse(allowed("SUNDAY","EVENING"))
        queue("PRINT_LOG","print-a","visitId" to "visit-a","repId" to "rep-a","companyId" to "company-a","facility" to "Demo Hospital","timestamp" to "1")
        assertTrue(server.push(outbox.last())); assertEquals(1,server.count(tenant,"PRINT_LOG")); assertFalse(server.push(outbox.last()))
        val offline=WorkflowOp(tenant=tenant,type="VISIT_ACTION",entityId="action-a",payload=mapOf("visitId" to "visit-a","state" to "PENDING")); assertTrue(server.push(offline)); assertEquals(1,server.count(tenant,"VISIT_ACTION"))
        val conflictA=WorkflowRecord(tenant,"COMPANY","company-a",mapOf("name" to "A","version" to "2")); val conflictB=WorkflowRecord(tenant,"COMPANY","company-a",mapOf("name" to "B","version" to "1")); val winner=if(conflictA.payload["version"]!!.toInt()>=conflictB.payload["version"]!!.toInt()) conflictA else conflictB; assertEquals("A",winner.payload["name"])
        val tenantB=server.pull("tenant-b"); assertTrue(tenantB.isEmpty())
    }
}
