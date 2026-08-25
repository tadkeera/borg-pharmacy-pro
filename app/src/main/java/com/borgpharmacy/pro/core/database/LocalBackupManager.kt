package com.borgpharmacy.pro.core.database

import android.content.Context
import com.borgpharmacy.pro.core.audit.AuditLogRepository
import java.io.File

class LocalBackupManager(private val context: Context, private val audit: AuditLogRepository? = null) {
    suspend fun backupAndAudit(tenant: String, actor: String, role: String): File {
        val file = backup()
        audit?.record(tenant, actor, role, "backup_created", "DATABASE", file.name, file.length().toString())
        return file
    }
    suspend fun restoreAndAudit(file: File, tenant: String, actor: String, role: String) {
        restore(file)
        audit?.record(tenant, actor, role, "backup_restored", "DATABASE", file.name, file.length().toString())
    }
    fun backup(): File {
        val source = context.getDatabasePath(BorgProDatabase.DATABASE_NAME)
        require(source.exists())
        val target = File(context.filesDir, "backups/${System.currentTimeMillis()}.db")
        target.parentFile?.mkdirs(); source.copyTo(target, true); return target
    }
    fun verify(file: File): Boolean = file.exists() && file.length() > 0
    fun restore(file: File) { require(verify(file)); val target=context.getDatabasePath(BorgProDatabase.DATABASE_NAME); target.parentFile?.mkdirs(); file.copyTo(target,true) }
}
