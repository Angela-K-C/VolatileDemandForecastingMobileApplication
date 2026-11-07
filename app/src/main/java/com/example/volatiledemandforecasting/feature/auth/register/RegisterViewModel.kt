package com.example.volatiledemandforecasting.feature.auth.register

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

data class RegisterUiState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val displayNameError: UiText? = null,
    val emailError: UiText? = null,
    val passwordError: UiText? = null,
    val isSubmitting: Boolean = false,
    val error: UiText? = null,
)

/**
 * Self-registration may be switched off on the server. The backend answers `REGISTRATION_CLOSED`
 * in that case, and the app shows it rather than guessing in advance.
 */
class RegisterViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun onDisplayNameChange(value: String) = _state.update { it.copy(displayName = value, displayNameError = null, error = null) }

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, emailError = null, error = null) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, passwordError = null, error = null) }

    fun submit() {
        val current = _state.value
        if (current.isSubmitting) return

        val nameError = AuthValidation.displayName(current.displayName)
        val emailError = AuthValidation.email(current.email)
        val passwordError = AuthValidation.newPassword(current.password)
        if (nameError != null || emailError != null || passwordError != null) {
            _state.update { it.copy(displayNameError = nameError, emailError = emailError, passwordError = passwordError) }
            return
        }

        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            attempt { auth.register(current.email, current.password, current.displayName) }
                .onFailure { e ->
                    val fieldError = AuthValidation.serverEmailError(e)
                    _state.update {
                        if (fieldError != null) it.copy(emailError = fieldError) else it.copy(error = e.toUiText())
                    }
                }
            _state.update { it.copy(isSubmitting = false) }
        }
    }
}
