package com.borgpharmacy.pro.core.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Stores only Supabase session material encrypted by an Android Keystore master key. */
class SecureSessionStore(context: Context) {
    private val masterKey = MasterKey.Builder(context.applicationContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    private val preferences = EncryptedSharedPreferences.create(
        context.applicationContext,
        "supabase_secure_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    fun save(accessToken: String, refreshToken: String, expiresIn: Long, tokenType: String) = preferences.edit()
        .putString(KEY_ACCESS, accessToken).putString(KEY_REFRESH, refreshToken)
        .putLong(KEY_EXPIRES, expiresIn).putString(KEY_TYPE, tokenType).apply()
    fun read(): StoredSession? {
        val access = preferences.getString(KEY_ACCESS, null) ?: return null
        val refresh = preferences.getString(KEY_REFRESH, null) ?: return null
        return StoredSession(access, refresh, preferences.getLong(KEY_EXPIRES, 0L), preferences.getString(KEY_TYPE, "Bearer") ?: "Bearer")
    }
    fun clear() = preferences.edit().clear().apply()
    data class StoredSession(val accessToken: String, val refreshToken: String, val expiresIn: Long, val tokenType: String)
    private companion object { const val KEY_ACCESS="access_token"; const val KEY_REFRESH="refresh_token"; const val KEY_EXPIRES="expires_in"; const val KEY_TYPE="token_type" }
}
