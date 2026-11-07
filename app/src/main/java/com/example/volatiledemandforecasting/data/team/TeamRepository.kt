package com.example.volatiledemandforecasting.data.team

import com.example.volatiledemandforecasting.core.network.apiCall
import com.example.volatiledemandforecasting.core.security.SessionManager
import com.example.volatiledemandforecasting.core.security.Tokens
import com.example.volatiledemandforecasting.data.auth.TokenResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable
data class MemberDto(
    val userId: String,
    val email: String,
    val displayName: String,
    val role: String,
    val joinedAt: String? = null,
)

@Serializable
data class InvitationDto(
    val id: String,
    val workspaceId: String,
    val workspaceName: String,
    val email: String,
    val role: String,
    /** `PENDING`, `ACCEPTED`, `DECLINED`, `REVOKED` or `EXPIRED`. */
    val status: String = "PENDING",
    val invitedBy: String? = null,
    val createdAt: String? = null,
    val expiresAt: String? = null,
)

@Serializable
data class InviteRequest(val email: String, val role: String)

@Serializable
data class RoleRequest(val role: String)

@Serializable
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

/** Roles an owner can give. The demo's read-only role is never assignable. */
object AssignableRoles {
    val all = listOf("OWNER", "MANAGER", "VIEWER")
}

interface TeamApi {
    @GET("v1/workspaces/{workspaceId}/members")
    suspend fun members(@Path("workspaceId") workspaceId: String): List<MemberDto>

    @PATCH("v1/workspaces/{workspaceId}/members/{userId}")
    suspend fun changeRole(@Path("workspaceId") workspaceId: String, @Path("userId") userId: String, @Body request: RoleRequest): MemberDto

    @DELETE("v1/workspaces/{workspaceId}/members/{userId}")
    suspend fun removeMember(@Path("workspaceId") workspaceId: String, @Path("userId") userId: String)

    @GET("v1/workspaces/{workspaceId}/invitations")
    suspend fun pendingInvitations(@Path("workspaceId") workspaceId: String): List<InvitationDto>

    @POST("v1/workspaces/{workspaceId}/invitations")
    suspend fun invite(@Path("workspaceId") workspaceId: String, @Body request: InviteRequest): InvitationDto

    @DELETE("v1/workspaces/{workspaceId}/invitations/{invitationId}")
    suspend fun revoke(@Path("workspaceId") workspaceId: String, @Path("invitationId") invitationId: String)

    @GET("v1/me/invitations")
    suspend fun myInvitations(): List<InvitationDto>

    @POST("v1/invitations/{invitationId}/accept")
    suspend fun accept(@Path("invitationId") invitationId: String): InvitationDto

    @POST("v1/invitations/{invitationId}/decline")
    suspend fun decline(@Path("invitationId") invitationId: String): InvitationDto

    @POST("v1/auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): TokenResponse
}

class TeamRepository(private val api: TeamApi, private val session: SessionManager, private val json: Json) {
    suspend fun members(workspaceId: String): List<MemberDto> = apiCall(json) { api.members(workspaceId) }
    suspend fun changeRole(workspaceId: String, userId: String, role: String): MemberDto =
        apiCall(json) { api.changeRole(workspaceId, userId, RoleRequest(role)) }
    suspend fun removeMember(workspaceId: String, userId: String) = apiCall(json) { api.removeMember(workspaceId, userId) }
    suspend fun pendingInvitations(workspaceId: String): List<InvitationDto> = apiCall(json) { api.pendingInvitations(workspaceId) }
    suspend fun invite(workspaceId: String, email: String, role: String): InvitationDto =
        apiCall(json) { api.invite(workspaceId, InviteRequest(email.trim(), role)) }
    suspend fun revoke(workspaceId: String, invitationId: String) = apiCall(json) { api.revoke(workspaceId, invitationId) }

    /** Invitations to the signed-in user's email. Only `PENDING` ones can be answered. */
    suspend fun myInvitations(): List<InvitationDto> = apiCall(json) { api.myInvitations() }
    suspend fun accept(invitationId: String): InvitationDto = apiCall(json) { api.accept(invitationId) }
    suspend fun decline(invitationId: String): InvitationDto = apiCall(json) { api.decline(invitationId) }

    /** The backend signs this user out everywhere else and returns new tokens, which replace the old ones here. */
    suspend fun changePassword(current: String, new: String) {
        val tokens = apiCall(json) { api.changePassword(ChangePasswordRequest(current, new)) }
        session.replaceTokens(Tokens(tokens.accessToken, tokens.refreshToken))
    }
}
