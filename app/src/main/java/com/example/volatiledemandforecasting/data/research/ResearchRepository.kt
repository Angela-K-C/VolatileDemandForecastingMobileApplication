package com.example.volatiledemandforecasting.data.research

import com.example.volatiledemandforecasting.core.network.apiCall
import com.example.volatiledemandforecasting.data.forecast.MetricsDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming
import java.io.OutputStream

/** One group of forecast days, with every method measured on exactly those days. */
@Serializable
data class ResearchBlockDto(
    val n: Int = 0,
    val ai: MetricsDto? = null,
    @SerialName("naive_last_value") val naiveLastValue: MetricsDto? = null,
    @SerialName("seasonal_naive_7") val seasonalNaive7: MetricsDto? = null,
)

@Serializable
data class ResearchSummaryDto(
    val storeId: String,
    val runs: Int = 0,
    val overall: ResearchBlockDto = ResearchBlockDto(),
    val byDemandClass: Map<String, ResearchBlockDto> = emptyMap(),
    val byDayAhead: Map<String, ResearchBlockDto> = emptyMap(),
    val note: String? = null,
)

interface ResearchApi {
    @GET("v1/research/summary")
    suspend fun summary(@Query("storeId") storeId: String, @Query("from") from: String?, @Query("to") to: String?): ResearchSummaryDto

    @Streaming
    @GET("v1/research/export")
    suspend fun export(@Query("storeId") storeId: String, @Query("from") from: String?, @Query("to") to: String?): ResponseBody
}

class ResearchRepository(private val api: ResearchApi, private val json: Json) {
    suspend fun summary(storeId: String, from: String?, to: String?): ResearchSummaryDto =
        apiCall(json) { api.summary(storeId, from, to) }

    /** Streams the CSV into [sink], which the caller opened through the Storage Access Framework. */
    suspend fun exportCsv(storeId: String, from: String?, to: String?, sink: OutputStream) {
        val body = apiCall(json) { api.export(storeId, from, to) }
        withContext(Dispatchers.IO) { body.use { it.byteStream().copyTo(sink) } }
    }
}
