package com.borgpharmacy.pro.domain.model

/** Roles are security identifiers; enforcement must also happen in Supabase RLS/Edge Functions. */
enum class UserRole {
    OWNER,
    ADMIN,
    PHARMACIST,
    EMPLOYEE,
    REPRESENTATIVE,
    VIEWER,
    ;

    companion object {
        fun parse(value: String?): UserRole? = value
            ?.trim()
            ?.uppercase()
            ?.let { normalized -> values().firstOrNull { it.name == normalized } }
    }
}

enum class Permission {
    READ_CATALOG,
    WRITE_CATALOG,
    READ_REPORTS,
    MANAGE_USERS,
    MANAGE_TENANT,
    SYNC_DATA,
}

object RolePolicy {
    fun has(role: UserRole, permission: Permission): Boolean = when (permission) {
        Permission.READ_CATALOG -> true
        Permission.READ_REPORTS -> true
        Permission.WRITE_CATALOG -> role == UserRole.OWNER || role == UserRole.ADMIN || role == UserRole.PHARMACIST
        Permission.MANAGE_USERS -> role == UserRole.OWNER || role == UserRole.ADMIN
        Permission.MANAGE_TENANT -> role == UserRole.OWNER
        Permission.SYNC_DATA -> role != UserRole.VIEWER
    }
}
