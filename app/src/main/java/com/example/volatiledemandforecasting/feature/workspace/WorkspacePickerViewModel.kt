package com.example.volatiledemandforecasting.feature.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volatiledemandforecasting.core.datastore.StoreSelection
import com.example.volatiledemandforecasting.core.network.attempt
import com.example.volatiledemandforecasting.core.ui.components.UiState
import com.example.volatiledemandforecasting.core.ui.components.toUiState
import com.example.volatiledemandforecasting.data.workspace.ActiveStore
import com.example.volatiledemandforecasting.data.workspace.Selection
import com.example.volatiledemandforecasting.data.workspace.StoreDto
import com.example.volatiledemandforecasting.data.workspace.WorkspaceDto
import com.example.volatiledemandforecasting.data.workspace.WorkspaceRepository
import com.example.volatiledemandforecasting.data.workspace.WorkspaceWithStores
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PickerData(
    /** The read-only sample shop. Not listed: it is offered once, on the welcome screen. */
    val sample: Pair<WorkspaceDto, StoreDto>?,
    val own: List<WorkspaceWithStores>,
    val currentStoreId: String?,
) {
    val viewingSample: Boolean get() = sample != null && sample.second.id == currentStoreId
}

class WorkspacePickerViewModel(
    private val repository: WorkspaceRepository,
    private val activeStore: ActiveStore,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<PickerData>>(UiState.Loading)
    val state: StateFlow<UiState<PickerData>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            attempt { repository.workspacesWithStores() }
                .onSuccess { all ->
                    val current = ((activeStore.state.value) as? Selection.Selected)?.store?.storeId
                    _state.value = PickerData(
                        sample = all.firstOrNull { it.workspace.isReference && it.stores.isNotEmpty() }?.let { it.workspace to it.stores.first() },
                        own = all.filterNot { it.workspace.isReference },
                        currentStoreId = current,
                    ).let { UiState.Content(it) }
                }
                .onFailure { _state.value = it.toUiState() }
        }
    }

    fun trySample(onDone: () -> Unit) {
        val (workspace, store) = (_state.value as? UiState.Content)?.data?.sample ?: return
        select(workspace, store, onDone)
    }

    /** Saves the choice. Every store-scoped screen reloads from it, and it is remembered for next launch. */
    fun select(workspace: WorkspaceDto, store: StoreDto, onDone: () -> Unit) {
        viewModelScope.launch {
            activeStore.select(
                StoreSelection(
                    workspaceId = workspace.id,
                    workspaceName = workspace.name,
                    role = workspace.role,
                    isReference = workspace.isReference,
                    storeId = store.id,
                    storeName = store.name,
                )
            )
            onDone()
        }
    }
}
