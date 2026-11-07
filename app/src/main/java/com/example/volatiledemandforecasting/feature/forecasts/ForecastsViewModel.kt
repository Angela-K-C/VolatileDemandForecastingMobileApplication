package com.example.volatiledemandforecasting.feature.forecasts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volatiledemandforecasting.core.datastore.StoreSelection
import com.example.volatiledemandforecasting.core.network.attempt
import com.example.volatiledemandforecasting.core.network.toUiText
import com.example.volatiledemandforecasting.core.ui.UiText
import com.example.volatiledemandforecasting.core.ui.components.UiState
import com.example.volatiledemandforecasting.core.ui.components.toUiState
import com.example.volatiledemandforecasting.data.forecast.ForecastRepository
import com.example.volatiledemandforecasting.data.forecast.ForecastSummaryDto
import com.example.volatiledemandforecasting.data.workspace.ActiveStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class HistoryFilter(val replay: Boolean?) { All(null), Replays(true), TrueForecasts(false) }

data class ForecastList(
    val items: List<ForecastSummaryDto>,
    val total: Int,
    val page: Int = 1,
    val loadingMore: Boolean = false,
) {
    val hasMore: Boolean get() = items.size < total
}

private const val PAGE_SIZE = 20

@OptIn(ExperimentalCoroutinesApi::class)
class ForecastsViewModel(
    private val repository: ForecastRepository,
    private val activeStore: ActiveStore,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<ForecastList>>(UiState.Loading)
    val state: StateFlow<UiState<ForecastList>> = _state.asStateFlow()

    private val _filter = MutableStateFlow(HistoryFilter.All)
    val filter: StateFlow<HistoryFilter> = _filter.asStateFlow()

    private val _messages = Channel<UiText>(Channel.BUFFERED)
    val messages: Flow<UiText> = _messages.receiveAsFlow()

    private var store: StoreSelection? = null

    init {
        viewModelScope.launch {
            combine(activeStore.selected, _filter) { s, f -> s to f }.collectLatest { (s, _) ->
                store = s
                loadFirstPage()
            }
        }
    }

    fun setFilter(value: HistoryFilter) = _filter.update { value }

    fun retry() {
        viewModelScope.launch { loadFirstPage() }
    }

    private suspend fun loadFirstPage() {
        val s = store ?: return
        _state.value = UiState.Loading
        attempt { repository.list(s.storeId, _filter.value.replay, null, page = 1, pageSize = PAGE_SIZE) }
            .onSuccess { page ->
                _state.value = if (page.items.isEmpty()) UiState.Empty else UiState.Content(ForecastList(page.items, page.total))
            }
            .onFailure { _state.value = it.toUiState() }
    }

    fun loadMore() {
        val s = store ?: return
        val current = (_state.value as? UiState.Content)?.data ?: return
        if (current.loadingMore || !current.hasMore) return
        _state.value = UiState.Content(current.copy(loadingMore = true))
        viewModelScope.launch {
            val next = current.page + 1
            attempt { repository.list(s.storeId, _filter.value.replay, null, page = next, pageSize = PAGE_SIZE) }
                .onSuccess { _state.value = UiState.Content(ForecastList(current.items + it.items, it.total, next)) }
                .onFailure {
                    _state.value = UiState.Content(current.copy(loadingMore = false))
                    _messages.send(it.toUiText())
                }
        }
    }

    fun delete(item: ForecastSummaryDto) {
        viewModelScope.launch {
            attempt { repository.delete(item.forecastId) }
                .onSuccess { loadFirstPage() }
                .onFailure { _messages.send(it.toUiText()) }
        }
    }
}
