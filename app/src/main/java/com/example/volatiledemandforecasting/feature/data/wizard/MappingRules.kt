package com.example.volatiledemandforecasting.feature.data.wizard

import com.example.volatiledemandforecasting.data.imports.DetectedDto
import com.example.volatiledemandforecasting.data.imports.ImportField

/** What is still missing before a mapping can be sent. Nothing here guesses an answer for the user. */
enum class MappingProblem { SalesDate, UnitsSold, ProductIdentity, DateFormat, ZeroQuestion, DuplicateColumn }

object MappingRules {

    /** Starts from the backend's suggestions. `CHECK` ones are flagged in the UI but still pre-filled. */
    fun prefill(detected: DetectedDto?): Map<String, String> =
        detected?.suggestedMapping?.mapValues { it.value.column }.orEmpty()

    fun problems(
        mapping: Map<String, String?>,
        detected: DetectedDto?,
        dateFormat: String?,
        missingDaysAreZero: Boolean?,
    ): List<MappingProblem> = buildList {
        if (mapping[ImportField.SALES_DATE].isNullOrBlank()) add(MappingProblem.SalesDate)
        if (mapping[ImportField.UNITS_SOLD].isNullOrBlank()) add(MappingProblem.UnitsSold)
        if (mapping[ImportField.PRODUCT_ID].isNullOrBlank() && mapping[ImportField.PRODUCT_NAME].isNullOrBlank()) add(MappingProblem.ProductIdentity)
        val chosen = mapping.values.filterNotNull().filter { it.isNotBlank() }
        if (chosen.size != chosen.toSet().size) add(MappingProblem.DuplicateColumn)
        if (detected?.dateFormatAmbiguous == true && dateFormat.isNullOrBlank()) add(MappingProblem.DateFormat)
        // The required question has no default: the answer decides how gaps are stored.
        if (missingDaysAreZero == null) add(MappingProblem.ZeroQuestion)
    }

    /** Only mapped fields are sent. */
    fun toRequestColumns(mapping: Map<String, String?>): Map<String, String> =
        mapping.mapNotNull { (field, column) -> column?.takeIf { it.isNotBlank() }?.let { field to it } }.toMap()

    /** "INVALID_DATE" becomes "Invalid date". The API's own message is shown next to it. */
    fun humanize(code: String): String =
        code.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
}
