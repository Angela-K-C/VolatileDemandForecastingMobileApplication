package com.example.volatiledemandforecasting.feature.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.appViewModels
import com.example.volatiledemandforecasting.core.ui.Formatting
import com.example.volatiledemandforecasting.core.ui.components.DemoDataBadge
import com.example.volatiledemandforecasting.core.ui.components.EmptyState
import com.example.volatiledemandforecasting.core.ui.components.UiStateHost
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.data.workspace.Roles
import com.example.volatiledemandforecasting.data.workspace.StoreDto
import com.example.volatiledemandforecasting.data.workspace.WorkspaceDto
import com.example.volatiledemandforecasting.data.workspace.WorkspaceWithStores

class WorkspacePickerFragment : ShellFragment() {
    override val titleRes = R.string.screen_workspace_picker

    private val viewModel: WorkspacePickerViewModel by appViewModels {
        WorkspacePickerViewModel(it.workspaceRepository, it.activeStore)
    }

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val navController = findNavController()
        UiStateHost(
            state = state,
            onRetry = viewModel::load,
            modifier = Modifier.padding(padding),
            empty = {},
        ) { data ->
            LazyColumn(
                Modifier.padding(padding),
                contentPadding = PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                items(data.reference, key = { it.workspace.id }) { entry ->
                    WorkspaceCard(entry, data.currentStoreId, blurb = stringResource(R.string.picker_reference_blurb)) { workspace, store ->
                        viewModel.select(workspace, store) { navController.popBackStack() }
                    }
                }
                if (data.own.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.picker_your_workspaces),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = Spacing.sm),
                        )
                    }
                    items(data.own, key = { it.workspace.id }) { entry ->
                        WorkspaceCard(entry, data.currentStoreId, blurb = null) { workspace, store ->
                            viewModel.select(workspace, store) { navController.popBackStack() }
                        }
                    }
                } else {
                    item { Text(stringResource(R.string.picker_no_own), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                item {
                    // Creating a workspace is part of the business journey (build step 4).
                    OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.picker_create_workspace))
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkspaceCard(
    entry: WorkspaceWithStores,
    currentStoreId: String?,
    blurb: String?,
    onPick: (WorkspaceDto, StoreDto) -> Unit,
) {
    val workspace = entry.workspace
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(vertical = Spacing.md)) {
            Column(Modifier.padding(horizontal = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(workspace.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (workspace.isReference) DemoDataBadge()
                }
                if (blurb != null) Text(blurb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    stringResource(R.string.picker_role, roleLabel(workspace)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (entry.stores.isEmpty()) {
                Text(
                    stringResource(R.string.picker_no_stores),
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            entry.stores.forEach { store ->
                HorizontalDivider(Modifier.padding(top = Spacing.md))
                val selected = store.id == currentStoreId
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { onPick(workspace, store) })
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(store.name, style = MaterialTheme.typography.bodyLarge)
                        if (store.historyStart != null && store.historyEnd != null) {
                            Text(
                                stringResource(R.string.picker_history_range, Formatting.date(store.historyStart), Formatting.date(store.historyEnd)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (selected) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = stringResource(R.string.picker_current), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun roleLabel(workspace: WorkspaceDto): String = stringResource(
    when {
        workspace.isReference -> R.string.role_reference
        workspace.role == Roles.OWNER -> R.string.role_owner
        workspace.role == Roles.MANAGER -> R.string.role_manager
        else -> R.string.role_viewer
    }
)
