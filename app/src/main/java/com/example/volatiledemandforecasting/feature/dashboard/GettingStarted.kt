package com.example.volatiledemandforecasting.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.ui.components.PrimaryButton
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.data.training.EligibilityCheckDto

/** The steps from a new shop to AI forecasts, in order. */
enum class SetupStep { CreateShop, UploadSales, FirstForecast, AiUnlocked }

/** Where a business shop is on the way to its first forecasts. The sample shop never shows this. */
data class SetupProgress(
    val hasSales: Boolean,
    val hasForecast: Boolean,
    val aiReady: Boolean,
) {
    fun isDone(step: SetupStep): Boolean = when (step) {
        SetupStep.CreateShop -> true
        SetupStep.UploadSales -> hasSales
        SetupStep.FirstForecast -> hasForecast
        SetupStep.AiUnlocked -> aiReady
    }

    /** The first unfinished step: the one the checklist offers a button for. */
    val next: SetupStep? get() = SetupStep.entries.firstOrNull { !isDone(it) }

    val doneCount: Int get() = SetupStep.entries.count(::isDone)

    /**
     * Shown until the shop has sales and a forecast. AI unlocking takes months and needs nothing
     * from the user, so it alone doesn't keep the checklist on screen; the AI card covers it.
     */
    val showChecklist: Boolean get() = !hasSales || !hasForecast
}

/**
 * The "Getting started" card on the dashboard. [canUpload] is false for viewers, who are asked to
 * get an owner or manager to upload instead of being shown a button they can't use.
 */
@Composable
fun GettingStartedCard(
    progress: SetupProgress,
    canUpload: Boolean,
    aiDays: EligibilityCheckDto?,
    onUpload: () -> Unit,
    onForecast: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = SetupStep.entries.size
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(stringResource(R.string.gs_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.gs_progress, progress.doneCount, total),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { progress.doneCount / total.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            SetupStep.entries.forEach { step ->
                val isNext = step == progress.next
                StepRow(
                    title = stringResource(stepTitle(step)),
                    body = stepBody(step, canUpload, aiDays).takeIf { isNext || (step == SetupStep.AiUnlocked && !progress.isDone(step)) },
                    done = progress.isDone(step),
                    highlighted = isNext,
                )
                if (isNext) {
                    when (step) {
                        SetupStep.UploadSales -> if (canUpload) {
                            PrimaryButton(stringResource(R.string.data_upload), onUpload, Modifier.fillMaxWidth())
                        }
                        SetupStep.FirstForecast -> PrimaryButton(stringResource(R.string.dash_generate), onForecast, Modifier.fillMaxWidth())
                        else -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun StepRow(title: String, body: String?, done: Boolean, highlighted: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.Top) {
        Icon(
            if (done) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = stringResource(if (done) R.string.gs_done else R.string.gs_to_do),
            tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(22.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal,
                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            if (body != null) {
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun stepTitle(step: SetupStep): Int = when (step) {
    SetupStep.CreateShop -> R.string.gs_step_shop
    SetupStep.UploadSales -> R.string.gs_step_upload
    SetupStep.FirstForecast -> R.string.gs_step_forecast
    SetupStep.AiUnlocked -> R.string.gs_step_ai
}

@Composable
private fun stepBody(step: SetupStep, canUpload: Boolean, aiDays: EligibilityCheckDto?): String? = when (step) {
    SetupStep.CreateShop -> null
    SetupStep.UploadSales -> stringResource(if (canUpload) R.string.gs_upload_body else R.string.gs_upload_body_viewer)
    SetupStep.FirstForecast -> stringResource(R.string.gs_forecast_body)
    SetupStep.AiUnlocked -> {
        val have = aiDays?.value?.toInt()
        val need = aiDays?.required?.toInt()
        if (have != null && need != null) stringResource(R.string.gs_ai_body_days, have, need)
        else stringResource(R.string.gs_ai_body)
    }
}
