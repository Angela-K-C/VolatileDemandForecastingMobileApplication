package com.example.volatiledemandforecasting.app

import android.content.Context
import androidx.room.Room
import com.example.volatiledemandforecasting.data.cache.CacheDatabase
import com.example.volatiledemandforecasting.data.cache.ResponseCache
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
import com.example.volatiledemandforecasting.data.forecast.BatchApi
import com.example.volatiledemandforecasting.data.forecast.BatchRepository
import com.example.volatiledemandforecasting.data.forecast.ForecastApi
import com.example.volatiledemandforecasting.data.forecast.ForecastRepository
import com.example.volatiledemandforecasting.data.imports.ImportApi
import com.example.volatiledemandforecasting.data.imports.ImportRepository
import com.example.volatiledemandforecasting.data.store.StoreApi
import com.example.volatiledemandforecasting.data.store.StoreRepository
import com.example.volatiledemandforecasting.data.team.TeamApi
import com.example.volatiledemandforecasting.data.team.TeamRepository
import com.example.volatiledemandforecasting.data.training.TrainingApi
import com.example.volatiledemandforecasting.data.training.TrainingRepository
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
private const val THIRTY_DAYS = 30L * 24 * 60 * 60 * 1000

class AppContainer(context: Context) {

    private val json = Network.json
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val session = SessionManager(KeystoreTokenStore(context))

    private val cacheDatabase = Room.databaseBuilder(context, CacheDatabase::class.java, "response-cache.db")
        // It only holds copies of server data, so a schema change can start over.
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
    val responseCache = ResponseCache(cacheDatabase.dao(), json)

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
    val productRepository = ProductRepository(authedRetrofit.create<ProductApi>(), json, responseCache)
    val storeRepository = StoreRepository(authedRetrofit.create<StoreApi>(), json)
    val forecastRepository = ForecastRepository(authedRetrofit.create<ForecastApi>(), json, responseCache)
    val teamRepository = TeamRepository(authedRetrofit.create<TeamApi>(), session, json)
    val batchRepository = BatchRepository(authedRetrofit.create<BatchApi>(), json, responseCache)
    val importRepository = ImportRepository(authedRetrofit.create<ImportApi>(), json, responseCache)
    val trainingRepository = TrainingRepository(authedRetrofit.create<TrainingApi>(), json)

    init {
        applicationScope.launch { runCatching { responseCache.pruneOlderThan(THIRTY_DAYS) } }
        // Sign-out clears what belongs to the user (spec §14): the chosen workspace and store.
        applicationScope.launch {
            session.state.collect { if (it is SessionState.SignedOut) { activeStore.clear(); responseCache.clear() } }
        }
    }
}
