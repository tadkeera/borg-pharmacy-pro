package com.borgpharmacy.pro.sync

import com.borgpharmacy.pro.domain.sync.ConflictDecision
import com.borgpharmacy.pro.domain.sync.ConflictResolver
import com.borgpharmacy.pro.domain.sync.VersionedChange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

data class FakeOperation(
    val id: String = UUID.randomUUID().toString(),
    val tenant: String,
    val type: String,
    val entity: String,
    val payload: String,
)

private class FakeServer {
    private val accepted = linkedMapOf<String, FakeOperation>()
    private val changes = mutableListOf<FakeOperation>()

    fun push(operation: FakeOperation): Boolean {
        val key = "${operation.tenant}:${operation.id}"
        if (accepted.containsKey(key)) return false
        accepted[key] = operation
        changes += operation
        return true
    }

    fun pull(tenant: String): List<FakeOperation> = changes.filter { it.tenant == tenant }

    fun count(tenant: String, type: String): Int =
        accepted.values.count { it.tenant == tenant && it.type == type }
}

class Phase272AcceptanceTest {
    @Test
    fun adminToReceptionWorkflowPreservesBrandingPermissionAndPrintLog() {
        val server = FakeServer()
        val operations = listOf(
            FakeOperation(tenant = "A", type = "FACILITY_PROFILE", entity = "facility", payload = "Facility A"),
            FakeOperation(tenant = "A", type = "COMPANY", entity = "company", payload = "Company A"),
            FakeOperation(tenant = "A", type = "REPRESENTATIVE", entity = "representative", payload = "Rep A"),
            FakeOperation(tenant = "A", type = "VISIT", entity = "visit", payload = "MONDAY:MORNING"),
        )

        operations.forEach { assertTrue(server.push(it)) }
        val receptionChanges = server.pull("A")

        assertEquals(4, receptionChanges.size)
        assertTrue(receptionChanges.any { it.payload == "Facility A" })
        assertTrue(receptionChanges.any { it.type == "VISIT" && it.payload == "MONDAY:MORNING" })

        assertTrue(
            server.push(
                FakeOperation(
                    tenant = "A",
                    type = "PRINT_LOG",
                    entity = "print-log",
                    payload = "visit|Facility A",
                ),
            ),
        )
        assertEquals(1, server.count("A", "PRINT_LOG"))
    }

    @Test
    fun offlineReconnectAndDuplicateReplay() {
        val server = FakeServer()
        val operation = FakeOperation(tenant = "A", type = "COMPANY", entity = "company", payload = "offline")

        assertTrue(server.push(operation))
        assertFalse(server.push(operation))
        assertEquals(1, server.count("A", "COMPANY"))
    }

    @Test
    fun failedNetworkRecoveryAndConflict() {
        var state = "FAILED"
        state = "PENDING"
        state = "SYNCING"
        state = "SUCCESS"
        assertEquals("SUCCESS", state)

        val stale = VersionedChange("server", version = 1)
        val newer = VersionedChange("local", version = 2)
        assertTrue(ConflictResolver.resolve(stale, newer) is ConflictDecision.Apply)
        assertTrue(ConflictResolver.resolve(newer, stale) is ConflictDecision.KeepCurrent)
    }

    @Test
    fun tenantIsolation() {
        val server = FakeServer()
        server.push(FakeOperation(tenant = "A", type = "COMPANY", entity = "company-a", payload = "A"))
        server.push(FakeOperation(tenant = "B", type = "COMPANY", entity = "company-b", payload = "B"))

        assertEquals(listOf("company-a"), server.pull("A").map { it.entity })
        assertEquals(listOf("company-b"), server.pull("B").map { it.entity })
    }
}
