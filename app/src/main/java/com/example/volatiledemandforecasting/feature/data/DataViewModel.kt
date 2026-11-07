package com.example.volatiledemandforecasting.feature.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volatiledemandforecasting.core.datastore.StoreSelection
import com.example.volatiledemandforecasting.core.network.attempt
import com.example.volatiledemandforecasting.core.network.toUiText
import com.example.volatiledemandforecasting.core.ui.UiText
import com.example.volatiledemandforecasting.core.ui.components.UiState
import com.example.volatiledemandforecasting.core.ui.components.toUiState
import com.example.volatiledemandforecasting.data.imports.ImportDto
import com.example.volatiledemandforecasting.data.imports.ImportRepository
import com.example.volatiledemandforecasting.data.store.HistorySummaryDto
import com.example.volatiledemandforecasting.data.store.StoreRepository
import com.example.volatiledemandforecasting.data.workspace.ActiveStore
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DataScreen(
    val store: StoreSelection,
    val summary: HistorySummaryDto,
    val imports: List<ImportDto>,
    val totalImports: Int,
    val page: Int = 1,
) {
    val hasMore: Boolean get() = imports.size < totalImports
}

private const val PAGE_SIZE = 20

class DataViewModel(
    private val stores: StoreRepository,
    private val imports: ImportRepository,
    private val activeStore: ActiveStore,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<DataScreen>>(UiState.Loading)
    val state: StateFlow<UiState<DataScreen>> = _state.asStateFlow()

    /** An import status to filter by, or null for all. */
    private val _filter = MutableStateFlow<String?>(null)
    val filter: StateFlow<String?> = _filter.asStateFlow()

    private val _messages = Channel<UiText>(Channel.BUFFERED)
    val messages: Flow<UiText> = _messages.receiveAsFlow()

    private var store: StoreSelection? = null

    init {
        viewModelScope.launch {
            combine(activeStore.selected, _filter) { s, f -> s to f }.collectLatest { (s, _) ->
                store = s
                load()
            }
        }
    }

    fun setFilter(status: String?) = _filter.update { status }

    fun retry() {
        viewModelScope.launch { load() }
    }

    /** Called when coming back from the wizard, so the new import and history range show. */
    fun refresh() {
        viewModelScope.launch { load(silent = true) }
    }

    /** [silent] keeps the current list on screen while it reloads. */
    private suspend fun load(silent: Boolean = false) {
        val s = store ?: return
        if (!silent || _state.value !is UiState.Content) _state.value = UiState.Loading
        attempt {
            coroutineScope {
                val summary = async { stores.historySummary(s.storeId) }
                val page = async { imports.list(s.storeId, _filter.value, page = 1, pageSize = PAGE_SIZE) }
                val p = page.await()
                DataScreen(s, summary.await(), p.items, p.total)
            }
        }
            .onSuccess { _state.value = UiState.Content(it) }
            .onFailure { if (!silent) _state.value = it.toUiState() }
    }

    fun loadMore() {
        val s = store ?: return
        val current = (_state.value as? UiState.Content)?.data ?: return
        if (!current.hasMore) return
        viewModelScope.launch {
            val next = current.page + 1
            attempt { imports.list(s.storeId, _filter.value, page = next, pageSize = PAGE_SIZE) }
                .onSuccess { _state.value = UiState.Content(current.copy(imports = current.imports + it.items, totalImports = it.total, page = next)) }
                .onFailure { _messages.send(it.toUiText()) }
        }
    }

    /** Removes an imported batch's rows from the history, after the user confirmed. */
    fun delete(item: ImportDto) {
        viewModelScope.launch {
            attempt { imports.delete(item.id) }
                .onSuccess { load() }
                .onFailure { _messages.send(it.toUiText()) }
        }
    }
}
