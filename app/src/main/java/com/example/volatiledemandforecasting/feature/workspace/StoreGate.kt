package com.example.volatiledemandforecasting.feature.workspace

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.datastore.StoreSelection
import com.example.volatiledemandforecasting.core.ui.components.EmptyState
import com.example.volatiledemandforecasting.core.ui.components.LoadingState
import com.example.volatiledemandforecasting.core.ui.components.PrimaryButton
import com.example.volatiledemandforecasting.data.workspace.Selection

/** Store-scoped screens show this until the user has chosen a workspace and store. */
@Composable
fun StoreGate(
    selection: Selection,
    onChoose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (StoreSelection) -> Unit,
) {
    when (selection) {
        Selection.Loading -> LoadingState(modifier)
        Selection.None -> EmptyState(
            title = stringResource(R.string.picker_none_title),
            body = stringResource(R.string.picker_none_body),
            modifier = modifier,
            action = { PrimaryButton(stringResource(R.string.picker_choose_action), onChoose) },
        )
        is Selection.Selected -> content(selection.store)
    }
}
