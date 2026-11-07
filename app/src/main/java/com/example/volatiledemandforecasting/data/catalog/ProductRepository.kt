package com.example.volatiledemandforecasting.data.catalog

import com.example.volatiledemandforecasting.core.network.apiCall
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** `label` is the API's plain-language text ("Steady seller"). The class code is only used for filtering. */
@Serializable
data class DemandClassDto(
    @SerialName("class") val code: String? = null,
    val label: String? = null,
    val adi: Double? = null,
    val cv2: Double? = null,
    val zeroShare: Double? = null,
)

@Serializable
data class ProductDto(
    val id: String,
    val sku: String,
    val name: String,
    val category: String? = null,
    val unitOfMeasure: String = "units",
    val defaultPrice: Double? = null,
    val isActive: Boolean = true,
    val firstSaleDate: String? = null,
    val demandClass: DemandClassDto? = null,
)

@Serializable
data class ProductPageDto(val page: Int, val pageSize: Int, val total: Int, val items: List<ProductDto>)

/** `units == null` means not recorded, which is different from 0. The chart draws a gap. */
@Serializable
data class HistoryPointDto(val date: String, val units: Double? = null, val price: Double? = null)

@Serializable
data class HistoryDto(val productId: String, val from: String, val to: String, val points: List<HistoryPointDto>)

/** Codes accepted by the `demandClass` filter (api.md). */
object DemandClasses {
    val filterable = listOf("SMOOTH", "ERRATIC", "INTERMITTENT", "LUMPY", "INSUFFICIENT_DATA")
}

interface ProductApi {
    @GET("v1/stores/{storeId}/products")
    suspend fun products(
        @Path("storeId") storeId: String,
        @Query("q") query: String?,
        @Query("demandClass") demandClass: String?,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): ProductPageDto

    @GET("v1/stores/{storeId}/products/{productId}")
    suspend fun product(@Path("storeId") storeId: String, @Path("productId") productId: String): ProductDto

    @GET("v1/stores/{storeId}/products/{productId}/history")
    suspend fun history(
        @Path("storeId") storeId: String,
        @Path("productId") productId: String,
        @Query("from") from: String,
        @Query("to") to: String,
    ): HistoryDto
}

class ProductRepository(private val api: ProductApi, private val json: Json) {

    suspend fun products(storeId: String, query: String, demandClass: String?, page: Int, pageSize: Int): ProductPageDto =
        apiCall(json) { api.products(storeId, query.trim().ifEmpty { null }, demandClass, page, pageSize) }

    suspend fun product(storeId: String, productId: String): ProductDto = apiCall(json) { api.product(storeId, productId) }

    suspend fun history(storeId: String, productId: String, from: String, to: String): HistoryDto =
        apiCall(json) { api.history(storeId, productId, from, to) }
}
