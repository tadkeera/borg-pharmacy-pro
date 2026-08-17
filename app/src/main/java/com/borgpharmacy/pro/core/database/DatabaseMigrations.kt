package com.borgpharmacy.pro.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Non-destructive schema migration for devices that already have version 1.
 * Existing rows retain their business data; new synchronization metadata starts at safe defaults.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE facility_profiles ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE companies ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE companies ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE representatives ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE representatives ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE visits ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE visits ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE users ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE users ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 0")

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sync_queue (
                id TEXT NOT NULL PRIMARY KEY,
                tenantId TEXT NOT NULL,
                entityType TEXT NOT NULL,
                entityId TEXT NOT NULL,
                operation TEXT NOT NULL,
                payload TEXT NOT NULL,
                version INTEGER NOT NULL,
                idempotencyKey TEXT NOT NULL,
                status TEXT NOT NULL,
                attempts INTEGER NOT NULL,
                nextAttemptAt INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                lastError TEXT
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_tenantId_nextAttemptAt ON sync_queue(tenantId, nextAttemptAt)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_tenantId_status_createdAt ON sync_queue(tenantId, status, createdAt)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sync_queue_tenantId_idempotencyKey ON sync_queue(tenantId, idempotencyKey)")

        database.execSQL("CREATE INDEX IF NOT EXISTS index_companies_tenantId_updatedAt ON companies(tenantId, updatedAt)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_representatives_tenantId_companyId ON representatives(tenantId, companyId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_representatives_tenantId_updatedAt ON representatives(tenantId, updatedAt)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_visits_tenantId_date_shift_slotIndex ON visits(tenantId, date, shift, slotIndex)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_visits_tenantId_updatedAt ON visits(tenantId, updatedAt)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_users_tenantId_updatedAt ON users(tenantId, updatedAt)")
    }
}
