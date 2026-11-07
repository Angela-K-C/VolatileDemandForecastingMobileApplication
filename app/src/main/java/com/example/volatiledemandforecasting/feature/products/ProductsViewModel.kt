package com.example.volatiledemandforecasting.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volatiledemandforecasting.core.datastore.StoreSelection
import com.example.volatiledemandforecasting.core.network.attempt
import com.example.volatiledemandforecasting.core.network.toUiText
import com.example.volatiledemandforecasting.core.ui.UiText
import com.example.volatiledemandforecasting.core.ui.components.UiState
import com.example.volatiledemandforecasting.core.ui.components.toUiState
import com.example.volatiledemandforecasting.data.catalog.ProductDto
import com.example.volatiledemandforecasting.data.catalog.ProductRepository
import com.example.volatiledemandforecasting.data.workspace.ActiveStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ProductList(
    val items: List<ProductDto>,
    val total: Int,
    val loadingMore: Boolean = false,
    val loadMoreError: UiText? = null,
    val page: Int = 1,
) {
    val hasMore: Boolean get() = items.size < total
}

private const val PAGE_SIZE = 50

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ProductsViewModel(
    private val repository: ProductRepository,
    private val activeStore: ActiveStore,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<ProductList>>(UiState.Loading)
    val state: StateFlow<UiState<ProductList>> = _state.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** An API demand class code, or null for all. */
    private val _demandClass = MutableStateFlow<String?>(null)
    val demandClass: StateFlow<String?> = _demandClass.asStateFlow()

    private var store: StoreSelection? = null

    init {
        viewModelScope.launch {
            combine(activeStore.selected, _query.debounce(300).distinctUntilChanged(), _demandClass, activeStore.dataRevision) { s, q, c, _ -> Triple(s, q, c) }
                .collectLatest { (s, _, _) ->
                    store = s
                    loadFirstPage()
                }
        }
    }

    fun setQuery(value: String) = _query.update { value }

    fun setDemandClass(value: String?) = _demandClass.update { value }

    fun retry() {
        viewModelScope.launch { loadFirstPage() }
    }

    private suspend fun loadFirstPage() {
        val s = store ?: return
        _state.value = UiState.Loading
        attempt { repository.products(s.storeId, _query.value, _demandClass.value, page = 1, pageSize = PAGE_SIZE) }
            .onSuccess { page ->
                _state.value = if (page.items.isEmpty()) UiState.Empty else UiState.Content(ProductList(page.items, page.total))
            }
            .onFailure { _state.value = it.toUiState() }
    }

    fun loadMore() {
        val s = store ?: return
        val current = (_state.value as? UiState.Content)?.data ?: return
        if (current.loadingMore || !current.hasMore) return
        _state.value = UiState.Content(current.copy(loadingMore = true, loadMoreError = null))
        viewModelScope.launch {
            val next = current.page + 1
            attempt { repository.products(s.storeId, _query.value, _demandClass.value, page = next, pageSize = PAGE_SIZE) }
                .onSuccess { page ->
                    _state.value = UiState.Content(
                        ProductList(current.items + page.items, page.total, page = next)
                    )
                }
                .onFailure { _state.value = UiState.Content(current.copy(loadingMore = false, loadMoreError = it.toUiText())) }
        }
    }
}
