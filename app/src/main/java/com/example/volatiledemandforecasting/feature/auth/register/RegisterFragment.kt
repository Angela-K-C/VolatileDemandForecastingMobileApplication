package com.example.volatiledemandforecasting.feature.auth.register

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.app.ComposeFragment
import com.example.volatiledemandforecasting.app.appViewModels

class RegisterFragment : ComposeFragment() {

    private val viewModel: RegisterViewModel by appViewModels { RegisterViewModel(it.authRepository) }

    @Composable
    override fun Content() {
        val state by viewModel.state.collectAsStateWithLifecycle()
        RegisterScreen(
            state = state,
            onDisplayNameChange = viewModel::onDisplayNameChange,
            onEmailChange = viewModel::onEmailChange,
            onPasswordChange = viewModel::onPasswordChange,
            onSubmit = viewModel::submit,
            onBackToSignIn = { findNavController().navigateUp() },
        )
    }
}
