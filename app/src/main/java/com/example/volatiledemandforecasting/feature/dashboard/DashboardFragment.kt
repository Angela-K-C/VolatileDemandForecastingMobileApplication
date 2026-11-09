package com.example.volatiledemandforecasting.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.appViewModels
import com.example.volatiledemandforecasting.app.navigation.MainTab
import com.example.volatiledemandforecasting.app.navigation.navigateToForecastResult
import com.example.volatiledemandforecasting.app.navigation.navigateToTab
import com.example.volatiledemandforecasting.core.ui.Formatting
import com.example.volatiledemandforecasting.core.ui.components.InlineMessage
import com.example.volatiledemandforecasting.core.ui.components.MetricCard
import com.example.volatiledemandforecasting.core.ui.components.Term
import com.example.volatiledemandforecasting.core.ui.components.PrimaryButton
import com.example.volatiledemandforecasting.core.ui.components.SecondaryButton
import com.example.volatiledemandforecasting.core.ui.components.StatusKind
import com.example.volatiledemandforecasting.core.ui.components.UiStateHost
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.data.workspace.canManageData
import com.example.volatiledemandforecasting.feature.forecasts.ForecastRow
import com.example.volatiledemandforecasting.feature.workspace.StoreGate

class DashboardFragment : ShellFragment() {
    override val titleRes = R.string.nav_dashboard
    override val tab = MainTab.Dashboard

    private val viewModel: DashboardViewModel by appViewModels {
        DashboardViewModel(it.storeRepository, it.productRepository, it.forecastRepository, it.trainingRepository, it.batchRepository, it.activeStore)
    }

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val navController = findNavController()
        val selection by collectSelection()
        val state by viewModel.state.collectAsStateWithLifecycle()

        StoreGate(selection, onChoose = { navController.navigate(R.id.workspacePickerFragment) }, modifier = Modifier.padding(padding)) {
            UiStateHost(state, onRetry = viewModel::load, modifier = Modifier.padding(padding), empty = {}) { data ->
                Column(
                    Modifier
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    if (data.store.isReference) InlineMessage(stringResource(R.string.dash_demo_notice), StatusKind.Neutral)
                    if (data.showStaleNotice) {
                        InlineMessage(
                            stringResource(R.string.dash_stale_notice, Formatting.date(data.summary.historyEnd)),
                            StatusKind.Warning,
                        )
                    }
                    val setup = data.setup
                    if (setup != null && setup.showChecklist) {
                        // New shops: one clear next step instead of a dashboard of empty numbers.
                        GettingStartedCard(
                            progress = setup,
                            canManage = data.store.canManageData,
                            aiDays = data.eligibilityProgress,
                            onUpload = { navController.navigate(R.id.importWizardFragment) },
                            onForecast = { navController.navigateToTab(MainTab.Forecasts) },
                            onSetUpAi = { navController.navigate(R.id.modelInfoFragment) },
                        )
                    }
                    SummaryCards(data)
                    if (setup == null || !setup.showChecklist) {
                        // Every product at once, on the Forecasts tab.
                        PrimaryButton(
                            text = stringResource(R.string.dash_generate),
                            onClick = { navController.navigateToTab(MainTab.Forecasts) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (data.store.canManageData) {
                            SecondaryButton(stringResource(R.string.data_upload), { navController.navigate(R.id.importWizardFragment) }, Modifier.fillMaxWidth())
                        }
                    }
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(stringResource(R.string.dash_recent), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    }
                    if (data.recent.isEmpty()) {
                        Text(stringResource(R.string.dash_no_forecast), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    data.recent.forEach { item ->
                        ForecastRow(item, onClick = { navController.navigateToForecastResult(item.forecastId) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCards(data: DashboardData) {
    val aiLabel = stringResource(
        when (data.aiStatus) {
            AiStatus.Available -> R.string.dash_ai_available
            AiStatus.Training -> R.string.dash_ai_training
            AiStatus.NotEligible -> R.string.dash_ai_not_eligible
            AiStatus.BaselinesOnly -> R.string.dash_ai_baselines_only
        }
    )
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            MetricCard(stringResource(R.string.dash_active_products), data.activeProducts.toString(), Modifier.weight(1f))
            MetricCard(
                stringResource(R.string.dash_ai_status), aiLabel, Modifier.weight(1f), info = Term.AiForecast,
                supporting = data.eligibilityProgress?.let {
                    stringResource(R.string.dash_eligibility_days, it.value?.toInt() ?: 0, it.required?.toInt() ?: 0)
                },
            )
        }
        MetricCard(
            label = stringResource(R.string.dash_latest_sales),
            value = Formatting.date(data.summary.historyEnd),
            modifier = Modifier.fillMaxWidth(),
            supporting = data.summary.historyEnd?.let { stringResource(R.string.dash_sales_up_to, Formatting.date(it)) },
        )
        MetricCard(
            label = stringResource(R.string.dash_latest_forecast),
            value = data.latestForecast?.let { Formatting.date(it.createdAt) } ?: stringResource(R.string.dash_no_forecast),
            modifier = Modifier.fillMaxWidth(),
            supporting = data.latestForecast?.productName,
        )
    }
}
