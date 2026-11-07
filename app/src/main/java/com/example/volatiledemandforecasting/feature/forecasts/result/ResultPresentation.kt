package com.example.volatiledemandforecasting.feature.forecasts.result

import com.example.volatiledemandforecasting.core.ui.Formatting
import com.example.volatiledemandforecasting.data.forecast.ForecastDto
import com.example.volatiledemandforecasting.data.forecast.ForecastPointDto
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Lays the API's history and forecast points out on one shared date axis. Days with nothing
 * recorded stay null so the chart draws a gap. No value is computed or smoothed here.
 */
data class ChartModel(
    val firstDate: LocalDate,
    val dayCount: Int,
    val history: List<Double?>,
    val ai: List<Double?>,
    val baseline: List<Double?>,
    val actual: List<Double?>,
    /** Index of the as-of day, where the forecast starts. */
    val markerIndex: Int?,
) {
    fun dateAt(index: Int): LocalDate = firstDate.plusDays(index.toLong())
}

fun buildChartModel(forecast: ForecastDto): ChartModel? {
    val dated = forecast.history.mapNotNull { h -> Formatting.parseDate(h.date)?.let { it to h.actual } }
    val pointDates = forecast.points.mapNotNull { Formatting.parseDate(it.date) }
    val allDates = dated.map { it.first } + pointDates
    if (allDates.isEmpty()) return null
    val first = allDates.min()
    val last = allDates.max()
    val count = ChronoUnit.DAYS.between(first, last).toInt() + 1

    fun indexOf(date: LocalDate) = ChronoUnit.DAYS.between(first, date).toInt()

    fun series(values: List<Pair<LocalDate, Double?>>): List<Double?> {
        val out = MutableList<Double?>(count) { null }
        values.forEach { (date, v) -> out[indexOf(date)] = v }
        return out
    }

    val points = forecast.points.mapNotNull { p -> Formatting.parseDate(p.date)?.let { it to p } }
    return ChartModel(
        firstDate = first,
        dayCount = count,
        history = series(dated),
        ai = series(points.map { (d, p) -> d to p.ai }),
        baseline = series(points.map { (d, p) -> d to p.baseline }),
        actual = series(points.map { (d, p) -> d to p.actual }),
        markerIndex = Formatting.parseDate(forecast.asOfDate)?.let { indexOf(it) }?.takeIf { it in 0 until count },
    )
}

/** A column total is only offered when every day has a value, so a missing day never looks like zero. */
data class Totals(val ai: Double?, val baseline: Double?, val seasonal: Double?, val actual: Double?)

fun totalsOf(points: List<ForecastPointDto>): Totals {
    fun sum(selector: (ForecastPointDto) -> Double?): Double? {
        if (points.isEmpty()) return null
        val values = points.map(selector)
        return if (values.any { it == null }) null else values.filterNotNull().sum()
    }
    return Totals(sum { it.ai }, sum { it.baseline }, sum { it.seasonalNaive }, sum { it.actual })
}

/** Which side did better on a block of paired metrics, comparing only numbers the API sent in that block. */
enum class Verdict { AiBetter, BaselineBetter, Tie, Unknown }

fun verdictOf(ai: Double?, baseline: Double?): Verdict = when {
    ai == null || baseline == null -> Verdict.Unknown
    ai < baseline -> Verdict.AiBetter
    ai > baseline -> Verdict.BaselineBetter
    else -> Verdict.Tie
}
