package com.example.volatiledemandforecasting.feature.auth.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.ui.asString
import com.example.volatiledemandforecasting.core.ui.components.AppTextField
import com.example.volatiledemandforecasting.core.ui.components.InlineMessage
import com.example.volatiledemandforecasting.core.ui.components.PasswordField
import com.example.volatiledemandforecasting.core.ui.components.PrimaryButton
import com.example.volatiledemandforecasting.core.ui.components.StatusKind
import com.example.volatiledemandforecasting.feature.auth.AuthScaffold
import com.example.volatiledemandforecasting.feature.auth.AuthValidation

@Composable
fun RegisterScreen(
    state: RegisterUiState,
    onDisplayNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBackToSignIn: () -> Unit,
) {
    AuthScaffold(title = stringResource(R.string.register_title)) {
        state.error?.let { InlineMessage(it.asString(), StatusKind.Error) }

        AppTextField(
            value = state.displayName,
            onValueChange = onDisplayNameChange,
            label = stringResource(R.string.auth_display_name),
            modifier = Modifier.fillMaxWidth(),
            error = state.displayNameError,
            enabled = !state.isSubmitting,
            capitalization = KeyboardCapitalization.Words,
        )
        AppTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.auth_email),
            modifier = Modifier.fillMaxWidth(),
            error = state.emailError,
            enabled = !state.isSubmitting,
            keyboardType = KeyboardType.Email,
        )
        PasswordField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.auth_password),
            modifier = Modifier.fillMaxWidth(),
            error = state.passwordError,
            hint = stringResource(R.string.auth_password_hint, AuthValidation.MIN_PASSWORD_LENGTH),
            enabled = !state.isSubmitting,
            imeAction = ImeAction.Done,
            onImeAction = onSubmit,
        )
        PrimaryButton(
            text = stringResource(R.string.register_button),
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            loading = state.isSubmitting,
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(
                stringResource(R.string.register_have_account),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onBackToSignIn) { Text(stringResource(R.string.login_button)) }
        }
    }
}
