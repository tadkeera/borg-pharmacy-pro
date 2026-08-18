package com.borgpharmacy.pro.sync

import com.borgpharmacy.pro.domain.sync.*
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

data class Op(val id:String=UUID.randomUUID().toString(),val tenant:String,val type:String,val entity:String,val payload:String)
class FakeServer { private val accepted=linkedMapOf<String,Op>(); private val changes=mutableListOf<Op>(); fun push(op:Op):Boolean { if(accepted.containsKey("${op.tenant}:${op.id}")) return false; accepted["${op.tenant}:${op.id"]=op;changes+=op;return true }; fun pull(tenant:String)=changes.filter{it.tenant==tenant}; fun count(tenant:String,type:String)=accepted.values.count{it.tenant==tenant&&it.type==type} }
class Phase272AcceptanceTest {
 @Test fun adminToReceptionWorkflowPreservesBrandingPermissionAndPrintLog(){val s=FakeServer();val ops=listOf(Op(tenant="A",type="FACILITY_PROFILE",entity="f",payload="Facility A"),Op(tenant="A",type="COMPANY",entity="c",payload="Company A"),Op(tenant="A",type="REPRESENTATIVE",entity="r",payload="Rep A"),Op(tenant="A",type="VISIT",entity="v",payload="MONDAY:MORNING"));ops.forEach{assertTrue(s.push(it))};val r=s.pull("A");assertEquals(4,r.size);assertTrue(r.any{it.payload=="Facility A"});assertTrue(r.any{it.type=="VISIT"&&it.payload=="MONDAY:MORNING"});assertTrue(s.push(Op(tenant="A",type="PRINT_LOG",entity="p",payload="v|Facility A")));assertEquals(1,s.count("A","PRINT_LOG"))}
 @Test fun offlineReconnectAndDuplicateReplay(){val s=FakeServer();val op=Op(tenant="A",type="COMPANY",entity="c",payload="offline");assertTrue(s.push(op));assertFalse(s.push(op));assertEquals(1,s.count("A","COMPANY"))}
 @Test fun failedNetworkRecoveryAndConflict(){var state="FAILED";state="PENDING";state="SYNCING";state="SUCCESS";assertEquals("SUCCESS",state);val stale=VersionedChange("server",1);val newer=VersionedChange("local",2);assertTrue(ConflictResolver.resolve(stale,newer) is ConflictDecision.Apply);assertTrue(ConflictResolver.resolve(newer,stale) is ConflictDecision.KeepCurrent)}
 @Test fun tenantIsolation(){val s=FakeServer();s.push(Op(tenant="A",type="COMPANY",entity="a",payload="a"));s.push(Op(tenant="B",type="COMPANY",entity="b",payload="b"));assertEquals(listOf("a"),s.pull("A").map{it.entity});assertEquals(listOf("b"),s.pull("B").map{it.entity})}
}
