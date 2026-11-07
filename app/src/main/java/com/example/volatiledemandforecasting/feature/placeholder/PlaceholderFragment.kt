package com.example.volatiledemandforecasting.feature.placeholder

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.navigation.ARG_PLACEHOLDER_STEP
import com.example.volatiledemandforecasting.app.navigation.ARG_PLACEHOLDER_TITLE
import com.example.volatiledemandforecasting.core.ui.components.EmptyState

/** Stands in for a supporting screen that a later build step provides. */
class PlaceholderFragment : ShellFragment() {
    override val titleRes: Int get() = requireArguments().getInt(ARG_PLACEHOLDER_TITLE)

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        NotBuiltYet(step = requireArguments().getInt(ARG_PLACEHOLDER_STEP), modifier = Modifier.padding(padding))
    }
}

/** Shown on screens whose build step has not happened yet. It displays no data of any kind. */
@Composable
fun NotBuiltYet(step: Int, modifier: Modifier = Modifier) {
    EmptyState(
        title = stringResource(R.string.not_built_title),
        body = stringResource(R.string.not_built_body, step),
        modifier = modifier,
    )
}
