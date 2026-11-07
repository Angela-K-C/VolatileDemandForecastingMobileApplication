package com.example.volatiledemandforecasting.feature.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volatiledemandforecasting.core.datastore.StoreSelection
import com.example.volatiledemandforecasting.core.network.ApiErrorCodes
import com.example.volatiledemandforecasting.core.network.ApiException
import com.example.volatiledemandforecasting.core.network.attempt
import com.example.volatiledemandforecasting.core.ui.components.UiState
import com.example.volatiledemandforecasting.core.ui.components.toUiState
import com.example.volatiledemandforecasting.data.store.ActiveModelDto
import com.example.volatiledemandforecasting.data.store.StoreRepository
import com.example.volatiledemandforecasting.data.workspace.ActiveStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** [model] is null when the store has no active AI model yet, which is normal for a new business store. */
data class ModelInfoScreen(val store: StoreSelection, val model: ActiveModelDto?)

class ModelInfoViewModel(
    private val stores: StoreRepository,
    private val activeStore: ActiveStore,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<ModelInfoScreen>>(UiState.Loading)
    val state: StateFlow<UiState<ModelInfoScreen>> = _state.asStateFlow()

    private var store: StoreSelection? = null

    init {
        viewModelScope.launch {
            combine(activeStore.selected, activeStore.dataRevision) { s, _ -> s }.collect {
                store = it
                load()
            }
        }
    }

    fun load() {
        val s = store ?: return
        _state.value = UiState.Loading
        viewModelScope.launch {
            attempt { stores.activeModel(s.storeId) }
                .onSuccess { _state.value = UiState.Content(ModelInfoScreen(s, it)) }
                .onFailure { error ->
                    // No active model is a normal state for a new store, not an error.
                    _state.value =
                        if (error is ApiException && error.code == ApiErrorCodes.AI_NOT_AVAILABLE) UiState.Content(ModelInfoScreen(s, null))
                        else error.toUiState()
                }
        }
    }
}
