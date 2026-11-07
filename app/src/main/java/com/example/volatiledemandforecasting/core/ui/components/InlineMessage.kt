package com.example.volatiledemandforecasting.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.core.ui.theme.appColors

/** A message inside a form: an error, a notice, or a confirmation. Screen readers announce it. */
@Composable
fun InlineMessage(text: String, kind: StatusKind, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    val (container, content) = when (kind) {
        StatusKind.Success -> colors.successContainer to colors.onSuccessContainer
        StatusKind.Warning -> colors.warningContainer to colors.onWarningContainer
        StatusKind.Error -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        StatusKind.Neutral -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive },
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content,
    ) {
        Text(text, Modifier.padding(Spacing.md), style = MaterialTheme.typography.bodyMedium)
    }
}
