package com.example.volatiledemandforecasting.app

import android.content.Context
import com.example.volatiledemandforecasting.BuildConfig
import com.example.volatiledemandforecasting.core.network.Network
import com.example.volatiledemandforecasting.core.security.KeystoreTokenStore
import com.example.volatiledemandforecasting.core.security.SessionManager
import com.example.volatiledemandforecasting.data.auth.AuthApi
import com.example.volatiledemandforecasting.data.auth.AuthRepository
import com.example.volatiledemandforecasting.data.auth.RetrofitTokenRefresher
import com.example.volatiledemandforecasting.data.user.MeApi
import com.example.volatiledemandforecasting.data.user.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import retrofit2.create

/** Hand-wired dependencies, created once by [VolatileApp]. Screens never build their own clients. */
class AppContainer(context: Context) {

    private val json = Network.json
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val session = SessionManager(KeystoreTokenStore(context))

    // Public client: no token attached, so it can also do the refresh itself.
    private val publicRetrofit = Network.retrofit(BuildConfig.API_BASE_URL, Network.publicClient())
    private val authApi: AuthApi = publicRetrofit.create()

    private val authedRetrofit = Network.retrofit(
        BuildConfig.API_BASE_URL,
        Network.authedClient(session, RetrofitTokenRefresher(authApi)),
    )

    val authRepository = AuthRepository(authApi, session, json, applicationScope)
    val userRepository = UserRepository(authedRetrofit.create<MeApi>(), json)
}
