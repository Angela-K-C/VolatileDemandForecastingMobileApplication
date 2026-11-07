package com.example.volatiledemandforecasting.data.auth

import com.example.volatiledemandforecasting.core.network.apiCall
import com.example.volatiledemandforecasting.core.security.SessionManager
import com.example.volatiledemandforecasting.core.security.Tokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/** Every function throws `ApiException` for a backend error and `IOException` when offline. */
class AuthRepository(
    private val api: AuthApi,
    private val session: SessionManager,
    private val json: Json,
    /** Outlives any screen, so the server-side sign-out still happens after the user navigates away. */
    private val appScope: CoroutineScope,
) {
    suspend fun login(email: String, password: String) {
        val tokens = apiCall(json) { api.login(LoginBody(email.trim(), password)) }
        session.signIn(tokens.toTokens())
    }

    suspend fun register(email: String, password: String, displayName: String) {
        val tokens = apiCall(json) { api.register(RegisterBody(email.trim(), password, displayName.trim())) }
        session.signIn(tokens.toTokens())
    }

    /** Always succeeds from the user's side: the backend never says whether the account exists. */
    suspend fun requestPasswordReset(email: String) {
        apiCall(json) { api.requestPasswordReset(PasswordResetRequestBody(email.trim())) }
    }

    /** Signs the user in with the new password, and the backend ends their other sessions. */
    suspend fun confirmPasswordReset(email: String, code: String, newPassword: String) {
        val tokens = apiCall(json) { api.confirmPasswordReset(PasswordResetConfirmBody(email.trim(), code.trim(), newPassword)) }
        session.signIn(tokens.toTokens())
    }

    /**
     * Ends the session on this device straight away, then tells the server. If the server can't be
     * reached the refresh token just expires there; the user is signed out here either way.
     */
    fun logout() {
        val refreshToken = session.tokens?.refreshToken
        session.signOut()
        if (refreshToken != null) {
            appScope.launch { runCatching { apiCall(json) { api.logout(RefreshBody(refreshToken)) } } }
        }
    }

    private fun TokenResponse.toTokens() = Tokens(accessToken, refreshToken)
}
