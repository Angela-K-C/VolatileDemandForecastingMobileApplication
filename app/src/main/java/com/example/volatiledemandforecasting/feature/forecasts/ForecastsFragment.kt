package com.example.volatiledemandforecasting.feature.forecasts

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.appViewModels
import com.example.volatiledemandforecasting.app.navigation.MainTab
import com.example.volatiledemandforecasting.app.navigation.navigateToForecastResult
import com.example.volatiledemandforecasting.core.ui.asString
import com.example.volatiledemandforecasting.core.ui.components.ConfirmationDialog
import com.example.volatiledemandforecasting.core.ui.components.ContextMenuBox
import com.example.volatiledemandforecasting.core.ui.components.EmptyState
import com.example.volatiledemandforecasting.core.ui.components.MenuEntry
import com.example.volatiledemandforecasting.core.ui.components.PopupMenuButton
import com.example.volatiledemandforecasting.core.ui.components.UiStateHost
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.data.forecast.ForecastSummaryDto
import com.example.volatiledemandforecasting.feature.workspace.StoreGate

class ForecastsFragment : ShellFragment() {
    override val showsCachedData = true
    override val titleRes = R.string.nav_forecasts
    override val tab = MainTab.Forecasts

    private val viewModel: ForecastsViewModel by appViewModels { ForecastsViewModel(it.forecastRepository, it.activeStore) }

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val navController = findNavController()
        val selection by collectSelection()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val filter by viewModel.filter.collectAsStateWithLifecycle()
        val context = LocalContext.current
        var pendingDelete by remember { mutableStateOf<ForecastSummaryDto?>(null) }

        LaunchedEffect(Unit) {
            viewModel.messages.collect { Toast.makeText(context, it.asString(context), Toast.LENGTH_LONG).show() }
        }

        pendingDelete?.let { item ->
            ConfirmationDialog(
                title = stringResource(R.string.res_delete_confirm_title),
                body = stringResource(R.string.res_delete_confirm_body),
                confirmLabel = stringResource(R.string.res_delete),
                onConfirm = {
                    viewModel.delete(item)
                    pendingDelete = null
                },
                onDismiss = { pendingDelete = null },
                destructive = true,
            )
        }

        StoreGate(selection, onChoose = { navController.navigate(R.id.workspacePickerFragment) }, modifier = Modifier.padding(padding)) {
            Column(Modifier.padding(padding)) {
                PopupMenuButton(
                    label = stringResource(R.string.products_filter) + ": " + filterLabel(filter),
                    options = HistoryFilter.entries,
                    selected = filter,
                    optionLabel = { filterLabel(it) },
                    onSelected = viewModel::setFilter,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                )
                UiStateHost(
                    state = state,
                    onRetry = viewModel::retry,
                    empty = {
                        EmptyState(stringResource(R.string.hist_empty_title), stringResource(R.string.hist_empty_body))
                    },
                ) { list ->
                    val listState = rememberLazyListState()
                    LaunchedEffect(listState, list.items.size) {
                        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                            .collect { last -> if (last != null && last >= list.items.size - 3) viewModel.loadMore() }
                    }
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        items(list.items, key = { it.forecastId }) { item ->
                            val open = { navController.navigateToForecastResult(item.forecastId) }
                            val entries = listOf(
                                MenuEntry(stringResource(R.string.product_view_details), open, Icons.AutoMirrored.Outlined.OpenInNew),
                                MenuEntry(stringResource(R.string.res_delete), { pendingDelete = item }, Icons.Outlined.Delete, destructive = true),
                            )
                            ContextMenuBox(entries, onClick = open) { ForecastRow(item, onClick = null, modifier = Modifier.fillMaxWidth()) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun filterLabel(filter: HistoryFilter): String = stringResource(
    when (filter) {
        HistoryFilter.All -> R.string.hist_filter_all
        HistoryFilter.Replays -> R.string.hist_filter_replay
        HistoryFilter.TrueForecasts -> R.string.hist_filter_forecast
    }
)
