package com.borgpharmacy.pro

import com.borgpharmacy.pro.domain.sync.ConflictDecision
import com.borgpharmacy.pro.domain.sync.ConflictPolicy
import com.borgpharmacy.pro.domain.sync.ConflictResolver
import com.borgpharmacy.pro.domain.sync.VersionedChange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConflictResolutionTest {
    @Test
    fun newerVersionIsApplied() {
        val decision = ConflictResolver.resolve(
            current = VersionedChange("old", 10),
            incoming = VersionedChange("new", 11),
        )
        assertEquals("new", (decision as ConflictDecision.Apply<String>).change.value)
    }

    @Test
    fun staleVersionIsKeptUnderLastWritePolicy() {
        val decision = ConflictResolver.resolve(
            current = VersionedChange("current", 11),
            incoming = VersionedChange("stale", 10),
            policy = ConflictPolicy.LAST_WRITE_WINS,
        )
        assertEquals("current", (decision as ConflictDecision.KeepCurrent<String>).current.value)
    }

    @Test
    fun manualPolicySurfacesBothVersions() {
        val decision = ConflictResolver.resolve(
            current = VersionedChange("current", 11),
            incoming = VersionedChange("incoming", 11),
            policy = ConflictPolicy.MANUAL_REVIEW,
        )
        val manual = decision as ConflictDecision.Manual<String>
        assertEquals("incoming", manual.incoming.value)
        assertEquals("current", manual.current.value)
    }
}
