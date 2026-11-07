package com.example.volatiledemandforecasting.data.auth

import com.example.volatiledemandforecasting.core.network.RefreshResult
import com.example.volatiledemandforecasting.core.network.TokenRefresher
import com.example.volatiledemandforecasting.core.security.Tokens
import java.io.IOException

class RetrofitTokenRefresher(private val api: AuthApi) : TokenRefresher {
    override fun refresh(refreshToken: String): RefreshResult = try {
        val response = api.refresh(RefreshBody(refreshToken)).execute()
        val body = response.body()
        when {
            response.isSuccessful && body != null -> RefreshResult.Success(Tokens(body.accessToken, body.refreshToken))
            // 401 INVALID_REFRESH_TOKEN. Anything else (429, 5xx) is the server's problem, not the session's.
            response.code() == 401 -> RefreshResult.Rejected
            else -> RefreshResult.Unavailable
        }
    } catch (e: IOException) {
        RefreshResult.Unavailable
    }
}
