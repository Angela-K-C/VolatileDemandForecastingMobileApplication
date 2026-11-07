package com.example.volatiledemandforecasting.feature.auth.forgot

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
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
fun ForgotPasswordScreen(
    state: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onRequestCode: () -> Unit,
    onResetPassword: () -> Unit,
    onUseDifferentEmail: () -> Unit,
    onBackToSignIn: () -> Unit,
) {
    AuthScaffold(
        title = stringResource(R.string.forgot_title),
        subtitle = if (state.step == ForgotPasswordStep.EnterEmail) stringResource(R.string.forgot_request_body) else null,
    ) {
        state.error?.let { InlineMessage(it.asString(), StatusKind.Error) }

        when (state.step) {
            ForgotPasswordStep.EnterEmail -> {
                AppTextField(
                    value = state.email,
                    onValueChange = onEmailChange,
                    label = stringResource(R.string.auth_email),
                    modifier = Modifier.fillMaxWidth(),
                    error = state.emailError,
                    enabled = !state.isSubmitting,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                    onImeAction = onRequestCode,
                )
                PrimaryButton(
                    text = stringResource(R.string.forgot_send_code),
                    onClick = onRequestCode,
                    modifier = Modifier.fillMaxWidth(),
                    loading = state.isSubmitting,
                )
            }

            ForgotPasswordStep.EnterCode -> {
                // Same wording whether or not the account exists, as the backend's answer is.
                InlineMessage(stringResource(R.string.forgot_code_sent, state.email.trim()), StatusKind.Neutral)
                AppTextField(
                    value = state.code,
                    onValueChange = onCodeChange,
                    label = stringResource(R.string.forgot_code_label),
                    modifier = Modifier.fillMaxWidth(),
                    error = state.codeError,
                    enabled = !state.isSubmitting,
                    keyboardType = KeyboardType.NumberPassword,
                )
                PasswordField(
                    value = state.newPassword,
                    onValueChange = onNewPasswordChange,
                    label = stringResource(R.string.forgot_new_password),
                    modifier = Modifier.fillMaxWidth(),
                    error = state.passwordError,
                    hint = stringResource(R.string.auth_password_hint, AuthValidation.MIN_PASSWORD_LENGTH),
                    enabled = !state.isSubmitting,
                    imeAction = ImeAction.Done,
                    onImeAction = onResetPassword,
                )
                PrimaryButton(
                    text = stringResource(R.string.forgot_reset_button),
                    onClick = onResetPassword,
                    modifier = Modifier.fillMaxWidth(),
                    loading = state.isSubmitting,
                )
                TextButton(onClick = onRequestCode, enabled = !state.isSubmitting) {
                    Text(stringResource(R.string.forgot_resend))
                }
                TextButton(onClick = onUseDifferentEmail, enabled = !state.isSubmitting) {
                    Text(stringResource(R.string.forgot_change_email))
                }
            }
        }
        TextButton(onClick = onBackToSignIn) { Text(stringResource(R.string.back_to_sign_in)) }
    }
}
