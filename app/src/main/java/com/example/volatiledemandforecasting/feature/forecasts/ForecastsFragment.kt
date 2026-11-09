package com.example.volatiledemandforecasting.feature.forecasts

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.outlined.UploadFile
import com.example.volatiledemandforecasting.core.ui.components.EmptyState
import com.example.volatiledemandforecasting.data.workspace.canManageData
import com.example.volatiledemandforecasting.core.ui.components.InfoHint
import com.example.volatiledemandforecasting.core.ui.components.Term
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
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
import com.example.volatiledemandforecasting.app.navigation.MainTab
import com.example.volatiledemandforecasting.app.navigation.navigateToForecastResult
import com.example.volatiledemandforecasting.core.datastore.TableDisplay
import com.example.volatiledemandforecasting.core.ui.Formatting
import com.example.volatiledemandforecasting.core.ui.asString
import com.example.volatiledemandforecasting.core.ui.components.AppTextField
import com.example.volatiledemandforecasting.core.ui.components.InlineMessage
import com.example.volatiledemandforecasting.core.ui.components.PrimaryButton
import com.example.volatiledemandforecasting.core.ui.components.SecondaryButton
import com.example.volatiledemandforecasting.core.ui.components.StatusChip
import com.example.volatiledemandforecasting.core.ui.components.StatusKind
import com.example.volatiledemandforecasting.core.ui.components.UiStateHost
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.data.forecast.BatchDto
import com.example.volatiledemandforecasting.data.forecast.BatchItemDto
import com.example.volatiledemandforecasting.data.forecast.JobStatus
import com.example.volatiledemandforecasting.data.forecast.ShownMethod
import com.example.volatiledemandforecasting.data.forecast.shownTotal
import com.example.volatiledemandforecasting.data.forecast.usedFor
import com.example.volatiledemandforecasting.data.workspace.canGenerateForecasts
import com.example.volatiledemandforecasting.feature.workspace.StoreGate

/** The Forecasts tab: every product's expected sales for the next few days, from one store-wide forecast. */
class ForecastsFragment : ShellFragment() {
    override val showsCachedData = true
    override val titleRes = R.string.nav_forecasts
    override val tab = MainTab.Forecasts

    private val viewModel: ForecastsViewModel by appViewModels {
        ForecastsViewModel(it.batchRepository, it.storeRepository, it.activeStore)
    }

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val navController = findNavController()
        val selection by collectSelection()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val query by viewModel.query.collectAsStateWithLifecycle()
        val horizon by viewModel.horizon.collectAsStateWithLifecycle()
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            viewModel.messages.collect { Toast.makeText(context, it.asString(context), Toast.LENGTH_LONG).show() }
        }

        StoreGate(selection, onChoose = { navController.navigate(R.id.workspacePickerFragment) }, modifier = Modifier.padding(padding)) {
            UiStateHost(state, onRetry = viewModel::load, modifier = Modifier.padding(padding), empty = {}) { data ->
                if (!data.hasSales && data.batch == null) {
                    NoSalesYet(data.store.canManageData, onUpload = { navController.navigate(R.id.importWizardFragment) }, Modifier.padding(padding))
                    return@UiStateHost
                }
                val listState = rememberLazyListState()
                LaunchedEffect(listState, data.items.size) {
                    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                        .collect { last -> if (last != null && last >= listState.layoutInfo.totalItemsCount - 3) viewModel.loadMore() }
                }
                val batch = data.batch
                LazyColumn(
                    Modifier.padding(padding),
                    state = listState,
                    contentPadding = PaddingValues(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            Text(stringResource(R.string.sf_intro), style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.cfg_horizon), style = MaterialTheme.typography.titleSmall)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                data.capabilities.horizons.forEach { days ->
                                    FilterChip(selected = horizon == days, onClick = { viewModel.setHorizon(days) }, label = { Text(horizonLabel(days)) })
                                }
                            }
                            if (data.store.canGenerateForecasts) {
                                PrimaryButton(
                                    text = stringResource(if (batch == null) R.string.sf_forecast_all else R.string.sf_forecast_again),
                                    onClick = viewModel::forecastAll,
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = horizon != null && batch?.isRunning != true,
                                    loading = data.starting,
                                )
                            } else {
                                InlineMessage(stringResource(R.string.cfg_viewer), StatusKind.Neutral)
                            }
                        }
                    }
                    if (batch == null) {
                        item { InlineMessage(stringResource(R.string.sf_none_yet), StatusKind.Neutral) }
                    } else {
                        item { BatchStatus(batch, data.shown, onRetry = viewModel::retryFailed, canRetry = data.store.canGenerateForecasts) }
                        if (!batch.isRunning) {
                            item {
                                AppTextField(
                                    value = query,
                                    onValueChange = viewModel::setQuery,
                                    label = stringResource(R.string.products_search),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            items(data.items, key = { it.forecastId }) { item ->
                                ProductForecastRow(item, batch, data.shown, onClick = { navController.navigateToForecastResult(item.forecastId) })
                            }
                            if (data.items.isEmpty() && query.isNotBlank()) {
                                item { Text(stringResource(R.string.sf_no_match), style = MaterialTheme.typography.bodyMedium) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Before any sales there is nothing to forecast: say so, and point at the upload. */
@Composable
internal fun NoSalesYet(canUpload: Boolean, onUpload: () -> Unit, modifier: Modifier = Modifier, products: Boolean = false) {
    EmptyState(
        title = stringResource(R.string.empty_no_sales_title),
        body = stringResource(
            when {
                !canUpload -> R.string.empty_no_sales_body_viewer
                products -> R.string.empty_products_body
                else -> R.string.empty_forecasts_body
            }
        ),
        modifier = modifier,
        icon = androidx.compose.material.icons.Icons.Outlined.UploadFile,
        action = if (canUpload) ({ PrimaryButton(stringResource(R.string.data_upload), onUpload) }) else null,
    )
}

@Composable
private fun BatchStatus(batch: BatchDto, shown: ShownMethod, onRetry: () -> Unit, canRetry: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        if (batch.isRunning) {
            val done = batch.completed + batch.failed
            Text(stringResource(R.string.sf_running, done, batch.total), style = MaterialTheme.typography.titleSmall)
            LinearProgressIndicator(progress = { if (batch.total == 0) 0f else done.toFloat() / batch.total }, modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.sf_running_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }
        Text(
            stringResource(R.string.sf_title, horizonLabel(batch.horizonDays).lowercase(), Formatting.date(batch.asOfDate)),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(if (shown == ShownMethod.Ai) R.string.sf_using_ai else R.string.sf_using_simple),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            InfoHint(if (shown == ShownMethod.Ai) Term.AiForecast else Term.SimpleForecast)
        }
        if (batch.outdated) InlineMessage(stringResource(R.string.sf_outdated), StatusKind.Warning)
        if (batch.failed > 0) {
            InlineMessage(stringResource(R.string.sf_some_failed, batch.failed), StatusKind.Error)
            if (canRetry) SecondaryButton(stringResource(R.string.state_retry), onRetry)
        }
    }
}

@Composable
private fun ProductForecastRow(item: BatchItemDto, batch: BatchDto, shown: ShownMethod, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(item.productName, style = MaterialTheme.typography.titleSmall)
                Text(
                    listOfNotNull(item.sku, item.category).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when (item.status) {
                JobStatus.COMPLETED -> {
                    val method = shown.usedFor(item.points)
                    val total = item.points.shownTotal(method)
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            Formatting.units(total, TableDisplay.WholeUnits),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End,
                        )
                        Text(
                            stringResource(R.string.sf_units_in, item.unitOfMeasure, batch.horizonDays),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                // The reason is on the product's own forecast screen.
                JobStatus.FAILED -> StatusChip(stringResource(R.string.hist_status_failed), StatusKind.Error)
                else -> StatusChip(stringResource(R.string.hist_status_running), StatusKind.Warning)
            }
        }
    }
}
