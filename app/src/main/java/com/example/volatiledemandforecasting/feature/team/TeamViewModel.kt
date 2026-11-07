package com.example.volatiledemandforecasting.feature.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.datastore.StoreSelection
import com.example.volatiledemandforecasting.core.network.attempt
import com.example.volatiledemandforecasting.core.network.toUiText
import com.example.volatiledemandforecasting.core.ui.UiText
import com.example.volatiledemandforecasting.core.ui.components.UiState
import com.example.volatiledemandforecasting.core.ui.components.toUiState
import com.example.volatiledemandforecasting.data.team.InvitationDto
import com.example.volatiledemandforecasting.data.team.MemberDto
import com.example.volatiledemandforecasting.data.team.TeamRepository
import com.example.volatiledemandforecasting.data.user.UserRepository
import com.example.volatiledemandforecasting.data.workspace.ActiveStore
import com.example.volatiledemandforecasting.data.workspace.Roles
import com.example.volatiledemandforecasting.feature.auth.AuthValidation
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TeamData(
    val store: StoreSelection,
    val myUserId: String,
    val members: List<MemberDto>,
    /** Only owners can list invitations; for everyone else this is empty. */
    val invitations: List<InvitationDto>,
) {
    val isOwner: Boolean get() = store.role == Roles.OWNER
}

data class InviteForm(
    val email: String = "",
    val role: String = Roles.VIEWER,
    val emailError: UiText? = null,
    val sending: Boolean = false,
)

sealed interface TeamEvent {
    data class Message(val text: UiText) : TeamEvent
    /** The user left this workspace, so the screens that belong to it should close. */
    data object Left : TeamEvent
}

class TeamViewModel(
    private val team: TeamRepository,
    private val users: UserRepository,
    private val activeStore: ActiveStore,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<TeamData>>(UiState.Loading)
    val state: StateFlow<UiState<TeamData>> = _state.asStateFlow()

    private val _invite = MutableStateFlow(InviteForm())
    val invite: StateFlow<InviteForm> = _invite.asStateFlow()

    private val _events = Channel<TeamEvent>(Channel.BUFFERED)
    val events: Flow<TeamEvent> = _events.receiveAsFlow()

    init {
        load()
    }

    fun load(silent: Boolean = false) {
        if (!silent) _state.value = UiState.Loading
        viewModelScope.launch {
            attempt {
                val store = activeStore.selected.first()
                coroutineScope {
                    val me = async { users.me().id }
                    val members = async { team.members(store.workspaceId) }
                    val invitations = async {
                        if (store.role == Roles.OWNER) team.pendingInvitations(store.workspaceId).filter { it.status == "PENDING" } else emptyList()
                    }
                    TeamData(store, me.await(), members.await(), invitations.await())
                }
            }
                .onSuccess { _state.value = UiState.Content(it) }
                .onFailure { if (!silent) _state.value = it.toUiState() else _events.send(TeamEvent.Message(it.toUiText())) }
        }
    }

    fun setInviteEmail(value: String) = _invite.update { it.copy(email = value, emailError = null) }
    fun setInviteRole(role: String) = _invite.update { it.copy(role = role) }

    fun sendInvite() {
        val data = (_state.value as? UiState.Content)?.data ?: return
        val form = _invite.value
        if (form.sending) return
        val emailError = AuthValidation.email(form.email)
        if (emailError != null) {
            _invite.update { it.copy(emailError = emailError) }
            return
        }
        _invite.update { it.copy(sending = true) }
        viewModelScope.launch {
            attempt { team.invite(data.store.workspaceId, form.email, form.role) }
                .onSuccess {
                    _invite.value = InviteForm(role = form.role)
                    _events.send(TeamEvent.Message(UiText.Res(R.string.team_invited, listOf(form.email.trim()))))
                    load(silent = true)
                }
                .onFailure { e ->
                    _invite.update { it.copy(sending = false) }
                    _events.send(TeamEvent.Message(e.toUiText()))
                }
        }
    }

    fun revoke(invitation: InvitationDto) = act { team.revoke(invitation.workspaceId, invitation.id) }

    fun changeRole(member: MemberDto, role: String) {
        val data = (_state.value as? UiState.Content)?.data ?: return
        if (role == member.role) return
        act { team.changeRole(data.store.workspaceId, member.userId, role) }
    }

    fun remove(member: MemberDto) {
        val data = (_state.value as? UiState.Content)?.data ?: return
        act { team.removeMember(data.store.workspaceId, member.userId) }
    }

    /** Anyone can remove themselves. The backend refuses to remove the last owner (`LAST_OWNER`). */
    fun leave() {
        val data = (_state.value as? UiState.Content)?.data ?: return
        viewModelScope.launch {
            attempt { team.removeMember(data.store.workspaceId, data.myUserId) }
                .onSuccess {
                    activeStore.clear()
                    _events.send(TeamEvent.Left)
                }
                .onFailure { _events.send(TeamEvent.Message(it.toUiText())) }
        }
    }

    private fun act(block: suspend () -> Unit) {
        viewModelScope.launch {
            attempt { block() }
                .onSuccess { load(silent = true) }
                .onFailure { _events.send(TeamEvent.Message(it.toUiText())) }
        }
    }
}
