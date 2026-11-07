package com.example.volatiledemandforecasting.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.core.ui.theme.appColors

enum class StatusKind { Success, Warning, Error, Neutral }

/** Status is always spelled out in text, never colour alone (spec §16). */
@Composable
fun StatusChip(text: String, kind: StatusKind, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    val (container, content) = when (kind) {
        StatusKind.Success -> colors.successContainer to colors.onSuccessContainer
        StatusKind.Warning -> colors.warningContainer to colors.onWarningContainer
        StatusKind.Error -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        StatusKind.Neutral -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Pill(text, container, content, modifier)
}

/** Shows the API's plain-language `demandClass.label`, never the class code. */
@Composable
fun DemandClassChip(label: String, modifier: Modifier = Modifier) =
    Pill(label, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, modifier)

/** Marks the read-only Reference workspace. Its own colour so it is never mistaken for a status. */
@Composable
fun DemoDataBadge(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Pill(stringResource(R.string.demo_data_badge), colors.demoBadge, colors.onDemoBadge, modifier)
}

@Composable
private fun Pill(text: String, container: Color, content: Color, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(50), color = container, contentColor = content) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/** A labelled number from the API. The caller formats it; this never computes anything. */
@Composable
fun MetricCard(label: String, value: String, modifier: Modifier = Modifier, supporting: String? = null) {
    Card(
        modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(Spacing.lg)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = Spacing.xs))
            if (supporting != null) {
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }
        }
    }
}
