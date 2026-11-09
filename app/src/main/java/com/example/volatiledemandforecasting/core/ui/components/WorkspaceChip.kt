package com.example.volatiledemandforecasting.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.ui.theme.Spacing

/** What the chip shows. `null` means no workspace has been chosen yet. */
data class WorkspaceChipUi(val label: String, val isDemo: Boolean)

/**
 * Always tells the user which workspace and store they are in, and marks the Reference
 * workspace as demo data (spec §5.1). Tapping it switches workspace.
 */
@Composable
fun WorkspaceChip(workspace: WorkspaceChipUi?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // The backend names it after the M5 store; the app calls it the sample shop everywhere.
    val label = when {
        workspace == null -> stringResource(R.string.workspace_none)
        workspace.isDemo -> stringResource(R.string.sample_shop_name)
        else -> workspace.label
    }
    val description = stringResource(R.string.workspace_chip_description, label)
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        AssistChip(
            onClick = onClick,
            label = { Text(label) },
            leadingIcon = { Icon(Icons.Outlined.Storefront, contentDescription = null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.semantics { contentDescription = description },
        )
        if (workspace?.isDemo == true) DemoDataBadge()
    }
}
