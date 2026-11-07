package com.example.volatiledemandforecasting.data.training

import com.example.volatiledemandforecasting.core.network.apiCall
import com.example.volatiledemandforecasting.data.forecast.ApiErrorDto
import com.example.volatiledemandforecasting.data.store.BacktestDto
import com.example.volatiledemandforecasting.data.store.HorizonAccuracyDto
import com.example.volatiledemandforecasting.data.store.ModelRefDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

object TrainingStatus {
    const val QUEUED = "QUEUED"
    const val TRAINING = "TRAINING"
    const val COMPLETED = "COMPLETED"
    const val FAILED = "FAILED"

    fun isFinished(status: String) = status == COMPLETED || status == FAILED
}

object TrainingOutcome {
    const val TRAINED = "TRAINED"
    const val GATE_FAILED = "GATE_FAILED"
    const val NOT_ELIGIBLE = "NOT_ELIGIBLE"
}

@Serializable
data class EligibilityCheckDto(val value: Double? = null, val required: Double? = null, val passed: Boolean = false)

@Serializable
data class GateDto(val passed: Boolean = false, val rule: String? = null)

@Serializable
data class TrainingRunDto(
    val id: String,
    val workspaceId: String? = null,
    val status: String,
    /** `TRAINED`, `GATE_FAILED` or `NOT_ELIGIBLE` once the run has finished. */
    val outcome: String? = null,
    val trigger: String? = null,
    val createdAt: String? = null,
    val finishedAt: String? = null,
    val historyRows: Int? = null,
    val model: ModelRefDto? = null,
    val horizons: List<Int>? = null,
    val backtest: BacktestDto? = null,
    val gate: GateDto? = null,
    val horizonAccuracy: HorizonAccuracyDto? = null,
    val error: ApiErrorDto? = null,
    val retryable: Boolean = false,
)

@Serializable
data class EligibilityDto(
    val workspaceId: String? = null,
    val eligible: Boolean = false,
    val storesInModel: List<String> = emptyList(),
    /** `productsWithEnoughHistory`, `storeHistoryDays` and `observedProductDays`. The thresholds come from here. */
    val checks: Map<String, EligibilityCheckDto> = emptyMap(),
    val latestRun: TrainingRunDto? = null,
)

interface TrainingApi {
    @GET("v1/stores/{storeId}/models/eligibility")
    suspend fun eligibility(@Path("storeId") storeId: String): EligibilityDto

    @POST("v1/stores/{storeId}/models/train")
    suspend fun train(@Path("storeId") storeId: String): TrainingRunDto

    @GET("v1/training-runs/{runId}")
    suspend fun run(@Path("runId") runId: String): TrainingRunDto

    @POST("v1/training-runs/{runId}/retry")
    suspend fun retry(@Path("runId") runId: String): TrainingRunDto
}

class TrainingRepository(private val api: TrainingApi, private val json: Json) {
    suspend fun eligibility(storeId: String): EligibilityDto = apiCall(json) { api.eligibility(storeId) }
    suspend fun train(storeId: String): TrainingRunDto = apiCall(json) { api.train(storeId) }
    suspend fun run(runId: String): TrainingRunDto = apiCall(json) { api.run(runId) }
    suspend fun retry(runId: String): TrainingRunDto = apiCall(json) { api.retry(runId) }
}
