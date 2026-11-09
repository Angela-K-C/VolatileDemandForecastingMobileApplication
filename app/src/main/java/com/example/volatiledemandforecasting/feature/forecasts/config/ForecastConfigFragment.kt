package com.example.volatiledemandforecasting.feature.forecasts.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.appViewModels
import com.example.volatiledemandforecasting.app.navigation.ARG_HORIZON
import com.example.volatiledemandforecasting.app.navigation.ARG_PRODUCT_ID
import com.example.volatiledemandforecasting.app.navigation.navigateToForecastProgress
import com.example.volatiledemandforecasting.core.ui.Formatting
import com.example.volatiledemandforecasting.core.ui.asString
import com.example.volatiledemandforecasting.core.ui.components.DemandClassChip
import com.example.volatiledemandforecasting.core.ui.components.InlineMessage
import com.example.volatiledemandforecasting.core.ui.components.PrimaryButton
import com.example.volatiledemandforecasting.core.ui.components.StatusKind
import com.example.volatiledemandforecasting.core.ui.components.UiStateHost
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.data.forecast.ForecastMethod
import com.example.volatiledemandforecasting.data.workspace.canGenerateForecasts
import com.example.volatiledemandforecasting.feature.forecasts.horizonLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ForecastConfigFragment : ShellFragment() {
    override val titleRes = R.string.screen_forecast_config

    private val viewModel: ForecastConfigViewModel by appViewModels {
        val args = requireArguments()
        ForecastConfigViewModel(
            productId = args.getString(ARG_PRODUCT_ID).orEmpty(),
            presetHorizon = if (args.containsKey(ARG_HORIZON)) args.getInt(ARG_HORIZON) else null,
            products = it.productRepository,
            stores = it.storeRepository,
            forecasts = it.forecastRepository,
            activeStore = it.activeStore,
        )
    }

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val form by viewModel.form.collectAsStateWithLifecycle()
        val navController = findNavController()

        LaunchedEffect(Unit) {
            viewModel.created.collect { job ->
                navController.navigateToForecastProgress(job.jobId, job.forecastId, replacing = R.id.forecastConfigFragment)
            }
        }

        UiStateHost(state, onRetry = viewModel::load, modifier = Modifier.padding(padding), empty = {}) { data ->
            Column(
                Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(data.product.name, style = MaterialTheme.typography.titleLarge)
                    data.product.demandClass?.label?.let { DemandClassChip(it) }
                }
                if (!data.store.canGenerateForecasts) {
                    InlineMessage(stringResource(R.string.cfg_viewer), StatusKind.Warning)
                }

                Text(
                    stringResource(R.string.cfg_as_of_fixed, Formatting.date(data.capabilities.asOf.default)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Section(stringResource(R.string.cfg_horizon)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        data.capabilities.horizons.forEach { days ->
                            FilterChip(
                                selected = form.horizon == days,
                                onClick = { viewModel.setHorizon(days) },
                                label = { Text(horizonLabel(days)) },
                            )
                        }
                    }
                }

                // The app picks the method: AI alongside the simple forecast when the store has a model.
                if (!data.capabilities.aiAvailable) {
                    InlineMessage(aiReasonText(data.capabilities.aiUnavailableReason), StatusKind.Neutral)
                }

                form.error?.let { InlineMessage(it.asString(), StatusKind.Error) }

                PrimaryButton(
                    text = stringResource(R.string.cfg_generate),
                    onClick = viewModel::submit,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = data.store.canGenerateForecasts && form.horizon != null,
                    loading = form.submitting,
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        content()
    }
}

/** The API's `aiUnavailableReason` in plain language. */
@Composable
fun aiReasonText(reason: String?): String = stringResource(
    when (reason) {
        "NOT_TRAINED" -> R.string.ai_reason_not_trained
        "TRAINING" -> R.string.ai_reason_training
        "NOT_ELIGIBLE" -> R.string.ai_reason_not_eligible
        "GATE_FAILED" -> R.string.ai_reason_gate_failed
        "NO_HISTORY" -> R.string.ai_reason_no_history
        "MODEL_MISSING" -> R.string.ai_reason_model_missing
        else -> R.string.ai_reason_other
    }
)
