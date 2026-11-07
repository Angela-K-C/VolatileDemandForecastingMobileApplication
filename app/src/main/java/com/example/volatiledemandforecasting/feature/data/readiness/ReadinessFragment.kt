package com.example.volatiledemandforecasting.feature.data.readiness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.appViewModels
import com.example.volatiledemandforecasting.app.navigation.navigateToProduct
import com.example.volatiledemandforecasting.core.datastore.StoreSelection
import com.example.volatiledemandforecasting.core.network.attempt
import com.example.volatiledemandforecasting.core.ui.Formatting
import com.example.volatiledemandforecasting.core.ui.components.ContextMenuBox
import com.example.volatiledemandforecasting.core.ui.components.EmptyState
import com.example.volatiledemandforecasting.core.ui.components.InlineMessage
import com.example.volatiledemandforecasting.core.ui.components.MenuEntry
import com.example.volatiledemandforecasting.core.ui.components.StatusChip
import com.example.volatiledemandforecasting.core.ui.components.StatusKind
import com.example.volatiledemandforecasting.core.ui.components.UiState
import com.example.volatiledemandforecasting.core.ui.components.UiStateHost
import com.example.volatiledemandforecasting.core.ui.components.toUiState
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.data.store.ProductReadinessDto
import com.example.volatiledemandforecasting.data.store.ReadinessDetailDto
import com.example.volatiledemandforecasting.data.store.StoreRepository
import com.example.volatiledemandforecasting.data.workspace.ActiveStore
import com.example.volatiledemandforecasting.feature.workspace.StoreGate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ReadinessViewModel(private val stores: StoreRepository, private val activeStore: ActiveStore) : ViewModel() {
    private val _state = MutableStateFlow<UiState<ReadinessDetailDto>>(UiState.Loading)
    val state: StateFlow<UiState<ReadinessDetailDto>> = _state.asStateFlow()
    private var store: StoreSelection? = null

    init {
        viewModelScope.launch {
            combine(activeStore.selected, activeStore.dataRevision) { s, _ -> s }.collect {
                store = it
                load()
            }
        }
    }

    fun load() {
        val s = store ?: return
        _state.value = UiState.Loading
        viewModelScope.launch {
            attempt { stores.readinessDetail(s.storeId) }
                .onSuccess { _state.value = if (it.products.isEmpty()) UiState.Empty else UiState.Content(it) }
                .onFailure { _state.value = it.toUiState() }
        }
    }
}

class ReadinessFragment : ShellFragment() {
    override val titleRes = R.string.screen_readiness

    private val viewModel: ReadinessViewModel by appViewModels { ReadinessViewModel(it.storeRepository, it.activeStore) }

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val selection by collectSelection()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val navController = findNavController()
        StoreGate(selection, onChoose = {}, modifier = Modifier.padding(padding)) {
            UiStateHost(
                state, onRetry = viewModel::load, modifier = Modifier.padding(padding),
                empty = { EmptyState(stringResource(R.string.ready_empty_title), stringResource(R.string.ready_empty_body)) },
            ) { report ->
                LazyColumn(
                    Modifier.padding(padding),
                    contentPadding = PaddingValues(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Text(
                                stringResource(R.string.ready_summary, report.productsReady, report.productsTotal),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            // The thresholds are the API's own, for the current model.
                            Text(
                                stringResource(R.string.ready_rules, report.minHistoryDays, report.maxMissingDaysInLookback),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (report.stale) {
                                InlineMessage(stringResource(R.string.ready_stale), StatusKind.Warning)
                            }
                            Text(stringResource(R.string.ready_baselines_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(report.products, key = { it.productId }) { product ->
                        ContextMenuBox(
                            listOf(MenuEntry(stringResource(R.string.product_view_details), { navController.navigateToProduct(product.productId) })),
                            onClick = { navController.navigateToProduct(product.productId) },
                        ) { ProductReadinessRow(product, report) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductReadinessRow(product: ProductReadinessDto, report: ReadinessDetailDto) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(product.sku, style = MaterialTheme.typography.titleSmall)
            StatusChip(stringResource(if (product.ready) R.string.ready_ok else R.string.ready_not_ok), if (product.ready) StatusKind.Success else StatusKind.Warning)
            product.reasons.forEach { reason ->
                Text(
                    stringResource(
                        when (reason) {
                            "INSUFFICIENT_HISTORY" -> R.string.ready_reason_history
                            "TOO_MANY_GAPS" -> R.string.ready_reason_gaps
                            "STALE_HISTORY" -> R.string.ready_reason_stale
                            else -> R.string.ready_reason_other
                        },
                        report.minHistoryDays,
                        report.maxMissingDaysInLookback,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                stringResource(R.string.ready_days, product.recordedDaysInLookback, product.missingDaysInLookback) +
                    (product.latestDate?.let { " · " + stringResource(R.string.ready_latest, Formatting.date(it)) } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
