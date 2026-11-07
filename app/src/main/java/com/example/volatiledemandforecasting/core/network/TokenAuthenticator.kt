package com.example.volatiledemandforecasting.core.network

import com.example.volatiledemandforecasting.core.security.SessionManager
import com.example.volatiledemandforecasting.core.security.SignOutReason
import com.example.volatiledemandforecasting.core.security.Tokens
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

sealed interface RefreshResult {
    data class Success(val tokens: Tokens) : RefreshResult

    /** The server said the refresh token is no good: the user has to sign in again. */
    data object Rejected : RefreshResult

    /** The server was unreachable or failed. The session may still be fine, so nothing is cleared. */
    data object Unavailable : RefreshResult
}

/** Blocking, because OkHttp calls the authenticator on its own worker thread. */
fun interface TokenRefresher {
    fun refresh(refreshToken: String): RefreshResult
}

/**
 * Refreshes the access token when a request comes back `401 UNAUTHORIZED` (api.md, "Authentication").
 *
 * The backend rotates refresh tokens and treats a reused one as theft, ending every session. So all
 * refreshes go through one lock: when several requests fail together, the first refreshes and the
 * rest see the new token and simply retry with it. Both tokens are replaced every time.
 */
class TokenAuthenticator(
    private val session: SessionManager,
    private val refresher: TokenRefresher,
    private val json: Json,
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.code != 401 || response.attempts() >= 2) return null
        if (errorCode(response) != ApiErrorCodes.UNAUTHORIZED) return null

        val usedToken = response.request.header("Authorization")?.removePrefix("Bearer ") ?: return null

        synchronized(lock) {
            val current = session.tokens ?: return null

            // Another request already refreshed while this one waited for the lock.
            if (current.accessToken != usedToken) return response.request.withToken(current.accessToken)

            return when (val result = refresher.refresh(current.refreshToken)) {
                is RefreshResult.Success -> {
                    session.replaceTokens(result.tokens)
                    response.request.withToken(result.tokens.accessToken)
                }
                RefreshResult.Rejected -> {
                    session.signOut(SignOutReason.SessionExpired)
                    null
                }
                RefreshResult.Unavailable -> null
            }
        }
    }

    private fun errorCode(response: Response): String? =
        parseErrorBody(json, response.peekBody(PEEK_BYTES).string())?.code

    private fun Response.attempts(): Int = generateSequence(priorResponse) { it.priorResponse }.count() + 1

    private fun Request.withToken(token: String): Request =
        newBuilder().header("Authorization", "Bearer $token").build()

    private companion object {
        const val PEEK_BYTES = 8192L
    }
}
