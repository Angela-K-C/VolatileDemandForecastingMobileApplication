package com.example.volatiledemandforecasting.feature.auth.forgot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.app.ComposeFragment
import com.example.volatiledemandforecasting.app.appViewModels

class ForgotPasswordFragment : ComposeFragment() {

    private val viewModel: ForgotPasswordViewModel by appViewModels { ForgotPasswordViewModel(it.authRepository) }

    @Composable
    override fun Content() {
        val state by viewModel.state.collectAsStateWithLifecycle()
        ForgotPasswordScreen(
            state = state,
            onEmailChange = viewModel::onEmailChange,
            onCodeChange = viewModel::onCodeChange,
            onNewPasswordChange = viewModel::onNewPasswordChange,
            onRequestCode = viewModel::requestCode,
            onResetPassword = viewModel::resetPassword,
            onUseDifferentEmail = viewModel::useDifferentEmail,
            onBackToSignIn = { findNavController().navigateUp() },
        )
    }
}
