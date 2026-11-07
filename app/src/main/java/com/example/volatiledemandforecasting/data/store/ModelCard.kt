package com.example.volatiledemandforecasting.data.store

import com.example.volatiledemandforecasting.data.forecast.MetricsDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class ActiveModelRefDto(
    val id: String,
    val version: String,
    val trainingEndDate: String? = null,
    val featureSchema: String? = null,
)

/** `GET /v1/stores/{sid}/models/active`: the model plus its card for the model info screen. */
@Serializable
data class ActiveModelDto(val model: ActiveModelRefDto, val card: ModelCardDto)

@Serializable
data class ModelCardDto(
    val title: String? = null,
    val model: String? = null,
    /** Different models describe their training data with different keys, so this stays open. */
    val trainedOn: JsonObject? = null,
    val predicts: String? = null,
    val horizonDays: List<Int> = emptyList(),
    val inputsPlainLanguage: List<String> = emptyList(),
    val backtest: BacktestDto? = null,
    val horizonAccuracy: HorizonAccuracyDto? = null,
    val limitations: List<String> = emptyList(),
    val note: String? = null,
)

@Serializable
data class BacktestDto(
    /** An object with `startDate` and `endDate`. */
    val window: JsonElement? = null,
    val series: String? = null,
    val modelVersion: String? = null,
    val oneStepAhead: Boolean? = null,
    val ai: MetricsDto? = null,
    @SerialName("naive_last_value") val naiveLastValue: MetricsDto? = null,
    @SerialName("seasonal_naive_7") val seasonalNaive7: MetricsDto? = null,
    val byDemandClass: Map<String, ClassBacktestDto> = emptyMap(),
)

@Serializable
data class ClassBacktestDto(
    val products: Int? = null,
    val ai: MetricsDto? = null,
    @SerialName("naive_last_value") val naiveLastValue: MetricsDto? = null,
)

@Serializable
data class HorizonAccuracyDto(
    val description: String? = null,
    val validated: Boolean? = null,
    val byDay: List<DayAccuracyDto> = emptyList(),
)

@Serializable
data class DayAccuracyDto(
    val day: Int,
    val aiRmse: Double? = null,
    val aiMae: Double? = null,
    val naiveRmse: Double? = null,
    val naiveMae: Double? = null,
)
