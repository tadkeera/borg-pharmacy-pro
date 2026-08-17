package com.borgpharmacy.pro.domain.sync

enum class ConflictPolicy {
    LAST_WRITE_WINS,
    REJECT_STALE,
    MANUAL_REVIEW,
}

data class VersionedChange<T>(
    val value: T,
    val version: Long,
)

sealed interface ConflictDecision<out T> {
    data class Apply<T>(val change: VersionedChange<T>) : ConflictDecision<T>
    data class KeepCurrent<T>(val current: VersionedChange<T>, val reason: String) : ConflictDecision<T>
    data class Manual<T>(val incoming: VersionedChange<T>, val current: VersionedChange<T>) : ConflictDecision<T>
}

object ConflictResolver {
    fun <T> resolve(
        current: VersionedChange<T>?,
        incoming: VersionedChange<T>,
        policy: ConflictPolicy = ConflictPolicy.LAST_WRITE_WINS,
    ): ConflictDecision<T> {
        if (current == null || incoming.version > current.version) return ConflictDecision.Apply(incoming)
        return when (policy) {
            ConflictPolicy.LAST_WRITE_WINS,
            ConflictPolicy.REJECT_STALE,
            -> ConflictDecision.KeepCurrent(current, "incoming version is not newer")
            ConflictPolicy.MANUAL_REVIEW -> ConflictDecision.Manual(incoming, current)
        }
    }
}
