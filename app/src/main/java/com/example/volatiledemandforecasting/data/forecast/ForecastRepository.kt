package com.example.volatiledemandforecasting.data.forecast

import com.example.volatiledemandforecasting.core.network.apiCall
import com.example.volatiledemandforecasting.data.catalog.DemandClassDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import com.example.volatiledemandforecasting.data.cache.ResponseCache
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import java.io.OutputStream

object ForecastMethod {
    const val AI = "AI"
    const val BASELINE = "BASELINE"
    const val COMPARE = "COMPARE"
}

object JobStatus {
    const val QUEUED = "QUEUED"
    const val PREPARING_DATA = "PREPARING_DATA"
    const val RUNNING_MODEL = "RUNNING_MODEL"
    const val FINALIZING = "FINALIZING"
    const val COMPLETED = "COMPLETED"
    const val FAILED = "FAILED"

    fun isFinished(status: String) = status == COMPLETED || status == FAILED
}

@Serializable
data class ForecastRequest(
    val storeId: String,
    val productId: String,
    val horizonDays: Int,
    val method: String,
    /** Omitted, the backend uses the latest day of history. */
    val asOfDate: String? = null,
)

@Serializable
data class ApiErrorDto(val code: String, val message: String = "")

@Serializable
data class JobDto(
    val jobId: String,
    val forecastId: String,
    val status: String,
    val createdAt: String? = null,
    val startedAt: String? = null,
    val finishedAt: String? = null,
    val error: ApiErrorDto? = null,
    val retryable: Boolean = false,
)

@Serializable
data class ForecastSummaryDto(
    val forecastId: String,
    val status: String,
    val storeId: String,
    val productId: String,
    val sku: String,
    val productName: String,
    val asOfDate: String,
    val horizonDays: Int,
    val method: String,
    val isReplay: Boolean = false,
    val modelVersion: String? = null,
    val createdAt: String? = null,
    /** A past true forecast whose days have since been imported: show "Actuals now available". */
    val actualsAvailable: Boolean = false,
)

@Serializable
data class ForecastPageDto(val page: Int, val pageSize: Int, val total: Int, val items: List<ForecastSummaryDto>)

@Serializable
data class ForecastProductDto(
    val id: String,
    val sku: String,
    val name: String,
    val unitOfMeasure: String = "units",
    val demandClass: DemandClassDto? = null,
)

@Serializable
data class HorizonDto(
    val startDate: String? = null,
    val endDate: String? = null,
    val days: Int = 0,
    /** `false` means experimental: show the "Experimental" badge. */
    val validated: Boolean = true,
)

@Serializable
data class ForecastModelDto(
    val id: String,
    val version: String,
    val featureSchema: String? = null,
    val trainingEndDate: String? = null,
)

@Serializable
data class ForecastPointDto(
    val date: String,
    val ai: Double? = null,
    /** "Same as last day", the main baseline. */
    val baseline: Double? = null,
    /** "Same day last week", the second baseline. */
    val seasonalNaive: Double? = null,
    /** Only on replays, or once the real sales have been imported. */
    val actual: Double? = null,
)

@Serializable
data class RecentSalesDto(val date: String, val actual: Double)

/** All values come from the backend. The app never computes errors (handoff §2). */
@Serializable
data class MetricsDto(
    val n: Int? = null,
    val mae: Double? = null,
    val rmse: Double? = null,
    val wape: Double? = null,
)

/** "Error on these days": replays only. Only the mean absolute error is provided. */
@Serializable
data class RunMetricsDto(
    val window: String? = null,
    val n: Int? = null,
    val ai: MetricsDto? = null,
    val baseline: MetricsDto? = null,
)

/** The product's historical backtest (demo store only). AI and baselines always come from this one block. */
@Serializable
data class ProductBacktestDto(
    val n: Int? = null,
    /** A string `start/end` or an object with `startDate` and `endDate`. */
    val window: JsonElement? = null,
    val ai: MetricsDto? = null,
    @SerialName("naive_last_value") val naiveLastValue: MetricsDto? = null,
    @SerialName("seasonal_naive_7") val seasonalNaive7: MetricsDto? = null,
)

/**
 * `GET /v1/forecasts/{id}`. While the job hasn't completed, only the header fields and [error] are set.
 */
@Serializable
data class ForecastDto(
    val forecastId: String,
    val status: String,
    val createdAt: String? = null,
    val generatedAt: String? = null,
    val storeId: String,
    val isReference: Boolean = false,
    val product: ForecastProductDto,
    val asOfDate: String,
    val isReplay: Boolean = false,
    val method: String,
    val error: ApiErrorDto? = null,
    val horizon: HorizonDto? = null,
    val model: ForecastModelDto? = null,
    val aiUnavailableReason: String? = null,
    /** `KNOWN`, `LAST_KNOWN`, `NONE` or `NOT_USED`. */
    val priceAssumption: String? = null,
    val missingDaysInLookback: Int? = null,
    val warnings: List<JsonElement> = emptyList(),
    val points: List<ForecastPointDto> = emptyList(),
    val history: List<RecentSalesDto> = emptyList(),
    val runMetrics: RunMetricsDto? = null,
    val productBacktest: ProductBacktestDto? = null,
    val historyVersion: Int? = null,
    val mlengineVersion: String? = null,
    val mlRequestId: String? = null,
) {
    val isCompleted: Boolean get() = status == JobStatus.COMPLETED
    val warningCodes: List<String> get() = warnings.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
}

/** Reads the start and end dates out of a window the API gave either as `a/b` or as an object. */
fun JsonElement?.windowDates(): Pair<String, String>? = when (this) {
    null -> null
    is JsonObject -> {
        val start = get("startDate")?.jsonPrimitive?.contentOrNull
        val end = get("endDate")?.jsonPrimitive?.contentOrNull
        if (start != null && end != null) start to end else null
    }
    is JsonPrimitive -> contentOrNull?.split('/')?.takeIf { it.size == 2 }?.let { it[0] to it[1] }
    else -> null
}

interface ForecastApi {
    @POST("v1/forecasts")
    suspend fun create(@Body request: ForecastRequest): JobDto

    @GET("v1/forecast-jobs/{jobId}")
    suspend fun job(@Path("jobId") jobId: String): JobDto

    @POST("v1/forecast-jobs/{jobId}/retry")
    suspend fun retry(@Path("jobId") jobId: String): JobDto

    @GET("v1/forecasts/{forecastId}")
    suspend fun forecast(@Path("forecastId") forecastId: String): ForecastDto

    @GET("v1/forecasts")
    suspend fun list(
        @Query("storeId") storeId: String,
        @Query("productId") productId: String?,
        @Query("replay") replay: Boolean?,
        @Query("status") status: String?,
        @Query("batch") batch: Boolean?,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): ForecastPageDto

    @DELETE("v1/forecasts/{forecastId}")
    suspend fun delete(@Path("forecastId") forecastId: String)

    @Streaming
    @GET("v1/forecasts/{forecastId}/export")
    suspend fun export(@Path("forecastId") forecastId: String, @Query("format") format: String): ResponseBody
}

class ForecastRepository(private val api: ForecastApi, private val json: Json, private val cache: ResponseCache? = null) {

    suspend fun create(request: ForecastRequest): JobDto = apiCall(json) { api.create(request) }

    suspend fun job(jobId: String): JobDto = apiCall(json) { api.job(jobId) }

    suspend fun retry(jobId: String): JobDto = apiCall(json) { api.retry(jobId) }

    /** A completed forecast never changes, so only completed ones are saved for offline viewing. */
    suspend fun forecast(forecastId: String): ForecastDto {
        val fetch = suspend { apiCall(json) { api.forecast(forecastId) } }
        return cache?.load("forecast:$forecastId", ForecastDto.serializer(), cacheable = { it.status == "COMPLETED" }, fetch = fetch) ?: fetch()
    }

    suspend fun list(
        storeId: String,
        replay: Boolean?,
        status: String?,
        page: Int,
        pageSize: Int,
        productId: String? = null,
        /** false leaves out the runs that belong to store-wide batches. */
        batch: Boolean? = null,
    ): ForecastPageDto {
        val fetch = suspend { apiCall(json) { api.list(storeId, productId, replay, status, batch, page, pageSize) } }
        return cache?.load("forecasts:$storeId:$productId:$replay:$status:$batch:$page:$pageSize", ForecastPageDto.serializer(), fetch = fetch) ?: fetch()
    }

    suspend fun delete(forecastId: String) = apiCall(json) { api.delete(forecastId) }

    /** Streams the CSV straight into [sink], which the caller opened through the Storage Access Framework. */
    suspend fun exportCsv(forecastId: String, sink: OutputStream) {
        val body = apiCall(json) { api.export(forecastId, "csv") }
        withContext(Dispatchers.IO) { body.use { it.byteStream().copyTo(sink) } }
    }
}
