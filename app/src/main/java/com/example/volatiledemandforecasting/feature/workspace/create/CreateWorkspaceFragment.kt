package com.example.volatiledemandforecasting.feature.workspace.create

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.appViewModels
import com.example.volatiledemandforecasting.core.ui.asString
import com.example.volatiledemandforecasting.core.ui.components.AppTextField
import com.example.volatiledemandforecasting.core.ui.components.InlineMessage
import com.example.volatiledemandforecasting.core.ui.components.PrimaryButton
import com.example.volatiledemandforecasting.core.ui.components.StatusKind
import com.example.volatiledemandforecasting.core.ui.theme.Spacing

class CreateWorkspaceFragment : ShellFragment() {
    override val titleRes = R.string.screen_create_workspace

    private val viewModel: CreateWorkspaceViewModel by appViewModels { CreateWorkspaceViewModel(it.workspaceRepository, it.activeStore) }

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val navController = findNavController()

        // Both the picker and this form are behind us once the store exists and is selected.
        LaunchedEffect(Unit) {
            viewModel.done.collect {
                navController.popBackStack(R.id.workspacePickerFragment, inclusive = true)
                    .also { popped -> if (!popped) navController.popBackStack() }
            }
        }

        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (state.createdWorkspace == null) {
                Text(stringResource(R.string.create_workspace_section), style = MaterialTheme.typography.titleMedium)
                AppTextField(
                    state.workspaceName,
                    { v -> viewModel.edit { copy(workspaceName = v, errors = errors - StoreField.WorkspaceName) } },
                    stringResource(R.string.create_workspace_name),
                    Modifier.fillMaxWidth(),
                    error = state.errors[StoreField.WorkspaceName],
                    capitalization = KeyboardCapitalization.Words,
                    enabled = !state.submitting,
                )
            } else {
                InlineMessage(stringResource(R.string.create_workspace_made, state.createdWorkspace!!.name), StatusKind.Success)
            }

            Text(stringResource(R.string.create_store_section), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = Spacing.sm))
            AppTextField(
                state.storeName,
                { v -> viewModel.edit { copy(storeName = v, errors = errors - StoreField.StoreName) } },
                stringResource(R.string.create_store_name),
                Modifier.fillMaxWidth(),
                error = state.errors[StoreField.StoreName],
                capitalization = KeyboardCapitalization.Words,
                enabled = !state.submitting,
            )
            AppTextField(
                state.code,
                { v -> viewModel.edit { copy(code = v, errors = errors - StoreField.Code) } },
                stringResource(R.string.create_store_code),
                Modifier.fillMaxWidth(),
                error = state.errors[StoreField.Code],
                hint = stringResource(R.string.create_store_code_hint),
                capitalization = KeyboardCapitalization.Characters,
                enabled = !state.submitting,
            )
            AppTextField(
                state.country,
                { v -> viewModel.edit { copy(country = v, errors = errors - StoreField.Country) } },
                stringResource(R.string.create_country),
                Modifier.fillMaxWidth(),
                error = state.errors[StoreField.Country],
                hint = stringResource(R.string.create_country_hint),
                capitalization = KeyboardCapitalization.Characters,
                enabled = !state.submitting,
            )
            AppTextField(
                state.subdivision,
                { v -> viewModel.edit { copy(subdivision = v) } },
                stringResource(R.string.create_subdivision),
                Modifier.fillMaxWidth(),
                hint = stringResource(R.string.create_subdivision_hint),
                capitalization = KeyboardCapitalization.Characters,
                enabled = !state.submitting,
            )
            AppTextField(
                state.timezone,
                { v -> viewModel.edit { copy(timezone = v, errors = errors - StoreField.Timezone) } },
                stringResource(R.string.create_timezone),
                Modifier.fillMaxWidth(),
                error = state.errors[StoreField.Timezone],
                hint = stringResource(R.string.create_timezone_hint),
                enabled = !state.submitting,
            )
            AppTextField(
                state.currency,
                { v -> viewModel.edit { copy(currency = v, errors = errors - StoreField.Currency) } },
                stringResource(R.string.create_currency),
                Modifier.fillMaxWidth(),
                error = state.errors[StoreField.Currency],
                capitalization = KeyboardCapitalization.Characters,
                enabled = !state.submitting,
            )
            state.error?.let { InlineMessage(it.asString(), StatusKind.Error) }
            PrimaryButton(
                stringResource(R.string.create_submit),
                viewModel::submit,
                Modifier.fillMaxWidth(),
                loading = state.submitting,
            )
        }
    }
}
