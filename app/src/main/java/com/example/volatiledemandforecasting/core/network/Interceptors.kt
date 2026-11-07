package com.example.volatiledemandforecasting.core.network

import com.example.volatiledemandforecasting.core.security.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID

/** Every response echoes this ID, and so do the server logs (api.md). It carries no user data. */
class RequestIdInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.header("X-Request-Id") != null) return chain.proceed(request)
        return chain.proceed(request.newBuilder().header("X-Request-Id", "app_${UUID.randomUUID()}").build())
    }
}

class AuthInterceptor(private val session: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val tokens = session.tokens
        if (tokens == null || request.header("Authorization") != null) return chain.proceed(request)
        return chain.proceed(request.newBuilder().header("Authorization", "Bearer ${tokens.accessToken}").build())
    }
}
