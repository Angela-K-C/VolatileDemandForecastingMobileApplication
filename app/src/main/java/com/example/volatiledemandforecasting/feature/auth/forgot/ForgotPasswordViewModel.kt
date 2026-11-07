package com.example.volatiledemandforecasting.feature.auth.forgot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volatiledemandforecasting.core.network.attempt
import com.example.volatiledemandforecasting.core.network.toUiText
import com.example.volatiledemandforecasting.core.ui.UiText
import com.example.volatiledemandforecasting.data.auth.AuthRepository
import com.example.volatiledemandforecasting.feature.auth.AuthValidation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ForgotPasswordStep { EnterEmail, EnterCode }

data class ForgotPasswordUiState(
    val step: ForgotPasswordStep = ForgotPasswordStep.EnterEmail,
    val email: String = "",
    val code: String = "",
    val newPassword: String = "",
    val emailError: UiText? = null,
    val codeError: UiText? = null,
    val passwordError: UiText? = null,
    val isSubmitting: Boolean = false,
    val error: UiText? = null,
)

/**
 * Two steps (api.md, "Account"): ask for a code, then send the code with a new password. The
 * backend answers the first step the same way whether or not the account exists, and a successful
 * second step signs the user in, which `MainActivity` picks up.
 */
class ForgotPasswordViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, emailError = null, error = null) }

    fun onCodeChange(value: String) {
        // Digits only: the code is 8 digits, and this also strips spaces from a pasted code.
        val digits = value.filter(Char::isDigit).take(AuthValidation.RESET_CODE_LENGTH)
        _state.update { it.copy(code = digits, codeError = null, error = null) }
    }

    fun onNewPasswordChange(value: String) = _state.update { it.copy(newPassword = value, passwordError = null, error = null) }

    fun useDifferentEmail() = _state.update {
        it.copy(step = ForgotPasswordStep.EnterEmail, code = "", newPassword = "", codeError = null, passwordError = null, error = null)
    }

    /** Also used to ask for a new code. Only the newest code works. */
    fun requestCode() {
        val current = _state.value
        if (current.isSubmitting) return

        val emailError = AuthValidation.email(current.email)
        if (emailError != null) {
            _state.update { it.copy(emailError = emailError) }
            return
        }

        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            attempt { auth.requestPasswordReset(current.email) }
                .onSuccess {
                    _state.update { it.copy(step = ForgotPasswordStep.EnterCode, code = "", codeError = null) }
                }
                .onFailure { e ->
                    val fieldError = AuthValidation.serverEmailError(e)
                    _state.update {
                        if (fieldError != null) it.copy(emailError = fieldError) else it.copy(error = e.toUiText())
                    }
                }
            _state.update { it.copy(isSubmitting = false) }
        }
    }

    fun resetPassword() {
        val current = _state.value
        if (current.isSubmitting) return

        val codeError = AuthValidation.resetCode(current.code)
        val passwordError = AuthValidation.newPassword(current.newPassword)
        if (codeError != null || passwordError != null) {
            _state.update { it.copy(codeError = codeError, passwordError = passwordError) }
            return
        }

        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            attempt { auth.confirmPasswordReset(current.email, current.code, current.newPassword) }
                .onFailure { e -> _state.update { it.copy(error = e.toUiText()) } }
            _state.update { it.copy(isSubmitting = false) }
        }
    }
}
