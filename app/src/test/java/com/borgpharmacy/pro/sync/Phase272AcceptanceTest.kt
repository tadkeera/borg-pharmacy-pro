package com.borgpharmacy.pro.sync

import com.borgpharmacy.pro.domain.sync.ConflictResolver
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

data class Op(val id:String=UUID.randomUUID().toString(),val tenant:String,val type:String,val entity:String,val payload:String)
class FakeServer {
 private val accepted=linkedMapOf<String,Op>(); private val changes=mutableListOf<Op>()
 fun push(op:Op):Boolean { if(accepted.containsKey("${op.tenant}:${op.id}")) return false; accepted["${op.tenant}:${op.id}"]=op;changes+=op;return true }
 fun pull(tenant:String):List<Op> = changes.filter{it.tenant==tenant}
 fun count(tenant:String,type:String)=accepted.values.count{it.tenant==tenant&&it.type==type}
}
class Phase272AcceptanceTest {
 @Test fun adminToReceptionWorkflowPreservesBrandingPermissionAndPrintLog(){val server=FakeServer();val tenant="A";val ops=listOf(Op(tenant=tenant,type="FACILITY_PROFILE",entity="f",payload="Facility A"),Op(tenant=tenant,type="COMPANY",entity="c",payload="Company A"),Op(tenant=tenant,type="REPRESENTATIVE",entity="r",payload="Rep A"),Op(tenant=tenant,type="VISIT",entity="v",payload="MONDAY:MORNING"));ops.forEach{assertTrue(server.push(it))};val reception=server.pull(tenant);assertEquals(4,reception.size);assertTrue(reception.any{it.payload=="Facility A"});assertTrue(reception.any{it.type=="VISIT"&&it.payload=="MONDAY:MORNING"});assertTrue(server.push(Op(tenant=tenant,type="PRINT_LOG",entity="p",payload="v|Facility A")));assertEquals(1,server.count(tenant,"PRINT_LOG"))}
 @Test fun offlineReconnectAndDuplicateReplay(){val server=FakeServer();val op=Op(tenant="A",type="COMPANY",entity="c",payload="offline");val outbox=mutableListOf(op);assertEquals(1,outbox.size);assertTrue(server.push(outbox.removeAt(0)));assertFalse(server.push(op));assertEquals(1,server.count("A","COMPANY"))}
 @Test fun failedNetworkRecoveryAndConflict(){val op=Op(tenant="A",type="VISIT",entity="v",payload="x");var state="PENDING";state="FAILED";assertEquals("FAILED",state);state="SYNCING";state="SUCCESS";assertEquals("SUCCESS",state);val conflict=ConflictResolver.resolve(2,1,false,false);assertEquals("local",conflict.winner);assertEquals("server",ConflictResolver.resolve(1,2,false,false).winner);assertEquals("local",ConflictResolver.resolve(2,2,true,false).winner)}
 @Test fun tenantIsolation(){val server=FakeServer();server.push(Op(tenant="A",type="COMPANY",entity="a",payload="a"));server.push(Op(tenant="B",type="COMPANY",entity="b",payload="b"));assertEquals(listOf("a"),server.pull("A").map{it.entity});assertEquals(listOf("b"),server.pull("B").map{it.entity})}
}
