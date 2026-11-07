package com.example.volatiledemandforecasting.app

import android.content.Context
import com.example.volatiledemandforecasting.BuildConfig
import com.example.volatiledemandforecasting.core.datastore.AppPreferences
import com.example.volatiledemandforecasting.core.network.Network
import com.example.volatiledemandforecasting.core.security.KeystoreTokenStore
import com.example.volatiledemandforecasting.core.security.SessionManager
import com.example.volatiledemandforecasting.core.security.SessionState
import com.example.volatiledemandforecasting.data.auth.AuthApi
import com.example.volatiledemandforecasting.data.auth.AuthRepository
import com.example.volatiledemandforecasting.data.auth.RetrofitTokenRefresher
import com.example.volatiledemandforecasting.data.catalog.ProductApi
import com.example.volatiledemandforecasting.data.catalog.ProductRepository
import com.example.volatiledemandforecasting.data.forecast.ForecastApi
import com.example.volatiledemandforecasting.data.forecast.ForecastRepository
import com.example.volatiledemandforecasting.data.store.StoreApi
import com.example.volatiledemandforecasting.data.store.StoreRepository
import com.example.volatiledemandforecasting.data.user.MeApi
import com.example.volatiledemandforecasting.data.user.UserRepository
import com.example.volatiledemandforecasting.data.workspace.ActiveStore
import com.example.volatiledemandforecasting.data.workspace.WorkspaceApi
import com.example.volatiledemandforecasting.data.workspace.WorkspaceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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

    val preferences = AppPreferences(context)
    val activeStore = ActiveStore(preferences, applicationScope)

    val authRepository = AuthRepository(authApi, session, json, applicationScope)
    val userRepository = UserRepository(authedRetrofit.create<MeApi>(), json)
    val workspaceRepository = WorkspaceRepository(authedRetrofit.create<WorkspaceApi>(), json)
    val productRepository = ProductRepository(authedRetrofit.create<ProductApi>(), json)
    val storeRepository = StoreRepository(authedRetrofit.create<StoreApi>(), json)
    val forecastRepository = ForecastRepository(authedRetrofit.create<ForecastApi>(), json)

    init {
        // Sign-out clears what belongs to the user (spec §14): the chosen workspace and store.
        applicationScope.launch {
            session.state.collect { if (it is SessionState.SignedOut) activeStore.clear() }
        }
    }
}
