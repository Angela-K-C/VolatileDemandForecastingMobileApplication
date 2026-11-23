package com.example.volatiledemandforecasting.data.user

import com.example.volatiledemandforecasting.core.network.apiCall
import com.example.volatiledemandforecasting.core.security.SessionManager
import com.example.volatiledemandforecasting.core.security.Tokens
import com.example.volatiledemandforecasting.data.auth.TokenResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

@Serializable
data class WorkspaceAccessDto(
    val workspaceId: String,
    val name: String,
    /** `OWNER`, `MANAGER`, `VIEWER` or `REFERENCE` (the read-only demo workspace). */
    val role: String,
    val isReference: Boolean = false,
)

@Serializable
data class MeDto(
    val id: String,
    val email: String,
    val displayName: String,
    /** Unlocks research dates, model-version choice and the research endpoints. */
    val isResearch: Boolean,
    val workspaces: List<WorkspaceAccessDto> = emptyList(),
)

@Serializable
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

interface MeApi {
    @GET("v1/me")
    suspend fun me(): MeDto

    @POST("v1/auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): TokenResponse
}

class UserRepository(private val api: MeApi, private val session: SessionManager, private val json: Json) {
    suspend fun me(): MeDto = apiCall(json) { api.me() }

    /** The backend signs this user out everywhere else and returns new tokens, which replace the old ones here. */
    suspend fun changePassword(current: String, new: String) {
        val tokens = apiCall(json) { api.changePassword(ChangePasswordRequest(current, new)) }
        session.replaceTokens(Tokens(tokens.accessToken, tokens.refreshToken))
    }
}
