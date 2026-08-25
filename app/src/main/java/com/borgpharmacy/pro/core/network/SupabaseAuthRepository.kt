package com.borgpharmacy.pro.core.network

import android.content.Context
import com.borgpharmacy.pro.core.security.SecureSessionStore
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.Email
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SupabaseAuthRepository(context: Context) {
    private val store = SecureSessionStore(context)
    val sessionStatus: Flow<AuthState> = SupabaseClientProvider.client.auth.sessionStatus.map { status ->
        when (status) {
            is io.github.jan.supabase.auth.status.SessionStatus.Authenticated -> AuthState.Authenticated
            is io.github.jan.supabase.auth.status.SessionStatus.RefreshFailure -> AuthState.RefreshFailed
            is io.github.jan.supabase.auth.status.SessionStatus.Initializing -> AuthState.Initializing
            is io.github.jan.supabase.auth.status.SessionStatus.NotAuthenticated -> AuthState.SignedOut
        }
    }
    suspend fun restoreSession() {
        store.read()?.let { saved ->
            SupabaseClientProvider.client.auth.importSession(
                UserSession(saved.accessToken, saved.refreshToken, saved.expiresIn, saved.tokenType, null)
            )
        }
    }
    suspend fun login(email: String, password: String) {
        SupabaseClientProvider.client.auth.signInWith(Email) { this.email = email.trim(); this.password = password }
        persistCurrentSession()
    }
    suspend fun refresh() {
        SupabaseClientProvider.client.auth.refreshCurrentSession()
        persistCurrentSession()
    }
    suspend fun logout() {
        runCatching { SupabaseClientProvider.client.auth.signOut() }
        store.clear()
    }
    private fun persistCurrentSession() {
        SupabaseClientProvider.client.auth.currentSessionOrNull()?.let { session ->
            store.save(session.accessToken, session.refreshToken, session.expiresIn, session.tokenType)
        }
    }
    sealed interface AuthState { data object Initializing:AuthState; data object Authenticated:AuthState; data object SignedOut:AuthState; data object RefreshFailed:AuthState }
}
