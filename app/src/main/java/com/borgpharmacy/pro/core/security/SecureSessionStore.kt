package com.borgpharmacy.pro.core.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Stores only authentication session material; business data remains in Room. */
class SecureSessionStore(context: Context) {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun save(session: SessionSnapshot) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_TENANT_ID, session.tenantId)
            .putLong(KEY_EXPIRES_AT, session.expiresAtEpochSeconds)
            .apply()
    }

    fun read(): SessionSnapshot? {
        val access = preferences.getString(KEY_ACCESS_TOKEN, null)?.takeIf { it.isNotBlank() } ?: return null
        val refresh = preferences.getString(KEY_REFRESH_TOKEN, null)?.takeIf { it.isNotBlank() } ?: return null
        val userId = preferences.getString(KEY_USER_ID, null)?.takeIf { it.isNotBlank() } ?: return null
        val tenantId = preferences.getString(KEY_TENANT_ID, null)?.takeIf { it.isNotBlank() } ?: return null
        return SessionSnapshot(
            accessToken = access,
            refreshToken = refresh,
            userId = userId,
            tenantId = tenantId,
            expiresAtEpochSeconds = preferences.getLong(KEY_EXPIRES_AT, 0L),
        )
    }

    fun clear() = preferences.edit().clear().apply()

    companion object {
        private const val FILE_NAME = "borg_secure_session"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_TENANT_ID = "tenant_id"
        private const val KEY_EXPIRES_AT = "expires_at_epoch_seconds"
    }
}

data class SessionSnapshot(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val tenantId: String,
    val expiresAtEpochSeconds: Long,
) {
    fun isExpired(nowEpochSeconds: Long = System.currentTimeMillis() / 1000): Boolean =
        expiresAtEpochSeconds <= nowEpochSeconds + 60
}
