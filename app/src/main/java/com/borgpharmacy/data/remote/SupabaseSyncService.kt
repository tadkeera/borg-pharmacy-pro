package com.borgpharmacy.data.remote

import com.borgpharmacy.BuildConfig
import com.borgpharmacy.data.local.CompanyEntity
import com.borgpharmacy.data.local.DEFAULT_TENANT_ID
import com.borgpharmacy.data.local.RepresentativeEntity
import com.borgpharmacy.data.local.UserEntity
import com.borgpharmacy.data.local.VisitEntity
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
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Offline-first sync adapter.
 *
 * Sensitive writes are disabled here; use an authenticated Edge Function instead.
 * Pulls are explicitly filtered by tenant_id + active flags to prevent stale/deleted cloud rows
 * from corrupting the local Room database after a catalog replacement.
 */
class SupabaseSyncService {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    suspend fun signInWithPassword(email: String, password: String): SupabaseAuthSession = withContext(Dispatchers.IO) {
        val response = postAuth(
            path = "token?grant_type=password",
            body = buildJsonObject {
                put("email", email.trim().lowercase())
                put("password", password)
            }
        )
        json.decodeFromString<AuthTokenResponseDto>(response).toDomain()
    }

    suspend fun refreshSession(refreshToken: String): SupabaseAuthSession = withContext(Dispatchers.IO) {
        require(refreshToken.isNotBlank()) { "refresh token is required" }
        val response = postAuth(
            path = "token?grant_type=refresh_token",
            body = buildJsonObject { put("refresh_token", refreshToken) },
        )
        json.decodeFromString<AuthTokenResponseDto>(response).toDomain()
    }

    suspend fun fetchProfile(accessToken: String, userId: String): UserProfileRemoteDto? = withContext(Dispatchers.IO) {
        val encodedUserId = enc(userId)
        val response = getRest(
            path = "user_profiles?select=*&user_id=eq.$encodedUserId&limit=1",
            bearer = accessToken,
        )
        json.decodeFromString<List<UserProfileRemoteDto>>(response).firstOrNull()
    }

    /**
     * The client must never write through a shared secret embedded in the APK.
     * These legacy methods remain as compile-time migration guards and fail closed.
     */
    @Deprecated("Use an authenticated Edge Function with tenant and role checks")
    suspend fun pushCompanies(companies: List<CompanyEntity>) = failClosed("pushCompanies")

    @Deprecated("Use an authenticated Edge Function with tenant and role checks")
    suspend fun pushRepresentatives(representatives: List<RepresentativeEntity>) = failClosed("pushRepresentatives")

    @Deprecated("Use an authenticated Edge Function with tenant and role checks")
    suspend fun pushVisits(visits: List<VisitEntity>) = failClosed("pushVisits")

    @Deprecated("Use an authenticated Edge Function with tenant and role checks")
    suspend fun pushUsers(users: List<UserEntity>) = failClosed("pushUsers")

    @Deprecated("Use Supabase Auth sign-in and fetchProfile")
    suspend fun loginUser(username: String, passcodeHash: String): UserEntity? = failClosed("loginUser")

    private suspend fun failClosed(operation: String): Nothing = throw SecurityException(
        "Legacy unauthenticated RPC disabled: $operation"
    )

    suspend fun pushSecureOperations(
        accessToken: String,
        operations: List<SecureSyncOperationDto>,
    ): SecureSyncResponseDto = withContext(Dispatchers.IO) {
        require(accessToken.isNotBlank()) { "access token is required" }
        require(operations.isNotEmpty() && operations.size <= 100) { "sync batch must contain 1..100 operations" }
        val response = postFunction(
            functionName = "secure-sync",
            accessToken = accessToken,
            body = buildJsonObject { put("operations", json.encodeToJsonElement(operations)) },
        )
        json.decodeFromString<SecureSyncResponseDto>(response)
    }

    suspend fun adminCreateAuthUser(
        accessToken: String,
        email: String,
        password: String,
        displayName: String,
        role: String,
    ): CreatedAuthUserDto = withContext(Dispatchers.IO) {
        val response = postFunction(
            functionName = "admin-create-user",
            accessToken = accessToken,
            body = buildJsonObject {
                put("email", email.trim().lowercase())
                put("password", password)
                put("displayName", displayName.trim())
                put("role", if (role == "ADMIN") "ADMIN" else "PHARMACIST")
            }
        )
        json.decodeFromString<CreatedAuthUserDto>(response)
    }

    suspend fun pullAll(tenantId: String = DEFAULT_TENANT_ID): RemoteSnapshot = withContext(Dispatchers.IO) {
        val tenant = enc(tenantId)
        val activeFilter = "tenant_id=eq.$tenant&is_deleted=eq.false&deleted_at=is.null"
        val companies = json.decodeFromString<List<CompanyRemoteDto>>(
            getRest("companies?select=*&$activeFilter&order=updated_at.asc")
        ).map { it.toEntity() }
        val activeCompanyIds = companies.map { it.id }.toSet()
        val reps = if (activeCompanyIds.isEmpty()) {
            emptyList()
        } else {
            // مهم: صفحة الويب القديمة ربما سجلت المندوب tenant_id فارغ/قديم.
            // نسحب المندوبين النشطين ثم نقبل فقط من company_id تابع لشركات هذا الـ tenant ونطبع tenantId محلياً.
            json.decodeFromString<List<RepresentativeRemoteDto>>(
                getRest("representatives?select=*&$activeFilter&order=updated_at.asc")
            ).map { it.toEntity() }
                .filter { it.companyId in activeCompanyIds }
                .map { if (it.tenantId == tenantId) it else it.copy(tenantId = tenantId) }
        }
        val visits = json.decodeFromString<List<VisitRemoteDto>>(
            getRest("visits?select=*&$activeFilter&order=updated_at.asc")
        ).map { it.toEntity() }
        val users = runCatching { pullUsers(tenantId) }.getOrDefault(emptyList())
        RemoteSnapshot(companies, reps, visits, users)
    }

    @Deprecated("Use an authenticated Edge Function with server-side tenant checks")
    suspend fun pruneTenantToActiveCompanies(tenantId: String, activeCompanyIds: List<String>) = failClosed("pruneTenantToActiveCompanies")

    @Deprecated("Use an authenticated Edge Function with server-side tenant checks")
    suspend fun repairRepresentativeCompanyLinks(tenantId: String) = failClosed("repairRepresentativeCompanyLinks")

    @Deprecated("Use an authenticated Edge Function with server-side tenant checks")
    suspend fun moveRepresentative(repId: String, targetCompanyId: String) = failClosed("moveRepresentative")

    @Deprecated("Hard delete is disabled from the Android client")
    suspend fun hardDeleteRepresentative(repId: String) = failClosed("hardDeleteRepresentative")

    @Deprecated("Hard delete is disabled from the Android client")
    suspend fun hardDeleteCompany(companyId: String) = failClosed("hardDeleteCompany")

    private suspend fun pullUsers(tenantId: String): List<UserEntity> = failClosed("pullUsers")

    private suspend fun getRest(path: String, bearer: String = BuildConfig.SUPABASE_ANON_KEY): String = withContext(Dispatchers.IO) {
        val connection = (URL("${BuildConfig.SUPABASE_URL}/rest/v1/$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 30_000
            readTimeout = 30_000
            setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer $bearer")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        readResponse(connection, "REST $path")
    }

    private suspend fun postAuth(path: String, body: JsonObject): String = withContext(Dispatchers.IO) {
        val connection = (URL("${BuildConfig.SUPABASE_URL}/auth/v1/$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        writeJson(connection, body)
        readResponse(connection, "Auth $path")
    }

    private suspend fun postFunction(functionName: String, accessToken: String, body: JsonObject): String = withContext(Dispatchers.IO) {
        val connection = (URL("${BuildConfig.SUPABASE_URL}/functions/v1/$functionName").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        writeJson(connection, body)
        readResponse(connection, "Function $functionName")
    }

    private fun writeJson(connection: HttpURLConnection, body: JsonObject) {
        val bytes = json.encodeToString(JsonObject.serializer(), body).toByteArray(Charsets.UTF_8)
        connection.outputStream.use { it.write(bytes) }
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

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
}

@Serializable
data class SecureSyncOperationDto(
    @SerialName("idempotencyKey") val idempotencyKey: String,
    val operation: String,
    val entityType: String,
    val entityId: String,
    val payload: JsonObject,
    val version: Long,
)

@Serializable
data class SecureSyncResponseDto(
    val tenantId: String,
    val accepted: List<SecureSyncAcceptedDto> = emptyList(),
)

@Serializable
data class SecureSyncAcceptedDto(
    val idempotencyKey: String,
    val entityType: String,
    val entityId: String,
    val version: Long,
)

data class SupabaseAuthSession(
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
data class AuthTokenResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long = 0,
    val user: AuthUserDto,
) {
    fun toDomain(): SupabaseAuthSession {
        val now = System.currentTimeMillis() / 1000
        return SupabaseAuthSession(
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
data class AuthUserDto(val id: String, val email: String? = null)

@Serializable
data class UserProfileRemoteDto(
    @SerialName("user_id") val userId: String,
    @SerialName("tenant_id") val tenantId: String,
    @SerialName("display_name") val displayName: String = "",
    val role: String = "PHARMACIST",
    val active: Boolean = true,
    @SerialName("must_change_password") val mustChangePassword: Boolean = false,
)

@Serializable
data class CreatedAuthUserDto(val id: String, val email: String, val displayName: String, val role: String, val tenantId: String)

data class RemoteSnapshot(
    val companies: List<CompanyEntity>,
    val representatives: List<RepresentativeEntity>,
    val visits: List<VisitEntity>,
    val users: List<UserEntity>,
)

@Serializable
data class CompanyRemoteDto(
    val id: String,
    @SerialName("tenant_id") val tenantId: String = DEFAULT_TENANT_ID,
    val name: String,
    val tier: String,
    @SerialName("basedayindex") val baseDayIndex: Int? = null,
    @SerialName("baseshift") val baseShift: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("deleted_at") val deletedAt: Long?,
    @SerialName("sync_status") val syncStatus: String = "SYNCED",
    @SerialName("is_deleted") val isDeleted: Boolean = deletedAt != null,
)

@Serializable
data class RepresentativeRemoteDto(
    val id: String,
    @SerialName("tenant_id") val tenantId: String = DEFAULT_TENANT_ID,
    @SerialName("company_id") val companyId: String,
    val name: String,
    val phone: String,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("deleted_at") val deletedAt: Long?,
    @SerialName("sync_status") val syncStatus: String = "SYNCED",
    @SerialName("is_deleted") val isDeleted: Boolean = deletedAt != null,
)

@Serializable
data class VisitRemoteDto(
    val id: String,
    @SerialName("tenant_id") val tenantId: String = DEFAULT_TENANT_ID,
    @SerialName("company_id") val companyId: String,
    @SerialName("cycle_start_epoch_day") val cycleStartEpochDay: Long,
    @SerialName("day_of_cycle") val dayOfCycle: Int,
    @SerialName("week_of_cycle") val weekOfCycle: Int,
    @SerialName("date_epoch_day") val dateEpochDay: Long,
    val shift: String,
    @SerialName("slot_index") val slotIndex: Int,
    val status: String,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("deleted_at") val deletedAt: Long?,
    @SerialName("sync_status") val syncStatus: String = "SYNCED",
    @SerialName("is_deleted") val isDeleted: Boolean = deletedAt != null,
)

@Serializable
data class UserRemoteDto(
    val id: String,
    @SerialName("tenant_id") val tenantId: String = DEFAULT_TENANT_ID,
    val username: String,
    @SerialName("display_name") val displayName: String,
    val role: String,
    @SerialName("passcode_hash") val passcodeHash: String,
    @SerialName("must_change_passcode") val mustChangePasscode: Boolean = false,
    val active: Boolean = true,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("sync_status") val syncStatus: String = "SYNCED",
    @SerialName("is_deleted") val isDeleted: Boolean = !active,
)

private fun CompanyEntity.toRemote() = CompanyRemoteDto(id, tenantId, name, tier, baseDayIndex, baseShift, createdAt, updatedAt, deletedAt, syncStatus, isDeleted)
private fun RepresentativeEntity.toRemote() = RepresentativeRemoteDto(id, tenantId, companyId, name, phone, createdAt, updatedAt, deletedAt, syncStatus, isDeleted)
private fun VisitEntity.toRemote() = VisitRemoteDto(id, tenantId, companyId, cycleStartEpochDay, dayOfCycle, weekOfCycle, dateEpochDay, shift, slotIndex, status, createdAt, updatedAt, deletedAt, syncStatus, isDeleted)
private fun UserEntity.toRemote() = UserRemoteDto(id, tenantId, username, displayName, role, passcodeHash, mustChangePasscode, active, createdAt, updatedAt, syncStatus, isDeleted)

private fun CompanyRemoteDto.toEntity() = CompanyEntity(id = id, tenantId = tenantId, name = name, tier = tier, baseDayIndex = baseDayIndex, baseShift = baseShift, createdAt = createdAt, updatedAt = updatedAt, deletedAt = deletedAt, dirty = false, syncStatus = syncStatus, isDeleted = isDeleted || deletedAt != null)
private fun RepresentativeRemoteDto.toEntity() = RepresentativeEntity(id = id, tenantId = tenantId, companyId = companyId, name = name, phone = phone, createdAt = createdAt, updatedAt = updatedAt, deletedAt = deletedAt, dirty = false, syncStatus = syncStatus, isDeleted = isDeleted || deletedAt != null)
private fun VisitRemoteDto.toEntity() = VisitEntity(id = id, tenantId = tenantId, companyId = companyId, cycleStartEpochDay = cycleStartEpochDay, dayOfCycle = dayOfCycle, weekOfCycle = weekOfCycle, dateEpochDay = dateEpochDay, shift = shift, slotIndex = slotIndex, status = status, createdAt = createdAt, updatedAt = updatedAt, deletedAt = deletedAt, dirty = false, syncStatus = syncStatus, isDeleted = isDeleted || deletedAt != null)
private fun UserRemoteDto.toEntity() = UserEntity(id = id, tenantId = tenantId, username = username, displayName = displayName, role = role, passcodeHash = passcodeHash, mustChangePasscode = mustChangePasscode, active = active, createdAt = createdAt, updatedAt = updatedAt, syncStatus = syncStatus, isDeleted = isDeleted)
