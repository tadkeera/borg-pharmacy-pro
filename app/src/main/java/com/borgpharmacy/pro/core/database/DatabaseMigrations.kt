package com.borgpharmacy.pro.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS index_companies_tenantId_name ON companies(tenantId, name)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_representatives_tenantId_companyId ON representatives(tenantId, companyId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_visits_tenantId_cycleStart_date ON visits(tenantId, cycleStart, date)")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("CREATE TABLE IF NOT EXISTS sync_metadata (tenantId TEXT NOT NULL PRIMARY KEY, lastSuccessfulCursor INTEGER NOT NULL, lastSyncAt INTEGER)") } }

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS sync_outbox (operationId TEXT NOT NULL PRIMARY KEY, tenantId TEXT NOT NULL, entityType TEXT NOT NULL, entityId TEXT NOT NULL, operation TEXT NOT NULL, payload TEXT NOT NULL, idempotencyKey TEXT NOT NULL, state TEXT NOT NULL, attempts INTEGER NOT NULL, nextAttemptAt INTEGER NOT NULL, lastError TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sync_outbox_tenantId_idempotencyKey ON sync_outbox(tenantId, idempotencyKey)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_outbox_tenantId_state_nextAttemptAt ON sync_outbox(tenantId, state, nextAttemptAt)")
        db.execSQL("CREATE TABLE IF NOT EXISTS sync_conflicts (conflictId TEXT NOT NULL PRIMARY KEY, tenantId TEXT NOT NULL, operationId TEXT NOT NULL, entityType TEXT NOT NULL, entityId TEXT NOT NULL, localVersion INTEGER NOT NULL, serverVersion INTEGER NOT NULL, localPayload TEXT NOT NULL, serverPayload TEXT NOT NULL, resolutionStatus TEXT NOT NULL, createdAt INTEGER NOT NULL, resolvedAt INTEGER)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sync_conflicts_tenantId_operationId ON sync_conflicts(tenantId, operationId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_conflicts_tenantId ON sync_conflicts(tenantId)")
        db.execSQL("CREATE TABLE IF NOT EXISTS sync_logs (id TEXT NOT NULL PRIMARY KEY, tenantId TEXT NOT NULL, startedAt INTEGER NOT NULL, finishedAt INTEGER, pushed INTEGER NOT NULL, pulled INTEGER NOT NULL, conflicts INTEGER NOT NULL, error TEXT)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_logs_tenantId ON sync_logs(tenantId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_logs_startedAt ON sync_logs(startedAt)")
    }
}
