package com.example.volatiledemandforecasting.data.forecast

import com.example.volatiledemandforecasting.core.network.ApiException
import com.example.volatiledemandforecasting.core.network.apiCall
import com.example.volatiledemandforecasting.data.cache.ResponseCache
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

object BatchStatus {
    const val RUNNING = "RUNNING"
    const val COMPLETED = "COMPLETED"
}

@Serializable
data class BatchRequest(
    val horizonDays: Int,
    /** Omitted, the backend forecasts every active product with sales history. */
    val productIds: List<String>? = null,
)

/** A store-wide forecast: one forecast per product, grouped. */
@Serializable
data class BatchDto(
    val batchId: String,
    val storeId: String,
    val status: String,
    val method: String,
    val asOfDate: String,
    val horizonDays: Int,
    val total: Int,
    val completed: Int = 0,
    val failed: Int = 0,
    val pending: Int = 0,
    val createdAt: String? = null,
    val finishedAt: String? = null,
    /** The store's sales changed after this batch: offer to forecast again. */
    val outdated: Boolean = false,
) {
    val isRunning: Boolean get() = status == BatchStatus.RUNNING
}

/** One product of a batch. [points] is empty until [status] is COMPLETED. */
@Serializable
data class BatchItemDto(
    val forecastId: String,
    val productId: String,
    val sku: String,
    val productName: String,
    val unitOfMeasure: String = "units",
    val category: String? = null,
    val demandLabel: String? = null,
    val status: String,
    val error: ApiErrorDto? = null,
    val aiUnavailableReason: String? = null,
    val points: List<ForecastPointDto> = emptyList(),
)

@Serializable
data class BatchItemPageDto(val page: Int, val pageSize: Int, val total: Int, val items: List<BatchItemDto>)

interface BatchApi {
    @POST("v1/stores/{storeId}/forecast-batches")
    suspend fun create(@Path("storeId") storeId: String, @Body request: BatchRequest): BatchDto

    @GET("v1/forecast-batches/{batchId}")
    suspend fun batch(@Path("batchId") batchId: String): BatchDto

    @GET("v1/forecast-batches/{batchId}/items")
    suspend fun items(
        @Path("batchId") batchId: String,
        @Query("q") query: String?,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): BatchItemPageDto

    @GET("v1/stores/{storeId}/forecast-batches/latest")
    suspend fun latest(@Path("storeId") storeId: String): BatchDto

    @POST("v1/forecast-batches/{batchId}/retry")
    suspend fun retry(@Path("batchId") batchId: String): BatchDto
}

class BatchRepository(private val api: BatchApi, private val json: Json, private val cache: ResponseCache? = null) {

    suspend fun create(storeId: String, horizonDays: Int): BatchDto =
        apiCall(json) { api.create(storeId, BatchRequest(horizonDays)) }

    suspend fun batch(batchId: String): BatchDto = apiCall(json) { api.batch(batchId) }

    /** The store's newest batch, or null when nothing has been forecast yet. */
    suspend fun latest(storeId: String): BatchDto? = try {
        val fetch = suspend { apiCall(json) { api.latest(storeId) } }
        cache?.load("batch-latest:$storeId", BatchDto.serializer(), fetch = fetch) ?: fetch()
    } catch (e: ApiException) {
        if (e.httpStatus == 404) null else throw e
    }

    /** Pages of a finished batch never change, so they are kept for offline viewing. */
    suspend fun items(batch: BatchDto, query: String?, page: Int, pageSize: Int): BatchItemPageDto {
        val q = query?.trim()?.takeIf { it.isNotEmpty() }
        val fetch = suspend { apiCall(json) { api.items(batch.batchId, q, page, pageSize) } }
        if (cache == null || batch.isRunning) return fetch()
        return cache.load("batch-items:${batch.batchId}:$q:$page:$pageSize", BatchItemPageDto.serializer(), fetch = fetch)
    }

    suspend fun retry(batchId: String): BatchDto = apiCall(json) { api.retry(batchId) }
}
