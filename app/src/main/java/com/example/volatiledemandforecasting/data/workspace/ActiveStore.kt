package com.example.volatiledemandforecasting.data.workspace

import com.example.volatiledemandforecasting.core.datastore.AppPreferences
import com.example.volatiledemandforecasting.core.datastore.StoreSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

sealed interface Selection {
    /** The saved choice hasn't been read yet. It takes a few milliseconds at start-up. */
    data object Loading : Selection
    data object None : Selection
    data class Selected(val store: StoreSelection) : Selection
}

/**
 * The workspace and store every store-scoped screen works on. Screens collect [selected] so they
 * reload by themselves when the user switches workspace.
 */
class ActiveStore(private val prefs: AppPreferences, private val scope: CoroutineScope) {

    val state: StateFlow<Selection> = prefs.selection
        .map { if (it == null) Selection.None else Selection.Selected(it) }
        .stateIn(scope, SharingStarted.Eagerly, Selection.Loading)

    val selected: Flow<StoreSelection> = state.mapNotNull { (it as? Selection.Selected)?.store }.distinctUntilChanged()

    /**
     * Bumped when the store's data changes (an import, a deleted import, a finished training run), so
     * screens that already loaded reload instead of showing stale numbers.
     */
    private val _dataRevision = MutableStateFlow(0)
    val dataRevision: StateFlow<Int> = _dataRevision.asStateFlow()

    fun dataChanged() = _dataRevision.update { it + 1 }

    suspend fun select(selection: StoreSelection) = prefs.saveSelection(selection)

    /** Signing out clears user-scoped local state (spec §14). */
    suspend fun clear() = prefs.clearSelection()
}

/** The demo allows replays for everyone; elsewhere viewers are read-only (handoff §9). The backend still enforces it. */
val StoreSelection.canGenerateForecasts: Boolean get() = isReference || role != Roles.VIEWER

/** Uploading, training and deleting imports: owners and managers of their own workspaces. The backend enforces it too. */
val StoreSelection.canManageData: Boolean get() = !isReference && (role == Roles.OWNER || role == Roles.MANAGER)
