package com.borgpharmacy.pro.domain.sync

import com.borgpharmacy.pro.core.database.entity.SyncState

object SyncPolicy {
    fun nextAttempt(attempt: Int, now: Long = System.currentTimeMillis()): Long = now + (1L shl attempt.coerceIn(0, 6)) * 1_000L
    fun stateForFailure(attempt: Int): SyncState = if (attempt >= 8) SyncState.FAILED else SyncState.PENDING
}

data class ConflictDecision(val state: String, val winner: String)
object ConflictResolver {
    fun resolve(localVersion: Long, serverVersion: Long, localDeleted: Boolean, serverDeleted: Boolean): ConflictDecision = when {
        serverVersion > localVersion -> ConflictDecision("SERVER_WINS", "server")
        localVersion > serverVersion -> ConflictDecision("LOCAL_WINS", "local")
        localDeleted && !serverDeleted -> ConflictDecision("DELETE_WINS", "local")
        serverDeleted && !localDeleted -> ConflictDecision("DELETE_WINS", "server")
        else -> ConflictDecision("TIE_SERVER_WINS", "server")
    }
}
