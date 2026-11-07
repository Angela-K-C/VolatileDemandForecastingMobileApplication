package com.example.volatiledemandforecasting.core.network

import com.example.volatiledemandforecasting.core.security.SessionManager
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object Network {

    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private fun baseClient() = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        // Imports are validated and merged within the request (api.md), so allow time for 10 MB files.
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .addInterceptor(RequestIdInterceptor())

    /** For sign-in, registration, refresh, logout and password reset: no token is attached. */
    fun publicClient(): OkHttpClient = baseClient().build()

    /** For everything else: attaches the access token and refreshes it on `401 UNAUTHORIZED`. */
    fun authedClient(session: SessionManager, refresher: TokenRefresher): OkHttpClient = baseClient()
        .addInterceptor(AuthInterceptor(session))
        .authenticator(TokenAuthenticator(session, refresher, json))
        .build()

    fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json; charset=UTF-8".toMediaType()))
        .build()
}
