package com.example.volatiledemandforecasting.feature.products.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.appViewModels
import com.example.volatiledemandforecasting.app.navigation.ARG_PRODUCT_ID
import com.example.volatiledemandforecasting.app.navigation.navigateToForecastConfig
import com.example.volatiledemandforecasting.core.ui.Formatting
import com.example.volatiledemandforecasting.core.ui.charts.ChartSeries
import com.example.volatiledemandforecasting.core.ui.charts.LineChart
import com.example.volatiledemandforecasting.core.ui.components.DemandClassChip
import com.example.volatiledemandforecasting.core.ui.components.PrimaryButton
import com.example.volatiledemandforecasting.core.ui.components.UiStateHost
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.data.workspace.canGenerateForecasts

class ProductDetailFragment : ShellFragment() {
    override val showsCachedData = true
    override val titleRes = R.string.screen_product

    private val viewModel: ProductDetailViewModel by appViewModels {
        ProductDetailViewModel(
            requireArguments().getString(ARG_PRODUCT_ID).orEmpty(),
            it.productRepository, it.storeRepository, it.activeStore,
        )
    }

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val navController = findNavController()
        UiStateHost(state, onRetry = viewModel::load, modifier = Modifier.padding(padding), empty = {}) { detail ->
            val product = detail.product
            Column(
                Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Text(product.name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    listOfNotNull(
                        stringResource(R.string.product_sku, product.sku).takeIf { product.sku != product.name },
                        product.category?.let { stringResource(R.string.product_category, it) },
                        product.firstSaleDate?.let { stringResource(R.string.product_first_sale, Formatting.date(it)) },
                    ).joinToString("\n"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                product.demandClass?.label?.let { DemandClassChip(it) }

                Text(stringResource(R.string.product_history_title), style = MaterialTheme.typography.titleMedium)
                if (detail.hasAnySales && detail.firstDate != null) {
                    val first = detail.firstDate
                    val labels = remember(detail) {
                        listOf(0, detail.units.size / 2, detail.units.size - 1)
                            .map { it to Formatting.shortDate(first.plusDays(it.toLong()).toString()) }
                    }
                    val color = MaterialTheme.colorScheme.primary
                    val seriesLabel = stringResource(R.string.res_series_history)
                    LineChart(
                        series = listOf(ChartSeries(seriesLabel, detail.units, color)),
                        pointCount = detail.units.size,
                        xLabels = labels,
                        description = stringResource(R.string.chart_description, seriesLabel),
                    )
                    Text(
                        stringResource(R.string.product_history_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(stringResource(R.string.product_no_history), style = MaterialTheme.typography.bodyMedium)
                }

                if (detail.store.canGenerateForecasts) {
                    PrimaryButton(
                        stringResource(R.string.product_forecast),
                        onClick = { navController.navigateToForecastConfig(product.id) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
