package com.example.volatiledemandforecasting.feature.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.appViewModels
import com.example.volatiledemandforecasting.app.navigation.MainTab
import com.example.volatiledemandforecasting.app.navigation.navigateToForecastConfig
import com.example.volatiledemandforecasting.app.navigation.navigateToProduct
import com.example.volatiledemandforecasting.core.ui.asString
import com.example.volatiledemandforecasting.core.ui.components.ContextMenuBox
import com.example.volatiledemandforecasting.core.ui.components.DemandClassChip
import com.example.volatiledemandforecasting.core.ui.components.EmptyState
import com.example.volatiledemandforecasting.core.ui.components.MenuEntry
import com.example.volatiledemandforecasting.core.ui.components.PopupMenuButton
import com.example.volatiledemandforecasting.core.ui.components.SecondaryButton
import com.example.volatiledemandforecasting.core.ui.components.UiStateHost
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.data.catalog.DemandClasses
import com.example.volatiledemandforecasting.data.catalog.ProductDto
import com.example.volatiledemandforecasting.data.workspace.canGenerateForecasts
import com.example.volatiledemandforecasting.feature.workspace.StoreGate

class ProductsFragment : ShellFragment() {
    override val showsCachedData = true
    override val titleRes = R.string.nav_products
    override val tab = MainTab.Products

    private val viewModel: ProductsViewModel by appViewModels { ProductsViewModel(it.productRepository, it.activeStore) }

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val navController = findNavController()
        val selection by collectSelection()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val query by viewModel.query.collectAsStateWithLifecycle()
        val demandClass by viewModel.demandClass.collectAsStateWithLifecycle()

        StoreGate(selection, onChoose = { navController.navigate(R.id.workspacePickerFragment) }, modifier = Modifier.padding(padding)) { store ->
            Column(Modifier.padding(padding)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier.fillMaxWidth().padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.md),
                    placeholder = { Text(stringResource(R.string.products_search)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                )
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    // Popup menu (spec §6.3): changes how the list is filtered, nothing else.
                    PopupMenuButton(
                        label = stringResource(R.string.products_filter) + ": " + demandClassFilterLabel(demandClass),
                        options = listOf<String?>(null) + DemandClasses.filterable,
                        selected = demandClass,
                        optionLabel = { demandClassFilterLabel(it) },
                        onSelected = viewModel::setDemandClass,
                    )
                    val total = (state as? com.example.volatiledemandforecasting.core.ui.components.UiState.Content)?.data?.total
                    if (total != null) {
                        Text(stringResource(R.string.products_count, total), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                UiStateHost(
                    state = state,
                    onRetry = viewModel::retry,
                    empty = {
                        EmptyState(
                            title = stringResource(R.string.products_empty_title),
                            body = stringResource(R.string.products_empty_body),
                        )
                    },
                ) { list ->
                    val listState = rememberLazyListState()
                    LaunchedEffect(listState, list.items.size) {
                        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                            .collect { last -> if (last != null && last >= list.items.size - 5) viewModel.loadMore() }
                    }
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        items(list.items, key = { it.id }) { product ->
                            val entries = buildList {
                                add(MenuEntry(stringResource(R.string.product_view_details), { navController.navigateToProduct(product.id) }, Icons.Outlined.Info))
                                if (store.canGenerateForecasts) {
                                    add(MenuEntry(stringResource(R.string.product_forecast_next_day), { navController.navigateToForecastConfig(product.id, horizon = 1) }, Icons.Outlined.Timeline))
                                }
                            }
                            ContextMenuBox(entries, onClick = { navController.navigateToProduct(product.id) }) {
                                ProductRow(product)
                            }
                        }
                        if (list.loadMoreError != null) {
                            item {
                                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(list.loadMoreError.asString(), style = MaterialTheme.typography.bodySmall)
                                    SecondaryButton(stringResource(R.string.products_load_more), viewModel::loadMore)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductRow(product: ProductDto) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(product.name, style = MaterialTheme.typography.titleSmall)
            Text(
                listOfNotNull(product.sku.takeIf { it != product.name }, product.category).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            product.demandClass?.label?.let { DemandClassChip(it) }
        }
    }
}
