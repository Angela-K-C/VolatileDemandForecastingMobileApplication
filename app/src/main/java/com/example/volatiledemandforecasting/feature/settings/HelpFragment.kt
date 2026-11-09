package com.example.volatiledemandforecasting.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.core.ui.components.SecondaryButton
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.feature.onboarding.ARG_TOUR_REPLAY
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController

/** Getting started, the app tour again, and plain explanations of how forecasts work and what the app does with data (spec §8.12). */
class HelpFragment : ShellFragment() {
    override val titleRes = R.string.screen_help

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val navController = findNavController()
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(stringResource(R.string.help_start_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.help_start_body), style = MaterialTheme.typography.bodyMedium)
            SecondaryButton(
                stringResource(R.string.help_show_tour),
                { navController.navigate(R.id.onboardingFragment, bundleOf(ARG_TOUR_REPLAY to true)) },
                Modifier.fillMaxWidth(),
            )
            listOf(
                R.string.help_file_title to R.string.help_file_body,
                R.string.help_forecasts_title to R.string.help_forecasts_body,
                R.string.help_ai_title to R.string.help_ai_body,
                R.string.help_limits_title to R.string.help_limits_body,
                R.string.help_privacy_title to R.string.help_privacy_body,
            ).forEach { (title, body) ->
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
