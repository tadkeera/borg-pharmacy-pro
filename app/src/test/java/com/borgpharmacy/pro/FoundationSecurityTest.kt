package com.borgpharmacy.pro

import com.borgpharmacy.pro.core.security.SessionSnapshot
import com.borgpharmacy.pro.domain.model.Permission
import com.borgpharmacy.pro.domain.model.RolePolicy
import com.borgpharmacy.pro.domain.model.UserRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FoundationSecurityTest {
    @Test
    fun parsesAllSupportedRoles() {
        UserRole.values().forEach { role ->
            assertTrue(UserRole.parse(role.name.lowercase()) == role)
        }
        assertTrue(UserRole.parse("unknown") == null)
    }

    @Test
    fun rolePolicyProtectsAdministrativeOperations() {
        assertTrue(RolePolicy.has(UserRole.OWNER, Permission.MANAGE_TENANT))
        assertFalse(RolePolicy.has(UserRole.ADMIN, Permission.MANAGE_TENANT))
        assertTrue(RolePolicy.has(UserRole.ADMIN, Permission.MANAGE_USERS))
        assertFalse(RolePolicy.has(UserRole.EMPLOYEE, Permission.MANAGE_USERS))
        assertFalse(RolePolicy.has(UserRole.VIEWER, Permission.SYNC_DATA))
    }

    @Test
    fun sessionExpiresWithSafetyWindow() {
        val now = 1_000L
        val valid = snapshot(expiresAt = now + 120)
        val nearExpiry = snapshot(expiresAt = now + 60)
        assertFalse(valid.isExpired(now))
        assertTrue(nearExpiry.isExpired(now))
    }

    private fun snapshot(expiresAt: Long) = SessionSnapshot(
        accessToken = "access",
        refreshToken = "refresh",
        userId = "user-a",
        tenantId = "tenant-a",
        expiresAtEpochSeconds = expiresAt,
    )
}
