package com.example.volatiledemandforecasting.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volatiledemandforecasting.core.security.SessionManager
import com.example.volatiledemandforecasting.core.security.SessionState
import com.example.volatiledemandforecasting.core.ui.components.WorkspaceChipUi
import com.example.volatiledemandforecasting.data.auth.AuthRepository
import com.example.volatiledemandforecasting.data.team.TeamRepository
import com.example.volatiledemandforecasting.data.user.MeDto
import com.example.volatiledemandforecasting.data.user.UserRepository
import com.example.volatiledemandforecasting.data.workspace.ActiveStore
import com.example.volatiledemandforecasting.data.workspace.Roles
import com.example.volatiledemandforecasting.data.workspace.Selection
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
    /** Pending invitations to this user's email. Shown as a badge on Settings (api.md, Team). */
    val pendingInvitations: Int = 0,
)

/** Activity-scoped, so every destination shows the same user and workspace chip. */
class ShellViewModel(
    private val session: SessionManager,
    private val users: UserRepository,
    private val auth: AuthRepository,
    private val activeStore: ActiveStore,
    private val team: TeamRepository,
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
        viewModelScope.launch {
            activeStore.state.collect { selection ->
                val chosen = (selection as? Selection.Selected)?.store
                _state.update {
                    it.copy(
                        workspace = chosen?.let { s -> WorkspaceChipUi(label = s.storeName.ifBlank { s.workspaceName }, isDemo = s.isReference) },
                        canManageTeam = chosen != null && !chosen.isReference && chosen.role == Roles.OWNER,
                    )
                }
            }
        }
    }

    // A failed load leaves `user` null; the screens that need it fetch their own data and show errors.
    private suspend fun loadProfile() {
        try {
            val me = users.me()
            _state.update { it.copy(user = me) }
            refreshInvitations()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(user = null) }
        }
    }

    /** Called after accepting an invitation or leaving a workspace, so access and the badge are current. */
    fun refreshProfile() {
        viewModelScope.launch { loadProfile() }
    }

    fun refreshInvitations() {
        viewModelScope.launch {
            try {
                val pending = team.myInvitations().count { it.status == "PENDING" }
                _state.update { it.copy(pendingInvitations = pending) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The badge is a convenience; the invitations screen shows its own errors.
            }
        }
    }

    fun signOut() = auth.logout()
}
