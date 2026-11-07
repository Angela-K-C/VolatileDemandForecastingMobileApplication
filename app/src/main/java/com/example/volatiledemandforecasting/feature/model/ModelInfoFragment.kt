package com.example.volatiledemandforecasting.feature.model

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.appViewModels
import com.example.volatiledemandforecasting.core.ui.Formatting
import com.example.volatiledemandforecasting.core.ui.charts.ChartSeries
import com.example.volatiledemandforecasting.core.ui.charts.LineChart
import com.example.volatiledemandforecasting.core.ui.charts.LineStyle
import com.example.volatiledemandforecasting.core.ui.components.EmptyState
import com.example.volatiledemandforecasting.core.ui.components.InlineMessage
import com.example.volatiledemandforecasting.core.ui.components.StatusKind
import com.example.volatiledemandforecasting.core.ui.components.UiStateHost
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.data.forecast.windowDates
import com.example.volatiledemandforecasting.data.store.ActiveModelDto
import com.example.volatiledemandforecasting.feature.forecasts.MetricColumn
import com.example.volatiledemandforecasting.feature.forecasts.MetricsBlock
import com.example.volatiledemandforecasting.feature.workspace.StoreGate
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class ModelInfoFragment : ShellFragment() {
    override val titleRes = R.string.screen_model_info

    private val viewModel: ModelInfoViewModel by appViewModels { ModelInfoViewModel(it.storeRepository, it.activeStore) }

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val selection by collectSelection()
        val state by viewModel.state.collectAsStateWithLifecycle()
        StoreGate(selection, onChoose = {}, modifier = Modifier.padding(padding)) {
            UiStateHost(
                state, onRetry = viewModel::load, modifier = Modifier.padding(padding),
                empty = { EmptyState(stringResource(R.string.screen_model_info), stringResource(R.string.model_none)) },
            ) { model ->
                ModelInfo(model, Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun ModelInfo(model: ActiveModelDto, modifier: Modifier) {
    val card = model.card
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(card.title ?: model.model.id, style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.model_version, model.model.version),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        card.predicts?.let { LabelledText(stringResource(R.string.model_what_it_predicts), it) }
        trainedOnText(card.trainedOn)?.let { LabelledText(stringResource(R.string.model_trained_on), it) }

        if (card.inputsPlainLanguage.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(stringResource(R.string.model_inputs), style = MaterialTheme.typography.titleSmall)
                card.inputsPlainLanguage.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            }
        }

        card.backtest?.takeIf { it.ai != null }?.let { backtest ->
            val dates = backtest.window.windowDates()
            MetricsBlock(
                title = stringResource(R.string.model_backtest_title),
                body = stringResource(
                    R.string.model_backtest_body,
                    backtest.series.orEmpty(),
                    backtest.ai?.n ?: 0,
                    dates?.let { (a, b) -> Formatting.date(a) + " – " + Formatting.date(b) }.orEmpty(),
                ),
                columns = listOf(
                    MetricColumn(stringResource(R.string.metrics_ai), backtest.ai),
                    MetricColumn(stringResource(R.string.metrics_baseline), backtest.naiveLastValue),
                    MetricColumn(stringResource(R.string.metrics_last_week), backtest.seasonalNaive7),
                ),
            )
        }

        card.horizonAccuracy?.takeIf { it.byDay.isNotEmpty() }?.let { accuracy ->
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(stringResource(R.string.model_by_day_title), style = MaterialTheme.typography.titleSmall)
                val primary = MaterialTheme.colorScheme.primary
                val tertiary = MaterialTheme.colorScheme.tertiary
                val ai = stringResource(R.string.metrics_ai)
                val baseline = stringResource(R.string.metrics_baseline)
                LineChart(
                    series = listOf(
                        ChartSeries(ai, accuracy.byDay.map { it.aiRmse }, primary, LineStyle.Solid),
                        ChartSeries(baseline, accuracy.byDay.map { it.naiveRmse }, tertiary, LineStyle.Dotted),
                    ),
                    pointCount = accuracy.byDay.size,
                    xLabels = accuracy.byDay.mapIndexed { i, d -> i to d.day.toString() },
                    description = stringResource(R.string.chart_description, "$ai, $baseline"),
                    height = 160.dp,
                )
                Text(stringResource(R.string.model_by_day_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                accuracy.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text(stringResource(R.string.metrics_caveat), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (card.limitations.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(stringResource(R.string.model_limitations), style = MaterialTheme.typography.titleSmall)
                // Shown in full, including the known upward bias on days 2 to 7.
                card.limitations.forEach { InlineMessage(it, StatusKind.Neutral) }
            }
        }
        card.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun LabelledText(label: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** The card describes its training data with a few free-form keys; show whichever are present. */
private fun trainedOnText(trainedOn: JsonObject?): String? {
    if (trainedOn == null) return null
    fun text(key: String) = trainedOn[key]?.let { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }
    val range = listOfNotNull(text("startDate"), text("endDate")).takeIf { it.size == 2 }?.let { (a, b) -> Formatting.date(a) + " – " + Formatting.date(b) }
    return listOfNotNull(text("data"), text("products")?.let { "$it products" }, range).joinToString("\n").ifBlank { null }
}
