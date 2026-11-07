package com.example.volatiledemandforecasting.feature.products.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volatiledemandforecasting.core.datastore.StoreSelection
import com.example.volatiledemandforecasting.core.network.attempt
import com.example.volatiledemandforecasting.core.ui.Formatting
import com.example.volatiledemandforecasting.core.ui.components.UiState
import com.example.volatiledemandforecasting.core.ui.components.toUiState
import com.example.volatiledemandforecasting.data.catalog.ProductDto
import com.example.volatiledemandforecasting.data.catalog.ProductRepository
import com.example.volatiledemandforecasting.data.store.StoreRepository
import com.example.volatiledemandforecasting.data.workspace.ActiveStore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

const val HISTORY_DAYS = 90

data class ProductDetail(
    val store: StoreSelection,
    val product: ProductDto,
    /** One value per day from [firstDate]. Null is a day with no recorded sales, drawn as a gap, never as 0. */
    val units: List<Double?>,
    val firstDate: LocalDate?,
) {
    val hasAnySales: Boolean get() = units.any { it != null }
}

class ProductDetailViewModel(
    private val productId: String,
    private val products: ProductRepository,
    private val stores: StoreRepository,
    private val activeStore: ActiveStore,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<ProductDetail>>(UiState.Loading)
    val state: StateFlow<UiState<ProductDetail>> = _state.asStateFlow()

    private var store: StoreSelection? = null

    init {
        viewModelScope.launch {
            activeStore.selected.collect {
                store = it
                load()
            }
        }
    }

    fun load() {
        val s = store ?: return
        _state.value = UiState.Loading
        viewModelScope.launch {
            attempt {
                coroutineScope {
                    val product = async { products.product(s.storeId, productId) }
                    val end = Formatting.parseDate(stores.historySummary(s.storeId).historyEnd)
                    val units = if (end == null) null else {
                        val start = end.minusDays(HISTORY_DAYS - 1L)
                        val history = products.history(s.storeId, productId, start.toString(), end.toString())
                        val byDate = history.points.associate { it.date.take(10) to it.units }
                        start to (0 until HISTORY_DAYS).map { byDate[start.plusDays(it.toLong()).toString()] }
                    }
                    ProductDetail(s, product.await(), units?.second.orEmpty(), units?.first)
                }
            }
                .onSuccess { _state.value = UiState.Content(it) }
                .onFailure { _state.value = it.toUiState() }
        }
    }

    internal companion object {
        fun daysBetween(a: LocalDate, b: LocalDate) = ChronoUnit.DAYS.between(a, b).toInt()
    }
}
