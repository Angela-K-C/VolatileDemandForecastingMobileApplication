package com.example.volatiledemandforecasting.data.auth

import kotlinx.serialization.Serializable
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable
data class LoginBody(val email: String, val password: String)

@Serializable
data class RegisterBody(val email: String, val password: String, val displayName: String)

@Serializable
data class RefreshBody(val refreshToken: String)

@Serializable
data class PasswordResetRequestBody(val email: String)

@Serializable
data class PasswordResetConfirmBody(val email: String, val code: String, val newPassword: String)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
    val tokenType: String = "Bearer",
)

/**
 * The endpoints that need no access token, so this runs on the public client. Logout is here too:
 * it only needs the refresh token in its body, and sending a stale access token with it could
 * trigger a refresh that rotates that very token.
 */
interface AuthApi {
    @POST("v1/auth/login")
    suspend fun login(@Body body: LoginBody): TokenResponse

    @POST("v1/auth/register")
    suspend fun register(@Body body: RegisterBody): TokenResponse

    /** A blocking call, because the OkHttp `Authenticator` that uses it is not a coroutine. */
    @POST("v1/auth/refresh")
    fun refresh(@Body body: RefreshBody): Call<TokenResponse>

    @POST("v1/auth/logout")
    suspend fun logout(@Body body: RefreshBody)

    @POST("v1/auth/password-reset/request")
    suspend fun requestPasswordReset(@Body body: PasswordResetRequestBody)

    @POST("v1/auth/password-reset/confirm")
    suspend fun confirmPasswordReset(@Body body: PasswordResetConfirmBody): TokenResponse
}
