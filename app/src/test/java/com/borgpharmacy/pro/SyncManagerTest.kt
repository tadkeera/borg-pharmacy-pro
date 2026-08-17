package com.borgpharmacy.pro

import com.borgpharmacy.pro.core.database.dao.SyncQueueDao
import com.borgpharmacy.pro.core.database.entity.SyncQueueEntity
import com.borgpharmacy.pro.core.security.SessionSnapshot
import com.borgpharmacy.pro.core.security.SessionStore
import com.borgpharmacy.pro.core.sync.SyncManager
import com.borgpharmacy.pro.core.sync.SyncResult
import com.borgpharmacy.pro.data.remote.ProAuthSession
import com.borgpharmacy.pro.data.remote.ProSyncOperation
import com.borgpharmacy.pro.data.remote.ProSyncOutcome
import com.borgpharmacy.pro.data.remote.ProSyncResponse
import com.borgpharmacy.pro.data.remote.SyncRemoteDataSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncManagerTest {
    @Test
    fun appliesServerOutcomesToQueue() = runBlocking {
        val accepted = queue("accepted")
        val conflict = queue("conflict")
        val unknown = queue("unknown")
        val queueDao = FakeQueueDao(listOf(accepted, conflict, unknown))
        val remote = FakeRemote(
            ProSyncResponse(
                tenantId = "tenant-a",
                accepted = listOf(outcome("accepted")),
                conflicts = listOf(outcome("conflict", "incoming version is not newer")),
            ),
        )
        val manager = SyncManager(queueDao, FakeSessionStore(), remote)

        val result = manager.sync("tenant-a", nowMillis = 2_000_000L)

        assertTrue(result is SyncResult.Succeeded)
        assertEquals(listOf(accepted.id), queueDao.deleted)
        assertEquals("CONFLICT", queueDao.statuses[conflict.id]?.first)
        assertEquals("PENDING", queueDao.statuses[unknown.id]?.first)
    }

    private fun queue(key: String) = SyncQueueEntity(
        id = "id-$key",
        tenantId = "tenant-a",
        entityType = "VISIT",
        entityId = key,
        operation = "UPSERT",
        payload = "{}",
        version = 1,
        idempotencyKey = "tenant-a-$key-123456",
        nextAttemptAt = 0,
    )

    private fun outcome(key: String, reason: String? = null) = ProSyncOutcome(
        idempotencyKey = "tenant-a-$key-123456",
        entityType = "VISIT",
        entityId = key,
        version = 1,
        reason = reason,
    )

    private class FakeSessionStore : SessionStore {
        private var session = SessionSnapshot("access", "refresh", "user-a", "tenant-a", Long.MAX_VALUE)
        override fun save(session: SessionSnapshot) { this.session = session }
        override fun read() = session
        override fun clear() { session = session.copy(accessToken = "", refreshToken = "") }
    }

    private class FakeRemote(private val response: ProSyncResponse) : SyncRemoteDataSource {
        override suspend fun refreshSession(refreshToken: String) = ProAuthSession("access", "refresh", 3600, Long.MAX_VALUE, "user-a", "user@example.com")
        override suspend fun pushSecureOperations(accessToken: String, operations: List<ProSyncOperation>) = response
    }

    private class FakeQueueDao(initial: List<SyncQueueEntity>) : SyncQueueDao {
        private val entries = initial.toMutableList()
        val deleted = mutableListOf<String>()
        val statuses = mutableMapOf<String, Pair<String, String?>>()

        override suspend fun pending(tenant: String, now: Long, limit: Int) = entries.filter { it.tenantId == tenant && it.status == "PENDING" && it.nextAttemptAt <= now }.take(limit)
        override suspend fun enqueue(value: SyncQueueEntity) { entries += value }
        override suspend fun updateStatus(tenant: String, id: String, status: String, attempts: Int, nextAttemptAt: Long, lastError: String?) {
            statuses[id] = status to lastError
        }
        override suspend fun delete(tenant: String, id: String) { deleted += id }
    }
}
