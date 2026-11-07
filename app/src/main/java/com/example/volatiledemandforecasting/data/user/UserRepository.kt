package com.example.volatiledemandforecasting.data.user

import com.example.volatiledemandforecasting.core.network.apiCall
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.http.GET

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

interface MeApi {
    @GET("v1/me")
    suspend fun me(): MeDto
}

class UserRepository(private val api: MeApi, private val json: Json) {
    suspend fun me(): MeDto = apiCall(json) { api.me() }
}
