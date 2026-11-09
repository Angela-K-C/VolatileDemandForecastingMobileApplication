package com.example.volatiledemandforecasting.feature.forecasts.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volatiledemandforecasting.core.datastore.StoreSelection
import com.example.volatiledemandforecasting.core.network.attempt
import com.example.volatiledemandforecasting.core.network.toUiText
import com.example.volatiledemandforecasting.core.ui.Formatting
import com.example.volatiledemandforecasting.core.ui.UiText
import com.example.volatiledemandforecasting.core.ui.components.UiState
import com.example.volatiledemandforecasting.core.ui.components.toUiState
import com.example.volatiledemandforecasting.data.catalog.ProductDto
import com.example.volatiledemandforecasting.data.catalog.ProductRepository
import com.example.volatiledemandforecasting.data.forecast.ForecastMethod
import com.example.volatiledemandforecasting.data.forecast.ForecastRepository
import com.example.volatiledemandforecasting.data.forecast.ForecastRequest
import com.example.volatiledemandforecasting.data.forecast.JobDto
import com.example.volatiledemandforecasting.data.store.CapabilitiesDto
import com.example.volatiledemandforecasting.data.store.StoreRepository
import com.example.volatiledemandforecasting.data.workspace.ActiveStore
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** What the form is built from. Every option comes from [capabilities]. */
data class ConfigData(
    val store: StoreSelection,
    val product: ProductDto,
    val capabilities: CapabilitiesDto,
) {
    val canPickDate: Boolean get() = capabilities.asOf.editable
    val defaultAsOf: LocalDate? get() = Formatting.parseDate(capabilities.asOf.default)
    val replayMin: LocalDate? get() = Formatting.parseDate(capabilities.asOf.replayMin)
    val replayMax: LocalDate? get() = Formatting.parseDate(capabilities.asOf.replayMax)

    /** The default date is a true forecast; dates inside the replay range are replays. */
    fun isSelectable(date: LocalDate): Boolean {
        if (date == defaultAsOf) return true
        val min = replayMin ?: return false
        val max = replayMax ?: return false
        return !date.isBefore(min) && !date.isAfter(max)
    }

    fun isReplay(date: LocalDate?): Boolean = date != null && date != defaultAsOf && isSelectable(date)
}

data class ConfigForm(
    val asOf: LocalDate? = null,
    val horizon: Int? = null,
    val method: String = ForecastMethod.COMPARE,
    val submitting: Boolean = false,
    val error: UiText? = null,
)

class ForecastConfigViewModel(
    private val productId: String,
    private val presetHorizon: Int?,
    private val products: ProductRepository,
    private val stores: StoreRepository,
    private val forecasts: ForecastRepository,
    private val activeStore: ActiveStore,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<ConfigData>>(UiState.Loading)
    val state: StateFlow<UiState<ConfigData>> = _state.asStateFlow()

    private val _form = MutableStateFlow(ConfigForm())
    val form: StateFlow<ConfigForm> = _form.asStateFlow()

    private val _created = Channel<JobDto>(Channel.BUFFERED)
    val created: Flow<JobDto> = _created.receiveAsFlow()

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
                    val caps = async { stores.capabilities(s.storeId) }
                    ConfigData(s, product.await(), caps.await())
                }
            }
                .onSuccess { data ->
                    _form.value = initialForm(data)
                    _state.value = UiState.Content(data)
                }
                .onFailure { _state.value = it.toUiState() }
        }
    }

    private fun initialForm(data: ConfigData): ConfigForm {
        val caps = data.capabilities
        val horizon = presetHorizon?.takeIf { it in caps.horizons } ?: caps.horizons.firstOrNull()
        val method = when {
            caps.aiAvailable && ForecastMethod.COMPARE in caps.methods -> ForecastMethod.COMPARE
            ForecastMethod.BASELINE in caps.methods -> ForecastMethod.BASELINE
            else -> caps.methods.firstOrNull() ?: ForecastMethod.BASELINE
        }
        return ConfigForm(asOf = data.defaultAsOf, horizon = horizon, method = method)
    }

    fun setAsOf(date: LocalDate) = _form.update { it.copy(asOf = date, error = null) }
    fun setHorizon(days: Int) = _form.update { it.copy(horizon = days, error = null) }
    fun setMethod(method: String) = _form.update { it.copy(method = method, error = null) }

    fun submit() {
        val s = store ?: return
        if (_state.value !is UiState.Content) return
        val form = _form.value
        val horizon = form.horizon ?: return
        if (form.submitting) return
        _form.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            attempt {
                forecasts.create(
                    ForecastRequest(storeId = s.storeId, productId = productId, horizonDays = horizon, method = form.method)
                )
            }
                .onSuccess {
                    _form.update { f -> f.copy(submitting = false) }
                    _created.send(it)
                }
                .onFailure { e -> _form.update { f -> f.copy(submitting = false, error = e.toUiText()) } }
        }
    }
}
