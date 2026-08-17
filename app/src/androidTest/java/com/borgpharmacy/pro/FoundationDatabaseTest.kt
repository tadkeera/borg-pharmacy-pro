package com.borgpharmacy.pro

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.borgpharmacy.pro.core.database.BorgProDatabase
import com.borgpharmacy.pro.core.database.entity.CompanyEntity
import com.borgpharmacy.pro.core.database.entity.SyncQueueEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoundationDatabaseTest {
    private lateinit var database: BorgProDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, BorgProDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun tenantQueriesCannotReadAnotherTenant() = runBlocking {
        database.companyDao().upsertAll(
            listOf(
                CompanyEntity(tenantId = "tenant-a", name = "A", baseDay = 1, baseShift = "MORNING"),
                CompanyEntity(tenantId = "tenant-b", name = "B", baseDay = 1, baseShift = "MORNING"),
            ),
        )

        val tenantA = database.companyDao().page("tenant-a", limit = 100, offset = 0)
        val tenantB = database.companyDao().page("tenant-b", limit = 100, offset = 0)

        assertEquals(listOf("A"), tenantA.map { it.name })
        assertEquals(listOf("B"), tenantB.map { it.name })
        assertTrue(tenantA.none { it.tenantId == "tenant-b" })
    }

    @Test
    fun queueIdempotencyIsScopedToTenant() = runBlocking {
        database.syncQueueDao().enqueue(
            SyncQueueEntity(
                tenantId = "tenant-a",
                entityType = "VISIT",
                entityId = "visit-a",
                operation = "UPSERT",
                payload = "{}",
                version = 1,
                idempotencyKey = "same-key-123456",
            ),
        )
        database.syncQueueDao().enqueue(
            SyncQueueEntity(
                tenantId = "tenant-b",
                entityType = "VISIT",
                entityId = "visit-b",
                operation = "UPSERT",
                payload = "{}",
                version = 1,
                idempotencyKey = "same-key-123456",
            ),
        )

        assertEquals(1, database.syncQueueDao().pending("tenant-a", Long.MAX_VALUE, 10).size)
        assertEquals(1, database.syncQueueDao().pending("tenant-b", Long.MAX_VALUE, 10).size)
    }

    @Test
    fun databaseUsesVersionTwoSchema() {
        assertEquals(2, database.openHelper.readableDatabase.version)
    }
}
