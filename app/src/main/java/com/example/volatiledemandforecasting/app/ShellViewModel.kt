package com.example.volatiledemandforecasting.app

import androidx.lifecycle.ViewModel
import com.example.volatiledemandforecasting.core.ui.components.WorkspaceChipUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the shared chrome (top bar, Options menu) needs to know about the signed-in session. */
data class ShellUiState(
    val workspace: WorkspaceChipUi? = null,
    /** Owners only (spec §8.12). The backend still enforces it. */
    val canManageTeam: Boolean = false,
)

/**
 * Activity-scoped, so every destination shows the same workspace chip.
 * Nothing sets the workspace until the workspace picker exists (build step 3).
 */
class ShellViewModel : ViewModel() {
    private val _state = MutableStateFlow(ShellUiState())
    val state: StateFlow<ShellUiState> = _state.asStateFlow()
}
