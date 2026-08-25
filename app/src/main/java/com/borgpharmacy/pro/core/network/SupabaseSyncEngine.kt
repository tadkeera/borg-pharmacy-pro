package com.borgpharmacy.pro.core.network

class SupabaseSyncEngine {
    suspend fun sync() {
        // Sync orchestration is intentionally kept outside the UI; local Room remains authoritative offline.
    }
}
