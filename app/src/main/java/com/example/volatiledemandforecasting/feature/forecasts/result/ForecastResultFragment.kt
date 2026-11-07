package com.example.volatiledemandforecasting.feature.forecasts.result

import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.appViewModels
import com.example.volatiledemandforecasting.app.navigation.ARG_FORECAST_ID
import com.example.volatiledemandforecasting.core.datastore.TableDisplay
import com.example.volatiledemandforecasting.core.network.ApiException
import com.example.volatiledemandforecasting.core.network.toUiText
import com.example.volatiledemandforecasting.core.ui.Formatting
import com.example.volatiledemandforecasting.core.ui.asString
import com.example.volatiledemandforecasting.core.ui.charts.ChartSeries
import com.example.volatiledemandforecasting.core.ui.charts.LineChart
import com.example.volatiledemandforecasting.core.ui.charts.LineStyle
import com.example.volatiledemandforecasting.core.ui.components.ConfirmationDialog
import com.example.volatiledemandforecasting.core.ui.components.DemandClassChip
import com.example.volatiledemandforecasting.core.ui.components.InlineMessage
import com.example.volatiledemandforecasting.core.ui.components.PopupMenuButton
import com.example.volatiledemandforecasting.core.ui.components.SecondaryButton
import com.example.volatiledemandforecasting.core.ui.components.StatusChip
import com.example.volatiledemandforecasting.core.ui.components.StatusKind
import com.example.volatiledemandforecasting.core.ui.components.UiState
import com.example.volatiledemandforecasting.core.ui.components.UiStateHost
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.core.ui.theme.appColors
import com.example.volatiledemandforecasting.data.forecast.ForecastDto
import com.example.volatiledemandforecasting.data.forecast.JobStatus
import com.example.volatiledemandforecasting.data.forecast.windowDates
import com.example.volatiledemandforecasting.feature.forecasts.MetricColumn
import com.example.volatiledemandforecasting.feature.forecasts.MetricsBlock
import com.example.volatiledemandforecasting.feature.forecasts.config.aiReasonText
import com.example.volatiledemandforecasting.feature.forecasts.horizonLabel
import com.example.volatiledemandforecasting.feature.forecasts.methodLabel

class ForecastResultFragment : ShellFragment() {
    override val showsCachedData = true
    override val titleRes = R.string.screen_forecast_result

    private val viewModel: ForecastResultViewModel by appViewModels {
        ForecastResultViewModel(
            forecastId = requireArguments().getString(ARG_FORECAST_ID).orEmpty(),
            forecasts = it.forecastRepository,
            stores = it.storeRepository,
            preferences = it.preferences,
        )
    }

    private val createDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) viewModel.export { requireContext().contentResolver.openOutputStream(uri) }
    }

    private var showDeleteConfirm by mutableStateOf(false)

    @Composable
    override fun RowScope.ContextualAction() {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val completed = (state as? UiState.Content)?.data?.forecast?.isCompleted == true
        if (completed) {
            IconButton(onClick = { createDocument.launch("forecast-${requireArguments().getString(ARG_FORECAST_ID).orEmpty().take(8)}.csv") }) {
                Icon(Icons.Outlined.FileDownload, contentDescription = stringResource(R.string.res_export_csv))
            }
        }
        IconButton(onClick = { showDeleteConfirm = true }) {
            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.res_delete))
        }
    }

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val display by viewModel.display.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val navController = findNavController()

        LaunchedEffect(Unit) {
            viewModel.events.collect { event ->
                when (event) {
                    ResultEvent.Exported -> Toast.makeText(context, R.string.res_export_done, Toast.LENGTH_SHORT).show()
                    is ResultEvent.ExportFailed -> Toast.makeText(context, event.message.asString(context), Toast.LENGTH_LONG).show()
                    ResultEvent.Deleted -> navController.popBackStack()
                    is ResultEvent.DeleteFailed -> Toast.makeText(context, event.message.asString(context), Toast.LENGTH_LONG).show()
                }
            }
        }

        if (showDeleteConfirm) {
            ConfirmationDialog(
                title = stringResource(R.string.res_delete_confirm_title),
                body = stringResource(R.string.res_delete_confirm_body),
                confirmLabel = stringResource(R.string.res_delete),
                onConfirm = {
                    showDeleteConfirm = false
                    viewModel.delete()
                },
                onDismiss = { showDeleteConfirm = false },
                destructive = true,
            )
        }

        UiStateHost(state, onRetry = viewModel::load, modifier = Modifier.padding(padding), empty = {}) { data ->
            val forecast = data.forecast
            if (!forecast.isCompleted) {
                NotFinished(forecast, onRefresh = viewModel::load, modifier = Modifier.padding(padding))
            } else {
                ResultBody(data, display, viewModel::setDisplay, Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun NotFinished(forecast: ForecastDto, onRefresh: () -> Unit, modifier: Modifier) {
    Column(modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        if (forecast.status == JobStatus.FAILED) {
            val message = forecast.error?.let { ApiException(httpStatus = 0, code = it.code, message = it.message).toUiText() }
            InlineMessage(stringResource(R.string.res_failed, message?.asString().orEmpty()), StatusKind.Error)
        } else {
            InlineMessage(stringResource(R.string.res_not_ready), StatusKind.Warning)
            SecondaryButton(stringResource(R.string.state_retry), onRefresh)
        }
    }
}

@Composable
private fun ResultBody(data: ResultData, display: TableDisplay, onDisplay: (TableDisplay) -> Unit, modifier: Modifier) {
    val forecast = data.forecast
    val aiUsed = forecast.points.any { it.ai != null }
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Header(forecast)

        val chart = remember(forecast) { buildChartModel(forecast) }
        if (chart != null) {
            val onSurface = MaterialTheme.colorScheme.onSurface
            val primary = MaterialTheme.colorScheme.primary
            val tertiary = MaterialTheme.colorScheme.tertiary
            val success = MaterialTheme.appColors.success
            val series = buildList {
                add(ChartSeries(stringResource(R.string.res_series_history), chart.history, onSurface, LineStyle.Solid))
                if (chart.ai.any { it != null }) add(ChartSeries(stringResource(R.string.res_series_ai), chart.ai, primary, LineStyle.Dashed))
                if (chart.baseline.any { it != null }) add(ChartSeries(stringResource(R.string.res_series_baseline), chart.baseline, tertiary, LineStyle.Dotted))
                if (chart.actual.any { it != null }) add(ChartSeries(stringResource(R.string.res_series_actual), chart.actual, success, LineStyle.Dots))
            }
            val labels = listOf(0, chart.dayCount / 2, chart.dayCount - 1).distinct().map { it to Formatting.shortDate(chart.dateAt(it).toString()) }
            LineChart(
                series = series,
                pointCount = chart.dayCount,
                xLabels = labels,
                description = stringResource(R.string.chart_description, series.joinToString { it.label }),
                markerIndex = chart.markerIndex,
                markerLabel = stringResource(R.string.res_from_date, Formatting.shortDate(forecast.asOfDate)),
                height = 220.dp,
            )
        }

        if (forecast.priceAssumption == "LAST_KNOWN") InlineMessage(stringResource(R.string.res_price_assumed), StatusKind.Neutral)
        if (forecast.horizon?.validated == false) InlineMessage(stringResource(R.string.res_experimental_note), StatusKind.Warning)
        if (!aiUsed && forecast.aiUnavailableReason != null) {
            InlineMessage(stringResource(R.string.res_ai_unavailable_note, aiReasonText(forecast.aiUnavailableReason)), StatusKind.Neutral)
        }

        ForecastTable(forecast, display, onDisplay)

        // Evidence 1: this run, only when real sales are known for the forecast days (replays).
        forecast.runMetrics?.let { run ->
            MetricsBlock(
                title = stringResource(R.string.res_ev_run_title),
                body = stringResource(R.string.res_ev_run_body, run.n ?: 0, run.window?.replace("/", " – ").orEmpty()),
                columns = listOf(
                    MetricColumn(stringResource(R.string.metrics_ai), run.ai),
                    MetricColumn(stringResource(R.string.metrics_baseline), run.baseline),
                ),
            )
        }

        // Evidence 2: this product's own backtest (demo store only).
        forecast.productBacktest?.takeIf { it.ai != null || it.naiveLastValue != null }?.let { backtest ->
            val dates = backtest.window.windowDates()
            MetricsBlock(
                title = stringResource(R.string.res_ev_product_title),
                body = stringResource(
                    R.string.res_ev_product_body,
                    backtest.n ?: backtest.ai?.n ?: 0,
                    dates?.let { (a, b) -> Formatting.shortDate(a) + " – " + Formatting.shortDate(b) }.orEmpty(),
                ),
                columns = listOf(
                    MetricColumn(stringResource(R.string.metrics_ai), backtest.ai),
                    MetricColumn(stringResource(R.string.metrics_baseline), backtest.naiveLastValue),
                    MetricColumn(stringResource(R.string.metrics_last_week), backtest.seasonalNaive7),
                ),
                note = stringResource(R.string.res_ev_product_small),
            )
        }

        // Evidence 3: how the model did over the whole store.
        data.card?.backtest?.takeIf { it.ai != null }?.let { backtest ->
            val dates = backtest.window.windowDates()
            MetricsBlock(
                title = stringResource(R.string.res_ev_model_title),
                body = stringResource(
                    R.string.res_ev_model_body,
                    backtest.ai?.n ?: 0,
                    dates?.let { (a, b) -> Formatting.shortDate(a) + " – " + Formatting.shortDate(b) }.orEmpty(),
                ),
                columns = listOf(
                    MetricColumn(stringResource(R.string.metrics_ai), backtest.ai),
                    MetricColumn(stringResource(R.string.metrics_baseline), backtest.naiveLastValue),
                    MetricColumn(stringResource(R.string.metrics_last_week), backtest.seasonalNaive7),
                ),
            )
        }

        // The known bias of multi-day forecasts, taken from the model card's limitations (spec/handoff §9).
        if (aiUsed && (forecast.horizon?.days ?: 0) > 1) {
            InlineMessage(stringResource(R.string.res_four_percent), StatusKind.Warning)
        }
    }
}

@Composable
private fun Header(forecast: ForecastDto) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(forecast.product.name, style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(
                R.string.hist_summary,
                horizonLabel(forecast.horizon?.days ?: 0),
                methodLabel(forecast.method),
                Formatting.date(forecast.asOfDate),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            StatusChip(
                stringResource(if (forecast.isReplay) R.string.res_replay_tag else R.string.res_forecast_tag),
                if (forecast.isReplay) StatusKind.Neutral else StatusKind.Success,
            )
            if (forecast.horizon?.validated == false) StatusChip(stringResource(R.string.res_experimental), StatusKind.Warning)
            forecast.product.demandClass?.label?.let { DemandClassChip(it) }
            forecast.model?.let { StatusChip(stringResource(R.string.model_version, it.version), StatusKind.Neutral) }
        }
    }
}

@Composable
private fun ForecastTable(forecast: ForecastDto, display: TableDisplay, onDisplay: (TableDisplay) -> Unit) {
    val points = forecast.points
    val showAi = points.any { it.ai != null }
    val showLastWeek = points.any { it.seasonalNaive != null }
    val showActual = points.any { it.actual != null }
    val totals = totalsOf(points)
    val none = stringResource(R.string.res_not_known)

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.res_table_title), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            // Popup menu (spec §6.3): a display option only. Totals are always summed from the raw values.
            PopupMenuButton(
                label = stringResource(R.string.res_display_menu),
                options = TableDisplay.entries,
                selected = display,
                optionLabel = { stringResource(if (it == TableDisplay.Decimals) R.string.res_display_decimals else R.string.res_display_whole) },
                onSelected = onDisplay,
            )
        }
        TableRow(
            listOfNotNull(
                stringResource(R.string.res_col_date),
                stringResource(R.string.res_col_ai).takeIf { showAi },
                stringResource(R.string.res_col_last_day),
                stringResource(R.string.res_col_last_week).takeIf { showLastWeek },
                stringResource(R.string.res_col_actual).takeIf { showActual },
            ),
            header = true,
        )
        HorizontalDivider()
        points.forEach { p ->
            TableRow(
                listOfNotNull(
                    Formatting.shortDate(p.date),
                    Formatting.units(p.ai, display, none).takeIf { showAi },
                    Formatting.units(p.baseline, display, none),
                    Formatting.units(p.seasonalNaive, display, none).takeIf { showLastWeek },
                    Formatting.units(p.actual, display, none).takeIf { showActual },
                )
            )
        }
        if (points.size > 1) {
            HorizontalDivider()
            TableRow(
                listOfNotNull(
                    stringResource(R.string.res_total_title),
                    Formatting.units(totals.ai, display, none).takeIf { showAi },
                    Formatting.units(totals.baseline, display, none),
                    Formatting.units(totals.seasonal, display, none).takeIf { showLastWeek },
                    Formatting.units(totals.actual, display, none).takeIf { showActual },
                ),
                header = true,
            )
        }
    }
}

@Composable
private fun TableRow(cells: List<String>, header: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {
        cells.forEachIndexed { i, text ->
            Text(
                text,
                Modifier.weight(if (i == 0) 1.4f else 1f),
                style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
                fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = if (i == 0) TextAlign.Start else TextAlign.End,
            )
        }
    }
}
