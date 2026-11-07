package com.example.volatiledemandforecasting.feature.forecasts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.ui.Formatting
import com.example.volatiledemandforecasting.core.ui.components.StatusChip
import com.example.volatiledemandforecasting.core.ui.components.StatusKind
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.data.forecast.ForecastMethod
import com.example.volatiledemandforecasting.data.forecast.ForecastSummaryDto
import com.example.volatiledemandforecasting.data.forecast.JobStatus

@Composable
fun methodLabel(method: String): String = stringResource(
    when (method) {
        ForecastMethod.AI -> R.string.hist_method_ai
        ForecastMethod.BASELINE -> R.string.hist_method_baseline
        else -> R.string.hist_method_compare
    }
)

@Composable
fun horizonLabel(days: Int): String =
    if (days == 1) stringResource(R.string.cfg_horizon_next_day) else stringResource(R.string.cfg_horizon_days, days)

/** One saved forecast. Tags spell out what it is (replay, actuals available, failed) in words. */
@Composable
fun ForecastRow(item: ForecastSummaryDto, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    Card(
        modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(item.productName, style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.hist_summary, horizonLabel(item.horizonDays), methodLabel(item.method), Formatting.date(item.asOfDate)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                if (item.isReplay) StatusChip(stringResource(R.string.res_replay_tag), StatusKind.Neutral)
                if (item.actualsAvailable) StatusChip(stringResource(R.string.hist_actuals_now), StatusKind.Success)
                when (item.status) {
                    JobStatus.FAILED -> StatusChip(stringResource(R.string.hist_status_failed), StatusKind.Error)
                    JobStatus.COMPLETED -> {}
                    else -> StatusChip(stringResource(R.string.hist_status_running), StatusKind.Warning)
                }
            }
        }
    }
}
