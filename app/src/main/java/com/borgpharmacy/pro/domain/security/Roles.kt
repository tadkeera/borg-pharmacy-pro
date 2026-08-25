package com.borgpharmacy.pro.domain.security

enum class AppRole { SUPER_ADMIN, FACILITY_ADMIN, RECEPTIONIST, VIEWER }
object PermissionPolicy {
    fun canManageUsers(role:AppRole)=role==AppRole.SUPER_ADMIN||role==AppRole.FACILITY_ADMIN
    fun canEditSchedule(role:AppRole)=role==AppRole.SUPER_ADMIN||role==AppRole.FACILITY_ADMIN
    fun canPrint(role:AppRole)=role==AppRole.SUPER_ADMIN||role==AppRole.FACILITY_ADMIN||role==AppRole.RECEPTIONIST
    fun canView(role:AppRole)=true
}
