package com.example.volatiledemandforecasting.data.store

import com.example.volatiledemandforecasting.core.network.apiCall
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.http.GET
import retrofit2.http.Path

@Serializable
data class HistorySummaryDto(
    val storeId: String,
    val historyStart: String? = null,
    val historyEnd: String? = null,
    val productsWithData: Int = 0,
    val daysWithData: Int = 0,
    val records: Int = 0,
    val lastImportAt: String? = null,
)

/** Only the summary is read: the per-product list is large for the demo store and not needed here. */
@Serializable
data class ReadinessDto(
    val asOfDate: String? = null,
    val stale: Boolean = false,
    val productsReady: Int = 0,
    val productsTotal: Int = 0,
)

@Serializable
data class AsOfRangeDto(
    val default: String? = null,
    val editable: Boolean = false,
    val replayMin: String? = null,
    val replayMax: String? = null,
    val researchMin: String? = null,
    val calendarEnd: String? = null,
)

@Serializable
data class BaselineDto(val id: String, val label: String)

@Serializable
data class ModelRefDto(val id: String, val version: String)

@Serializable
data class TrainingStatusDto(
    val runId: String? = null,
    val status: String? = null,
    val outcome: String? = null,
    val finishedAt: String? = null,
)

/** Everything the forecast form and the dashboard's AI status are built from. Never hardcode these. */
@Serializable
data class CapabilitiesDto(
    val storeId: String,
    val isReference: Boolean = false,
    val aiAvailable: Boolean = false,
    /** `NOT_TRAINED`, `TRAINING`, `NOT_ELIGIBLE`, `GATE_FAILED`, `NO_HISTORY` or `MODEL_MISSING`. */
    val aiUnavailableReason: String? = null,
    val methods: List<String> = emptyList(),
    val horizons: List<Int> = emptyList(),
    val frequency: String = "DAILY",
    val asOf: AsOfRangeDto = AsOfRangeDto(),
    val minHistoryDays: Int = 0,
    val maxMissingDaysInLookback: Int = 0,
    val predictionIntervals: Boolean = false,
    val baselines: List<BaselineDto> = emptyList(),
    val activeModel: ModelRefDto? = null,
    val training: TrainingStatusDto? = null,
)

interface StoreApi {
    @GET("v1/stores/{storeId}/history/summary")
    suspend fun historySummary(@Path("storeId") storeId: String): HistorySummaryDto

    @GET("v1/stores/{storeId}/readiness")
    suspend fun readiness(@Path("storeId") storeId: String): ReadinessDto

    @GET("v1/stores/{storeId}/models/capabilities")
    suspend fun capabilities(@Path("storeId") storeId: String): CapabilitiesDto

    @GET("v1/stores/{storeId}/models/active")
    suspend fun activeModel(@Path("storeId") storeId: String): ActiveModelDto
}

class StoreRepository(private val api: StoreApi, private val json: Json) {
    suspend fun historySummary(storeId: String): HistorySummaryDto = apiCall(json) { api.historySummary(storeId) }
    suspend fun readiness(storeId: String): ReadinessDto = apiCall(json) { api.readiness(storeId) }
    suspend fun capabilities(storeId: String): CapabilitiesDto = apiCall(json) { api.capabilities(storeId) }
    suspend fun activeModel(storeId: String): ActiveModelDto = apiCall(json) { api.activeModel(storeId) }
}
