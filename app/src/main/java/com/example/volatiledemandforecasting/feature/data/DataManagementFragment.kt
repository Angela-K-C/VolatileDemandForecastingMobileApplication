package com.example.volatiledemandforecasting.feature.data

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.appViewModels
import com.example.volatiledemandforecasting.app.navigation.MainTab
import com.example.volatiledemandforecasting.core.ui.Formatting
import com.example.volatiledemandforecasting.core.ui.asString
import com.example.volatiledemandforecasting.core.ui.components.ConfirmationDialog
import com.example.volatiledemandforecasting.core.ui.components.ContextMenuBox
import com.example.volatiledemandforecasting.core.ui.components.InlineMessage
import com.example.volatiledemandforecasting.core.ui.components.MenuEntry
import com.example.volatiledemandforecasting.core.ui.components.PopupMenuButton
import com.example.volatiledemandforecasting.core.ui.components.PrimaryButton
import com.example.volatiledemandforecasting.core.ui.components.StatusChip
import com.example.volatiledemandforecasting.core.ui.components.StatusKind
import com.example.volatiledemandforecasting.core.ui.components.UiStateHost
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.data.imports.ImportDto
import com.example.volatiledemandforecasting.data.imports.ImportStatus
import com.example.volatiledemandforecasting.data.store.HistorySummaryDto
import com.example.volatiledemandforecasting.data.workspace.canManageData
import com.example.volatiledemandforecasting.feature.workspace.StoreGate

class DataManagementFragment : ShellFragment() {
    override val titleRes = R.string.nav_data
    override val tab = MainTab.Data

    private val viewModel: DataViewModel by appViewModels { DataViewModel(it.storeRepository, it.importRepository, it.activeStore) }

    override fun onResume() {
        super.onResume()
        // Coming back from the import wizard: show the new batch and history range.
        viewModel.refresh()
    }

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val navController = findNavController()
        val selection by collectSelection()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val filter by viewModel.filter.collectAsStateWithLifecycle()
        val context = LocalContext.current
        var pendingDelete by remember { mutableStateOf<ImportDto?>(null) }

        LaunchedEffect(Unit) {
            viewModel.messages.collect { Toast.makeText(context, it.asString(context), Toast.LENGTH_LONG).show() }
        }
        pendingDelete?.let { item ->
            ConfirmationDialog(
                title = stringResource(R.string.data_delete_title),
                body = stringResource(R.string.data_delete_body, item.filename),
                confirmLabel = stringResource(R.string.data_delete),
                onConfirm = {
                    viewModel.delete(item)
                    pendingDelete = null
                },
                onDismiss = { pendingDelete = null },
                destructive = true,
            )
        }

        StoreGate(selection, onChoose = { navController.navigate(R.id.workspacePickerFragment) }, modifier = Modifier.padding(padding)) { store ->
            UiStateHost(state, onRetry = viewModel::retry, modifier = Modifier.padding(padding), empty = {}) { data ->
                val listState = rememberLazyListState()
                LaunchedEffect(listState, data.imports.size) {
                    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                        .collect { last -> if (last != null && last >= data.imports.size) viewModel.loadMore() }
                }
                LazyColumn(
                    Modifier.padding(padding),
                    state = listState,
                    contentPadding = PaddingValues(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    if (store.isReference) {
                        item { InlineMessage(stringResource(R.string.data_reference_note), StatusKind.Neutral) }
                    } else if (store.canManageData) {
                        item {
                            PrimaryButton(stringResource(R.string.data_upload), { navController.navigate(R.id.importWizardFragment) }, Modifier.fillMaxWidth())
                        }
                    } else {
                        item { InlineMessage(stringResource(R.string.data_viewer_note), StatusKind.Neutral) }
                    }
                    item { SummaryCard(data.summary) }
                    item {
                        OutlinedButton(onClick = { navController.navigate(R.id.readinessFragment) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.data_readiness_link))
                        }
                    }
                    item {
                        androidx.compose.foundation.layout.Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            Text(stringResource(R.string.data_imports), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            // Popup menu (spec §6.3): filters the list, nothing else.
                            PopupMenuButton(
                                label = stringResource(R.string.products_filter) + ": " + statusLabel(filter),
                                options = listOf<String?>(null) + ImportStatus.filterable,
                                selected = filter,
                                optionLabel = { statusLabel(it) },
                                onSelected = viewModel::setFilter,
                            )
                        }
                    }
                    if (data.imports.isEmpty()) {
                        item { Text(stringResource(R.string.data_no_imports), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    items(data.imports, key = { it.id }) { item ->
                        val entries = buildList {
                            if (store.canManageData) add(MenuEntry(stringResource(R.string.data_delete), { pendingDelete = item }, Icons.Outlined.Delete, destructive = true))
                        }
                        if (entries.isEmpty()) ImportRow(item) else ContextMenuBox(entries, onClick = {}) { ImportRow(item) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(summary: HistorySummaryDto) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(stringResource(R.string.data_history_title), style = MaterialTheme.typography.titleSmall)
            if (summary.historyStart == null || summary.historyEnd == null) {
                Text(stringResource(R.string.data_history_empty), style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(
                    stringResource(R.string.picker_history_range, Formatting.date(summary.historyStart), Formatting.date(summary.historyEnd)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.data_history_counts, summary.productsWithData, summary.daysWithData),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                summary.lastImportAt?.let {
                    Text(stringResource(R.string.data_last_updated, Formatting.date(it)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ImportRow(item: ImportDto) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(item.filename, style = MaterialTheme.typography.titleSmall)
            val range = item.summary?.dateRange
            val details = listOfNotNull(
                Formatting.date(item.createdAt),
                item.summary?.let { stringResource(R.string.data_rows, it.rowsAccepted) },
                if (range?.from != null && range.to != null) Formatting.shortDate(range.from) + " – " + Formatting.shortDate(range.to) else null,
            )
            Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            StatusChip(statusLabel(item.status), statusKind(item.status))
        }
    }
}

@Composable
fun statusLabel(status: String?): String = stringResource(
    when (status) {
        null -> R.string.hist_filter_all
        ImportStatus.UPLOADED -> R.string.import_status_uploaded
        ImportStatus.NEEDS_ATTENTION -> R.string.import_status_needs_attention
        ImportStatus.READY -> R.string.import_status_ready
        ImportStatus.IMPORTED -> R.string.import_status_imported
        else -> R.string.import_status_failed
    }
)

fun statusKind(status: String): StatusKind = when (status) {
    ImportStatus.IMPORTED, ImportStatus.READY -> StatusKind.Success
    ImportStatus.NEEDS_ATTENTION -> StatusKind.Warning
    ImportStatus.FAILED -> StatusKind.Error
    else -> StatusKind.Neutral
}
