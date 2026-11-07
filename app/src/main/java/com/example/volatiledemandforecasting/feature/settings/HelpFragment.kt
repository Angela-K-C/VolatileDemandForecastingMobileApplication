package com.example.volatiledemandforecasting.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.example.volatiledemandforecasting.core.ui.theme.Spacing

/** Plain explanations of how forecasts work and what the app does with data (spec §8.12). */
class HelpFragment : ShellFragment() {
    override val titleRes = R.string.screen_help

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            listOf(
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
