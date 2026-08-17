package com.borgpharmacy.pro.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "facility_profiles")
data class FacilityProfileEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tenantId: String = id,
    val arabicName: String,
    val englishName: String,
    val logoPath: String?,
    val policy: Int,
    val adminUsername: String = "admin",
    val adminPasswordHash: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = 0L,
)

@Entity(tableName = "companies", indices = [
    Index("tenantId"),
    Index(value = ["tenantId", "name"]),
    Index(value = ["tenantId", "updatedAt"]),
])
data class CompanyEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tenantId: String,
    val name: String,
    val baseDay: Int,
    val baseShift: String,
    val isDeleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = 0L,
)

@Entity(tableName = "representatives", indices = [
    Index(value = ["tenantId", "companyId"]),
    Index(value = ["tenantId", "name"]),
    Index(value = ["tenantId", "updatedAt"]),
])
data class RepresentativeEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tenantId: String,
    val companyId: String,
    val name: String,
    val phone: String,
    val isDeleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = 0L,
)

@Entity(tableName = "visits", indices = [
    Index(value = ["tenantId", "cycleStart"]),
    Index(value = ["tenantId", "date", "shift", "slotIndex"]),
    Index(value = ["tenantId", "updatedAt"]),
])
data class VisitEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tenantId: String,
    val companyId: String,
    val cycleStart: Long,
    val week: Int,
    val date: Long,
    val shift: String,
    val slotIndex: Int,
    val isDeleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = 0L,
)

@Entity(tableName = "print_logs", indices = [
    Index(value = ["tenantId", "visitId"]),
    Index(value = ["tenantId", "printedAt"]),
])
data class PrintLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tenantId: String,
    val visitId: String,
    val printedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "users", indices = [
    Index(value = ["tenantId", "username"], unique = true),
    Index(value = ["tenantId", "updatedAt"]),
])
data class UserEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tenantId: String,
    val username: String,
    val passwordHash: String,
    val salt: String,
    val isAdmin: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = 0L,
)

@Entity(tableName = "sync_queue", indices = [
    Index(value = ["tenantId", "nextAttemptAt"]),
    Index(value = ["tenantId", "status", "createdAt"]),
    Index(value = ["tenantId", "idempotencyKey"], unique = true),
])
data class SyncQueueEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tenantId: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payload: String,
    val version: Long,
    val idempotencyKey: String,
    val status: String = "PENDING",
    val attempts: Int = 0,
    val nextAttemptAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastError: String? = null,
)
