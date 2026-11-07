package com.example.volatiledemandforecasting.feature.auth.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ComposeFragment
import com.example.volatiledemandforecasting.app.appViewModels

class LoginFragment : ComposeFragment() {

    private val viewModel: LoginViewModel by appViewModels { LoginViewModel(it.authRepository, it.session) }

    @Composable
    override fun Content() {
        val state by viewModel.state.collectAsStateWithLifecycle()
        LoginScreen(
            state = state,
            onEmailChange = viewModel::onEmailChange,
            onPasswordChange = viewModel::onPasswordChange,
            onSubmit = viewModel::submit,
            onForgotPassword = { findNavController().navigate(R.id.forgotPasswordFragment) },
            onCreateAccount = { findNavController().navigate(R.id.registerFragment) },
        )
    }
}
