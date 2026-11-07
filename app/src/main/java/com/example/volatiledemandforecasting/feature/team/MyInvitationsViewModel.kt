package com.example.volatiledemandforecasting.feature.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volatiledemandforecasting.core.network.attempt
import com.example.volatiledemandforecasting.core.network.toUiText
import com.example.volatiledemandforecasting.core.ui.UiText
import com.example.volatiledemandforecasting.core.ui.components.UiState
import com.example.volatiledemandforecasting.core.ui.components.toUiState
import com.example.volatiledemandforecasting.data.team.InvitationDto
import com.example.volatiledemandforecasting.data.team.TeamRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface InvitationEvent {
    data class Message(val text: UiText) : InvitationEvent
    /** Access changed, so the profile (and the workspace list) must be reloaded. */
    data object Accepted : InvitationEvent
}

class MyInvitationsViewModel(private val team: TeamRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<InvitationDto>>>(UiState.Loading)
    val state: StateFlow<UiState<List<InvitationDto>>> = _state.asStateFlow()

    /** Ids being answered, so a double tap can't send the same answer twice. */
    private val _busy = MutableStateFlow<Set<String>>(emptySet())
    val busy: StateFlow<Set<String>> = _busy.asStateFlow()

    private val _events = Channel<InvitationEvent>(Channel.BUFFERED)
    val events: Flow<InvitationEvent> = _events.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch { fetch() }
    }

    private suspend fun fetch() {
        attempt { team.myInvitations().filter { it.status == "PENDING" } }
            .onSuccess { _state.value = if (it.isEmpty()) UiState.Empty else UiState.Content(it) }
            .onFailure { _state.value = it.toUiState() }
    }

    fun accept(invitation: InvitationDto) = answer(invitation) {
        team.accept(invitation.id)
        _events.send(InvitationEvent.Accepted)
    }

    fun decline(invitation: InvitationDto) = answer(invitation) { team.decline(invitation.id) }

    private fun answer(invitation: InvitationDto, block: suspend () -> Unit) {
        if (invitation.id in _busy.value) return
        _busy.update { it + invitation.id }
        viewModelScope.launch {
            attempt { block() }.onFailure { _events.send(InvitationEvent.Message(it.toUiText())) }
            _busy.update { it - invitation.id }
            // Reload either way: an invitation that was revoked or expired meanwhile disappears.
            fetch()
        }
    }
}
