package com.example.volatiledemandforecasting.data.workspace

import com.example.volatiledemandforecasting.core.datastore.AppPreferences
import com.example.volatiledemandforecasting.core.datastore.StoreSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn

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

    suspend fun select(selection: StoreSelection) = prefs.saveSelection(selection)

    /** Signing out clears user-scoped local state (spec §14). */
    suspend fun clear() = prefs.clearSelection()
}

/** The demo allows replays for everyone; elsewhere viewers are read-only (handoff §9). The backend still enforces it. */
val StoreSelection.canGenerateForecasts: Boolean get() = isReference || role != Roles.VIEWER
