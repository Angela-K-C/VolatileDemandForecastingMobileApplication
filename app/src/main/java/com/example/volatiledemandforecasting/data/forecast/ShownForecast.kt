package com.example.volatiledemandforecasting.data.forecast

import com.example.volatiledemandforecasting.data.store.BacktestDto

/**
 * A shop owner sees one number per day. It comes from the AI only when the store's own testing
 * showed the AI beat the simple forecast; otherwise from the simple forecast ("same as last day").
 * The full comparison stays available under "How accurate is this?".
 */
enum class ShownMethod { Ai, Simple }

/** Same measure as the accuracy blocks: RMSE when both sides have it, else MAE. No numbers, no AI. */
fun shownMethodOf(backtest: BacktestDto?): ShownMethod {
    val ai = backtest?.ai
    val simple = backtest?.naiveLastValue
    val aiBetter = when {
        ai?.rmse != null && simple?.rmse != null -> ai.rmse < simple.rmse
        ai?.mae != null && simple?.mae != null -> ai.mae < simple.mae
        else -> false
    }
    return if (aiBetter) ShownMethod.Ai else ShownMethod.Simple
}

/** The method actually behind [points]: AI is only used if every day has an AI value. */
fun ShownMethod.usedFor(points: List<ForecastPointDto>): ShownMethod =
    if (this == ShownMethod.Ai && points.isNotEmpty() && points.all { it.ai != null }) ShownMethod.Ai else ShownMethod.Simple

fun ForecastPointDto.shownValue(method: ShownMethod): Double? = if (method == ShownMethod.Ai) ai else baseline

/** Expected units over the whole period, or null when a day has no value (a gap never counts as zero). */
fun List<ForecastPointDto>.shownTotal(method: ShownMethod): Double? {
    if (isEmpty()) return null
    val values = map { it.shownValue(method) }
    return if (values.any { it == null }) null else values.filterNotNull().sum()
}
