package com.example.volatiledemandforecasting.core.ui

import com.example.volatiledemandforecasting.core.datastore.TableDisplay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Display formatting only. Nothing here changes a value the API sent, except rounding it for the
 * screen; totals are summed from raw values by the caller (spec §8.10).
 */
object Formatting {


    fun parseDate(iso: String?): LocalDate? = iso?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }

    /** "8 May 2016", in the user's locale. Falls back to the raw text if it is not a date. */
    fun date(iso: String?): String = parseDate(iso)?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: iso.orEmpty()

    fun shortDate(iso: String?): String = parseDate(iso)?.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())) ?: iso.orEmpty()

    /** A decimal with one place by default, or a rounded whole number. Null is "not known", never 0. */
    fun units(value: Double?, display: TableDisplay = TableDisplay.Decimals, notKnown: String = "–"): String = when {
        value == null -> notKnown
        display == TableDisplay.WholeUnits -> Math.round(value).toString()
        else -> String.format(Locale.getDefault(), "%.1f", value)
    }

    /** Error metrics get two places so close results are still distinguishable. */
    fun metric(value: Double?, notKnown: String = "–"): String =
        if (value == null) notKnown else String.format(Locale.getDefault(), "%.2f", value)

    /** WAPE arrives as a fraction (0.61). It is shown as a percentage. */
    fun percent(fraction: Double?, notKnown: String = "–"): String =
        if (fraction == null) notKnown else String.format(Locale.getDefault(), "%.0f%%", fraction * 100)
}
