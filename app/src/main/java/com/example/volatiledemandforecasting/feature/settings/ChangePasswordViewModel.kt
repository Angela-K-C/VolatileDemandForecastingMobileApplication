package com.example.volatiledemandforecasting.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.network.ApiErrorCodes
import com.example.volatiledemandforecasting.core.network.ApiException
import com.example.volatiledemandforecasting.core.network.attempt
import com.example.volatiledemandforecasting.core.network.toUiText
import com.example.volatiledemandforecasting.core.ui.UiText
import com.example.volatiledemandforecasting.data.user.UserRepository
import com.example.volatiledemandforecasting.feature.auth.AuthValidation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChangePasswordState(
    val current: String = "",
    val new: String = "",
    val confirm: String = "",
    val currentError: UiText? = null,
    val newError: UiText? = null,
    val confirmError: UiText? = null,
    val submitting: Boolean = false,
    val error: UiText? = null,
)

object ChangePasswordRules {
    fun confirm(new: String, confirm: String): UiText? =
        if (new != confirm) UiText.Res(R.string.cp_mismatch) else null
}

class ChangePasswordViewModel(private val users: UserRepository) : ViewModel() {

    private val _state = MutableStateFlow(ChangePasswordState())
    val state: StateFlow<ChangePasswordState> = _state.asStateFlow()

    private val _done = Channel<Unit>(Channel.BUFFERED)
    val done: Flow<Unit> = _done.receiveAsFlow()

    fun setCurrent(v: String) = _state.update { it.copy(current = v, currentError = null, error = null) }
    fun setNew(v: String) = _state.update { it.copy(new = v, newError = null, error = null) }
    fun setConfirm(v: String) = _state.update { it.copy(confirm = v, confirmError = null, error = null) }

    fun submit() {
        val s = _state.value
        if (s.submitting) return
        val currentError = AuthValidation.existingPassword(s.current)
        val newError = AuthValidation.newPassword(s.new)
        val confirmError = if (newError == null) ChangePasswordRules.confirm(s.new, s.confirm) else null
        if (currentError != null || newError != null || confirmError != null) {
            _state.update { it.copy(currentError = currentError, newError = newError, confirmError = confirmError) }
            return
        }
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            attempt { users.changePassword(s.current, s.new) }
                .onSuccess {
                    _state.update { it.copy(submitting = false) }
                    _done.send(Unit)
                }
                .onFailure { e ->
                    // A wrong current password is a problem with that field, not a general failure.
                    if (e is ApiException && e.code == ApiErrorCodes.INVALID_CREDENTIALS) {
                        _state.update { it.copy(submitting = false, currentError = UiText.Res(R.string.cp_wrong_current)) }
                    } else {
                        _state.update { it.copy(submitting = false, error = e.toUiText()) }
                    }
                }
        }
    }
}
