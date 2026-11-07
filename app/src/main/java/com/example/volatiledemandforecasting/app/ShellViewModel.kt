package com.example.volatiledemandforecasting.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volatiledemandforecasting.core.security.SessionManager
import com.example.volatiledemandforecasting.core.security.SessionState
import com.example.volatiledemandforecasting.core.ui.components.WorkspaceChipUi
import com.example.volatiledemandforecasting.data.auth.AuthRepository
import com.example.volatiledemandforecasting.data.user.MeDto
import com.example.volatiledemandforecasting.data.user.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the shared chrome (top bar, Options menu) needs to know about the signed-in session. */
data class ShellUiState(
    /** From `GET /v1/me`. Null until it loads, and while signed out. */
    val user: MeDto? = null,
    val workspace: WorkspaceChipUi? = null,
    /** Owners only (spec §8.12). The backend still enforces it. */
    val canManageTeam: Boolean = false,
)

/**
 * Activity-scoped, so every destination shows the same user and workspace chip.
 * Nothing sets the workspace until the workspace picker exists (build step 3).
 */
class ShellViewModel(
    private val session: SessionManager,
    private val users: UserRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ShellUiState())
    val state: StateFlow<ShellUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            session.state.collectLatest { sessionState ->
                when (sessionState) {
                    SessionState.SignedIn -> loadProfile()
                    is SessionState.SignedOut -> _state.value = ShellUiState()
                }
            }
        }
    }

    // A failed load leaves `user` null. Step 3's workspace picker is the first screen that depends
    // on /v1/me, so it owns the loading and error states for it.
    private suspend fun loadProfile() {
        try {
            val me = users.me()
            _state.update { it.copy(user = me) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(user = null) }
        }
    }

    fun signOut() = auth.logout()
}
