package com.borgpharmacy.pro.data.remote

import com.borgpharmacy.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

/** Contract consumed by SyncManager; production implementation uses Supabase Edge Functions. */
interface SyncRemoteDataSource {
    suspend fun refreshSession(refreshToken: String): ProAuthSession
    suspend fun pushSecureOperations(accessToken: String, operations: List<ProSyncOperation>): ProSyncResponse
}

/**
 * Authenticated remote datasource for the consolidated pro architecture.
 * It intentionally exposes no legacy shared-token RPC methods.
 */
class SupabaseRemoteDataSource(
    private val json: Json = Json { encodeDefaults = true; ignoreUnknownKeys = true },
) : SyncRemoteDataSource {
    suspend fun signInWithPassword(email: String, password: String): ProAuthSession = withContext(Dispatchers.IO) {
        val response = postAuth(
            path = "token?grant_type=password",
            body = buildJsonObject {
                put("email", email.trim().lowercase())
                put("password", password)
            },
        )
        json.decodeFromString<AuthTokenResponse>(response).toDomain()
    }

    override suspend fun refreshSession(refreshToken: String): ProAuthSession = withContext(Dispatchers.IO) {
        require(refreshToken.isNotBlank()) { "refresh token is required" }
        val response = postAuth(
            path = "token?grant_type=refresh_token",
            body = buildJsonObject { put("refresh_token", refreshToken) },
        )
        json.decodeFromString<AuthTokenResponse>(response).toDomain()
    }

    suspend fun fetchProfile(accessToken: String, userId: String): ProUserProfile? = withContext(Dispatchers.IO) {
        val response = getRest(
            path = "user_profiles?select=*&user_id=eq.${enc(userId)}&limit=1",
            bearer = accessToken,
        )
        json.decodeFromString<List<ProUserProfile>>(response).firstOrNull()
    }

    override suspend fun pushSecureOperations(
        accessToken: String,
        operations: List<ProSyncOperation>,
    ): ProSyncResponse = withContext(Dispatchers.IO) {
        require(accessToken.isNotBlank()) { "access token is required" }
        require(operations.isNotEmpty() && operations.size <= 100) { "sync batch must contain 1..100 operations" }
        val response = postFunction(
            functionName = "secure-sync",
            accessToken = accessToken,
            body = buildJsonObject { put("operations", json.encodeToJsonElement(operations)) },
        )
        json.decodeFromString(response)
    }

    private suspend fun getRest(path: String, bearer: String): String = withContext(Dispatchers.IO) {
        val connection = (URL("${BuildConfig.SUPABASE_URL}/rest/v1/$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer $bearer")
            setRequestProperty("Content-Type", JSON_CONTENT_TYPE)
        }
        readResponse(connection, "REST $path")
    }

    private suspend fun postAuth(path: String, body: JsonObject): String = withContext(Dispatchers.IO) {
        val connection = (URL("${BuildConfig.SUPABASE_URL}/auth/v1/$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            doOutput = true
            setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Content-Type", JSON_CONTENT_TYPE)
        }
        writeJson(connection, body)
        readResponse(connection, "Auth $path")
    }

    private suspend fun postFunction(functionName: String, accessToken: String, body: JsonObject): String = withContext(Dispatchers.IO) {
        val connection = (URL("${BuildConfig.SUPABASE_URL}/functions/v1/$functionName").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            doOutput = true
            setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", JSON_CONTENT_TYPE)
        }
        writeJson(connection, body)
        readResponse(connection, "Function $functionName")
    }

    private fun writeJson(connection: HttpURLConnection, body: JsonObject) {
        connection.outputStream.use { output ->
            output.write(json.encodeToString(JsonObject.serializer(), body).toByteArray(Charsets.UTF_8))
        }
    }

    private fun readResponse(connection: HttpURLConnection, label: String): String {
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IllegalStateException("Supabase $label failed with HTTP $code: $response")
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun enc(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    companion object {
        private const val TIMEOUT_MILLIS = 30_000
        private const val JSON_CONTENT_TYPE = "application/json; charset=utf-8"
    }
}

@Serializable
data class ProSyncOperation(
    @SerialName("idempotencyKey") val idempotencyKey: String,
    val operation: String,
    val entityType: String,
    val entityId: String,
    val payload: JsonObject,
    val version: Long,
)

@Serializable
data class ProSyncResponse(
    val tenantId: String,
    val accepted: List<ProSyncOutcome> = emptyList(),
    val conflicts: List<ProSyncOutcome> = emptyList(),
    val rejected: List<ProSyncOutcome> = emptyList(),
)

@Serializable
data class ProSyncOutcome(
    val idempotencyKey: String,
    val entityType: String,
    val entityId: String,
    val version: Long,
    val reason: String? = null,
)

@Serializable
data class ProUserProfile(
    @SerialName("user_id") val userId: String,
    @SerialName("tenant_id") val tenantId: String,
    @SerialName("display_name") val displayName: String = "",
    val role: String = "VIEWER",
    val active: Boolean = true,
    @SerialName("must_change_password") val mustChangePassword: Boolean = false,
)

data class ProAuthSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val expiresAtEpochSeconds: Long,
    val userId: String,
    val email: String,
) {
    fun isExpired(nowEpochSeconds: Long = System.currentTimeMillis() / 1000): Boolean =
        expiresAtEpochSeconds <= nowEpochSeconds + 60
}

@Serializable
data class AuthTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long = 0,
    val user: AuthUser,
) {
    fun toDomain(): ProAuthSession {
        val now = System.currentTimeMillis() / 1000
        return ProAuthSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = expiresIn,
            expiresAtEpochSeconds = now + expiresIn,
            userId = user.id,
            email = user.email.orEmpty(),
        )
    }
}

@Serializable
data class AuthUser(val id: String, val email: String? = null)
