package com.example.volatiledemandforecasting.feature.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.network.attempt
import com.example.volatiledemandforecasting.core.network.toUiText
import com.example.volatiledemandforecasting.core.security.SessionManager
import com.example.volatiledemandforecasting.core.security.SessionState
import com.example.volatiledemandforecasting.core.security.SignOutReason
import com.example.volatiledemandforecasting.core.ui.UiText
import com.example.volatiledemandforecasting.data.auth.AuthRepository
import com.example.volatiledemandforecasting.feature.auth.AuthValidation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: UiText? = null,
    val passwordError: UiText? = null,
    val isSubmitting: Boolean = false,
    /** From the last attempt, e.g. wrong password or rate limited. */
    val error: UiText? = null,
    /** Why the user is back here, e.g. the session ended. */
    val notice: UiText? = null,
)

/** Signing in changes the session; `MainActivity` sees that and leaves this screen. */
class LoginViewModel(
    private val auth: AuthRepository,
    session: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState(notice = consumeNotice(session)))
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, emailError = null, error = null) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, passwordError = null, error = null) }

    fun submit() {
        val current = _state.value
        if (current.isSubmitting) return

        val emailError = AuthValidation.email(current.email)
        val passwordError = AuthValidation.existingPassword(current.password)
        if (emailError != null || passwordError != null) {
            _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        _state.update { it.copy(isSubmitting = true, error = null, notice = null) }
        viewModelScope.launch {
            attempt { auth.login(current.email, current.password) }
                .onFailure { e ->
                    val fieldError = AuthValidation.serverEmailError(e)
                    _state.update {
                        if (fieldError != null) it.copy(emailError = fieldError) else it.copy(error = e.toUiText())
                    }
                }
            _state.update { it.copy(isSubmitting = false) }
        }
    }

    private fun consumeNotice(session: SessionManager): UiText? {
        val reason = (session.state.value as? SessionState.SignedOut)?.reason ?: return null
        session.acknowledgeSignOutReason()
        return when (reason) {
            SignOutReason.SessionExpired -> UiText.Res(R.string.login_notice_session_expired)
        }
    }
}
