package com.borgpharmacy.pro

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.borgpharmacy.pro.core.database.MIGRATION_1_2
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomMigrationTest {
    private lateinit var context: Context
    private lateinit var helper: SupportSQLiteOpenHelper

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(DB_NAME)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE facility_profiles (id TEXT NOT NULL PRIMARY KEY, tenantId TEXT NOT NULL, arabicName TEXT NOT NULL, englishName TEXT NOT NULL, logoPath TEXT, policy INTEGER NOT NULL, adminUsername TEXT NOT NULL, adminPasswordHash TEXT NOT NULL)")
                    db.execSQL("CREATE TABLE companies (id TEXT NOT NULL PRIMARY KEY, tenantId TEXT NOT NULL, name TEXT NOT NULL, baseDay INTEGER NOT NULL, baseShift TEXT NOT NULL, isDeleted INTEGER NOT NULL DEFAULT 0)")
                    db.execSQL("CREATE TABLE representatives (id TEXT NOT NULL PRIMARY KEY, tenantId TEXT NOT NULL, companyId TEXT NOT NULL, name TEXT NOT NULL, phone TEXT NOT NULL, isDeleted INTEGER NOT NULL DEFAULT 0)")
                    db.execSQL("CREATE TABLE visits (id TEXT NOT NULL PRIMARY KEY, tenantId TEXT NOT NULL, companyId TEXT NOT NULL, cycleStart INTEGER NOT NULL, week INTEGER NOT NULL, date INTEGER NOT NULL, shift TEXT NOT NULL, slotIndex INTEGER NOT NULL, isDeleted INTEGER NOT NULL DEFAULT 0)")
                    db.execSQL("CREATE TABLE users (id TEXT NOT NULL PRIMARY KEY, tenantId TEXT NOT NULL, username TEXT NOT NULL, passwordHash TEXT NOT NULL, salt TEXT NOT NULL, isAdmin INTEGER NOT NULL DEFAULT 1)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
    }

    @After
    fun tearDown() {
        helper.close()
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun migrationPreservesRowsAndCreatesQueue() {
        val db = helper.writableDatabase
        db.execSQL("INSERT INTO companies(id, tenantId, name, baseDay, baseShift, isDeleted) VALUES ('company-a', 'tenant-a', 'Alpha', 0, 'MORNING', 0)")

        MIGRATION_1_2.migrate(db)

        val cursor = db.query("SELECT name, tenantId, syncVersion FROM companies WHERE id = 'company-a'")
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals("Alpha", it.getString(0))
            assertEquals("tenant-a", it.getString(1))
            assertEquals(0L, it.getLong(2))
        }
        val queueTables = db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'sync_queue'")
        queueTables.use { assertTrue(it.moveToFirst()) }
    }

    companion object {
        private const val DB_NAME = "phase2-migration-test.db"
    }
}
