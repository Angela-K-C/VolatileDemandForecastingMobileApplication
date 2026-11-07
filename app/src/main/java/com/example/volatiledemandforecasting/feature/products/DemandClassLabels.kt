package com.example.volatiledemandforecasting.feature.products

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.volatiledemandforecasting.R

/**
 * Plain-language names for the demand class codes, used only for the filter menu.
 * On a product the API's own `demandClass.label` is shown instead.
 */
@Composable
fun demandClassFilterLabel(code: String?): String = when (code) {
    null -> stringResource(R.string.products_filter_all)
    "SMOOTH" -> stringResource(R.string.class_smooth)
    "ERRATIC" -> stringResource(R.string.class_erratic)
    "INTERMITTENT" -> stringResource(R.string.class_intermittent)
    "LUMPY" -> stringResource(R.string.class_lumpy)
    else -> stringResource(R.string.class_insufficient)
}
