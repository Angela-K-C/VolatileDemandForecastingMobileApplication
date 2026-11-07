package com.example.volatiledemandforecasting.feature.devgallery

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.core.ui.components.ConfirmationDialog
import com.example.volatiledemandforecasting.core.ui.components.ContextMenuBox
import com.example.volatiledemandforecasting.core.ui.components.DemandClassChip
import com.example.volatiledemandforecasting.core.ui.components.DemoDataBadge
import com.example.volatiledemandforecasting.core.ui.components.DestructiveButton
import com.example.volatiledemandforecasting.core.ui.components.EmptyState
import com.example.volatiledemandforecasting.core.ui.components.MenuEntry
import com.example.volatiledemandforecasting.core.ui.components.MetricCard
import com.example.volatiledemandforecasting.core.ui.components.OfflineBanner
import com.example.volatiledemandforecasting.core.ui.components.PopupMenuButton
import com.example.volatiledemandforecasting.core.ui.components.PrimaryButton
import com.example.volatiledemandforecasting.core.ui.components.SecondaryButton
import com.example.volatiledemandforecasting.core.ui.components.StatusChip
import com.example.volatiledemandforecasting.core.ui.components.StatusKind
import com.example.volatiledemandforecasting.core.ui.components.UiState
import com.example.volatiledemandforecasting.core.ui.UiText
import com.example.volatiledemandforecasting.core.ui.components.UiStateHost
import com.example.volatiledemandforecasting.core.ui.theme.Spacing

private enum class DemoState(@param:StringRes val labelRes: Int) {
    Content(R.string.gallery_state_content),
    Loading(R.string.gallery_state_loading),
    Empty(R.string.gallery_state_empty),
    Error(R.string.gallery_state_error),
    Offline(R.string.gallery_state_offline),
}

private enum class SortOption(@param:StringRes val labelRes: Int) {
    Name(R.string.gallery_sort_name),
    Sku(R.string.gallery_sort_sku),
    Category(R.string.gallery_sort_category),
}

/**
 * Debug builds only (reached from the Options menu). Lets the three menu types and every
 * shared component be exercised before real screens exist. The sample items are not data.
 */
class DevGalleryFragment : ShellFragment() {
    override val titleRes = R.string.screen_dev_gallery

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            Text(
                stringResource(R.string.gallery_sample_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StatesSection()
            ContextMenuSection()
            PopupMenuSection()
            ButtonsSection()
        }
    }
}

@Composable
private fun Section(@StringRes titleRes: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun StatesSection() = Section(R.string.gallery_section_states) {
    var selected by rememberSaveable { mutableStateOf(DemoState.Content) }
    Row(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        DemoState.entries.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { selected = option },
                label = { Text(stringResource(option.labelRes)) },
            )
        }
    }
    val uiState: UiState<String> = when (selected) {
        DemoState.Content -> UiState.Content(stringResource(R.string.gallery_content_text))
        DemoState.Loading -> UiState.Loading
        DemoState.Empty -> UiState.Empty
        DemoState.Error -> UiState.Error(UiText.Res(R.string.gallery_error_message))
        DemoState.Offline -> UiState.Offline
    }
    val cardModifier = Modifier.fillMaxWidth().height(260.dp)
    Card(cardModifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column {
            if (selected == DemoState.Content) OfflineBanner(lastSynced = null)
            UiStateHost(
                state = uiState,
                onRetry = {},
                empty = {
                    EmptyState(
                        title = stringResource(R.string.gallery_empty_title),
                        body = stringResource(R.string.gallery_empty_body),
                    )
                },
            ) { text ->
                Text(text, Modifier.padding(Spacing.lg), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun ContextMenuSection() = Section(R.string.gallery_section_context) {
    var lastAction by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDelete by rememberSaveable { mutableStateOf<Int?>(null) }
    val open = stringResource(R.string.gallery_context_open)
    val edit = stringResource(R.string.gallery_context_edit)
    val delete = stringResource(R.string.gallery_context_delete)
    val deleteLabel = stringResource(R.string.action_delete)

    (1..3).forEach { index ->
        val name = stringResource(R.string.gallery_sample_item, index)
        ContextMenuBox(
            entries = listOf(
                MenuEntry(open, { lastAction = "$open: $name" }),
                MenuEntry(edit, { lastAction = "$edit: $name" }),
                MenuEntry(delete, { pendingDelete = index }, destructive = true),
            ),
            onClick = { lastAction = "$open (tap): $name" },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    Modifier.fillMaxWidth().padding(Spacing.lg),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(name, style = MaterialTheme.typography.bodyLarge)
                    DemandClassChip(stringResource(R.string.gallery_sample_chip))
                }
            }
        }
    }
    Text(
        stringResource(R.string.gallery_last_action, lastAction ?: stringResource(R.string.gallery_none)),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    pendingDelete?.let { index ->
        val deletedName = stringResource(R.string.gallery_sample_item, index)
        ConfirmationDialog(
            title = stringResource(R.string.gallery_confirm_title),
            body = stringResource(R.string.gallery_confirm_body),
            confirmLabel = deleteLabel,
            destructive = true,
            onConfirm = {
                lastAction = "$deleteLabel: $deletedName"
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun PopupMenuSection() = Section(R.string.gallery_section_popup) {
    var sort by rememberSaveable { mutableStateOf(SortOption.Name) }
    PopupMenuButton(
        label = stringResource(R.string.gallery_sort_label, stringResource(sort.labelRes)),
        options = SortOption.entries,
        selected = sort,
        optionLabel = { stringResource(it.labelRes) },
        onSelected = { sort = it },
    )
}

@Composable
private fun ButtonsSection() = Section(R.string.gallery_section_buttons) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        PrimaryButton(stringResource(R.string.gallery_primary), onClick = {})
        SecondaryButton(stringResource(R.string.gallery_secondary), onClick = {})
        DestructiveButton(stringResource(R.string.gallery_destructive), onClick = { showDialog = true })
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        StatusChip(stringResource(R.string.gallery_status_ready), StatusKind.Success)
        StatusChip(stringResource(R.string.gallery_status_attention), StatusKind.Warning)
        StatusChip(stringResource(R.string.gallery_status_failed), StatusKind.Error)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) { DemoDataBadge() }
    MetricCard(
        label = stringResource(R.string.gallery_metric_label),
        value = stringResource(R.string.gallery_metric_value),
        modifier = Modifier.fillMaxWidth(),
    )
    if (showDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.gallery_confirm_title),
            body = stringResource(R.string.gallery_confirm_body),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = { showDialog = false },
            onDismiss = { showDialog = false },
        )
    }
}
