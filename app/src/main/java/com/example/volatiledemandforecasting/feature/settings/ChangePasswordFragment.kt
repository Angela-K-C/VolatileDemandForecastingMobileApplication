package com.example.volatiledemandforecasting.feature.settings

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.appViewModels
import com.example.volatiledemandforecasting.core.ui.asString
import com.example.volatiledemandforecasting.core.ui.components.InlineMessage
import com.example.volatiledemandforecasting.core.ui.components.PasswordField
import com.example.volatiledemandforecasting.core.ui.components.PrimaryButton
import com.example.volatiledemandforecasting.core.ui.components.StatusKind
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.feature.auth.AuthValidation

class ChangePasswordFragment : ShellFragment() {
    override val titleRes = R.string.screen_change_password

    private val viewModel: ChangePasswordViewModel by appViewModels { ChangePasswordViewModel(it.userRepository) }

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val navController = findNavController()

        LaunchedEffect(Unit) {
            viewModel.done.collect {
                Toast.makeText(context, R.string.cp_done, Toast.LENGTH_LONG).show()
                navController.popBackStack()
            }
        }

        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(stringResource(R.string.cp_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PasswordField(
                state.current, viewModel::setCurrent, stringResource(R.string.cp_current), Modifier.fillMaxWidth(),
                error = state.currentError, enabled = !state.submitting,
            )
            PasswordField(
                state.new, viewModel::setNew, stringResource(R.string.cp_new), Modifier.fillMaxWidth(),
                error = state.newError, hint = stringResource(R.string.auth_password_hint, AuthValidation.MIN_PASSWORD_LENGTH), enabled = !state.submitting,
            )
            PasswordField(
                state.confirm, viewModel::setConfirm, stringResource(R.string.cp_confirm), Modifier.fillMaxWidth(),
                error = state.confirmError, enabled = !state.submitting, imeAction = ImeAction.Done, onImeAction = viewModel::submit,
            )
            state.error?.let { InlineMessage(it.asString(), StatusKind.Error) }
            PrimaryButton(stringResource(R.string.cp_submit), viewModel::submit, Modifier.fillMaxWidth(), loading = state.submitting)
        }
    }
}
