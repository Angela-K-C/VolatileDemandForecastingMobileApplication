package com.example.volatiledemandforecasting.feature.forecasts.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.appViewModels
import com.example.volatiledemandforecasting.app.navigation.ARG_FORECAST_ID
import com.example.volatiledemandforecasting.app.navigation.ARG_JOB_ID
import com.example.volatiledemandforecasting.app.navigation.MainTab
import com.example.volatiledemandforecasting.app.navigation.navigateToForecastResult
import com.example.volatiledemandforecasting.app.navigation.navigateToTab
import com.example.volatiledemandforecasting.core.ui.asString
import com.example.volatiledemandforecasting.core.ui.components.ErrorState
import com.example.volatiledemandforecasting.core.ui.components.PrimaryButton
import com.example.volatiledemandforecasting.core.ui.components.SecondaryButton
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.data.forecast.JobStatus

class ForecastProgressFragment : ShellFragment() {
    override val titleRes = R.string.screen_forecast_progress

    private val viewModel: ForecastProgressViewModel by appViewModels {
        val args = requireArguments()
        ForecastProgressViewModel(
            jobId = args.getString(ARG_JOB_ID).orEmpty(),
            forecastId = args.getString(ARG_FORECAST_ID).orEmpty(),
            forecasts = it.forecastRepository,
        )
    }

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val navController = findNavController()

        LaunchedEffect(Unit) {
            viewModel.completed.collect { forecastId ->
                navController.navigateToForecastResult(forecastId, replacing = R.id.forecastProgressFragment)
            }
        }

        when (val s = state) {
            is ProgressState.Running -> Running(s, Modifier.padding(padding))
            is ProgressState.Failed -> Column(
                Modifier.padding(padding).fillMaxSize().padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // The backend's own explanation; Retry only when it says the job can be retried.
                Text(stringResource(R.string.job_failed_title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text(
                    s.message.asString(),
                    modifier = Modifier.padding(top = Spacing.sm),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (s.retryable) PrimaryButton(stringResource(R.string.job_retry), viewModel::retry, Modifier.padding(top = Spacing.lg))
            }
            ProgressState.TimedOut -> Column(
                Modifier.padding(padding).fillMaxSize().padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.job_timeout), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                SecondaryButton(
                    stringResource(R.string.job_open_forecasts),
                    { navController.navigateToTab(MainTab.Forecasts) },
                    Modifier.padding(top = Spacing.lg),
                )
            }
            is ProgressState.Unreachable -> ErrorState(s.message.asString(), viewModel::startPolling, Modifier.padding(padding))
        }
    }
}

@Composable
private fun Running(state: ProgressState.Running, modifier: Modifier) {
    Column(
        modifier.fillMaxSize().padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Indeterminate: only the stage the backend reports is shown, never an invented percentage.
        CircularProgressIndicator()
        Text(
            stringResource(
                when (state.status) {
                    JobStatus.PREPARING_DATA -> R.string.job_preparing
                    JobStatus.RUNNING_MODEL -> R.string.job_running
                    JobStatus.FINALIZING -> R.string.job_finalizing
                    else -> R.string.job_queued
                }
            ),
            modifier = Modifier.padding(top = Spacing.lg),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            stringResource(R.string.job_leave_hint),
            modifier = Modifier.padding(top = Spacing.sm),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
