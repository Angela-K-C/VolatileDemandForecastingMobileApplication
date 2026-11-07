package com.example.volatiledemandforecasting.feature.research

import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.appViewModels
import com.example.volatiledemandforecasting.core.ui.asString
import com.example.volatiledemandforecasting.core.ui.components.AppTextField
import com.example.volatiledemandforecasting.core.ui.components.EmptyState
import com.example.volatiledemandforecasting.core.ui.components.PrimaryButton
import com.example.volatiledemandforecasting.core.ui.components.SecondaryButton
import com.example.volatiledemandforecasting.core.ui.components.UiStateHost
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.data.research.ResearchBlockDto
import com.example.volatiledemandforecasting.feature.forecasts.MetricColumn
import com.example.volatiledemandforecasting.feature.forecasts.MetricsBlock
import com.example.volatiledemandforecasting.feature.workspace.StoreGate

class ResearchFragment : ShellFragment() {
    override val titleRes = R.string.screen_research

    private val viewModel: ResearchViewModel by appViewModels { ResearchViewModel(it.researchRepository, it.activeStore) }

    private val createDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) viewModel.export { requireContext().contentResolver.openOutputStream(uri) }
    }

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val navController = findNavController()
        val selection by collectSelection()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val filter by viewModel.filter.collectAsStateWithLifecycle()
        val exporting by viewModel.exporting.collectAsStateWithLifecycle()
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            viewModel.events.collect { event ->
                val text = when (event) {
                    ResearchEvent.Exported -> context.getString(R.string.res_export_done)
                    is ResearchEvent.ExportFailed -> event.message.asString(context)
                }
                Toast.makeText(context, text, Toast.LENGTH_LONG).show()
            }
        }

        StoreGate(selection, onChoose = { navController.navigate(R.id.workspacePickerFragment) }, modifier = Modifier.padding(padding)) {
            UiStateHost(
                state, onRetry = viewModel::load, modifier = Modifier.padding(padding),
                empty = { EmptyState(stringResource(R.string.research_empty_title), stringResource(R.string.research_empty_body)) },
            ) { summary ->
                LazyColumn(
                    contentPadding = PaddingValues(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Text(stringResource(R.string.research_filter), style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                AppTextField(filter.from, viewModel::setFrom, stringResource(R.string.research_from), Modifier.weight(1f), error = filter.fromError)
                                AppTextField(filter.to, viewModel::setTo, stringResource(R.string.research_to), Modifier.weight(1f), error = filter.toError)
                            }
                            Text(stringResource(R.string.research_filter_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            SecondaryButton(stringResource(R.string.research_apply), viewModel::apply, Modifier.fillMaxWidth())
                        }
                    }
                    if (summary.overall.n == 0) {
                        item { Text(stringResource(R.string.research_none_in_range), style = MaterialTheme.typography.bodyMedium) }
                    } else {
                        item { Text(stringResource(R.string.research_counts, summary.runs, summary.overall.n), style = MaterialTheme.typography.bodyMedium) }
                        item { Block(stringResource(R.string.research_overall), summary.overall) }
                        if (summary.byDemandClass.isNotEmpty()) {
                            item { Text(stringResource(R.string.research_by_class), style = MaterialTheme.typography.titleMedium) }
                            summary.byDemandClass.entries.sortedBy { it.key }.forEach { (name, b) ->
                                item(key = "class-$name") { Block(name.lowercase().replaceFirstChar { it.uppercase() }, b) }
                            }
                        }
                        if (summary.byDayAhead.isNotEmpty()) {
                            item { Text(stringResource(R.string.research_by_day), style = MaterialTheme.typography.titleMedium) }
                            summary.byDayAhead.entries.sortedBy { it.key.toIntOrNull() ?: Int.MAX_VALUE }.forEach { (day, b) ->
                                item(key = "day-$day") { Block(stringResource(R.string.research_day_n, day), b) }
                            }
                        }
                        summary.note?.let { item { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    }
                    item {
                        PrimaryButton(
                            stringResource(R.string.research_export), { createDocument.launch("research-export.csv") },
                            Modifier.fillMaxWidth(), loading = exporting,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun Block(title: String, b: ResearchBlockDto) {
        MetricsBlock(
            title = title,
            body = stringResource(R.string.research_block_body, b.n),
            columns = listOf(
                MetricColumn(stringResource(R.string.research_ai), b.ai),
                MetricColumn(stringResource(R.string.research_naive), b.naiveLastValue),
                MetricColumn(stringResource(R.string.research_seasonal), b.seasonalNaive7),
            ),
        )
    }
}
