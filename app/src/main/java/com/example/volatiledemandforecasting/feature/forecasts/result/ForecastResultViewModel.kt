package com.example.volatiledemandforecasting.feature.forecasts.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volatiledemandforecasting.core.datastore.AppPreferences
import com.example.volatiledemandforecasting.core.datastore.TableDisplay
import com.example.volatiledemandforecasting.core.network.attempt
import com.example.volatiledemandforecasting.core.network.toUiText
import com.example.volatiledemandforecasting.core.ui.UiText
import com.example.volatiledemandforecasting.core.ui.components.UiState
import com.example.volatiledemandforecasting.core.ui.components.toUiState
import com.example.volatiledemandforecasting.data.forecast.ForecastDto
import com.example.volatiledemandforecasting.data.forecast.ForecastRepository
import com.example.volatiledemandforecasting.data.store.ModelCardDto
import com.example.volatiledemandforecasting.data.store.StoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream

data class ResultData(
    val forecast: ForecastDto,
    /** The store's model card, the third kind of evidence. Null when AI wasn't used or the card couldn't be loaded. */
    val card: ModelCardDto?,
)

sealed interface ResultEvent {
    data object Exported : ResultEvent
    data class ExportFailed(val message: UiText) : ResultEvent
    data object Deleted : ResultEvent
    data class DeleteFailed(val message: UiText) : ResultEvent
}

class ForecastResultViewModel(
    private val forecastId: String,
    private val forecasts: ForecastRepository,
    private val stores: StoreRepository,
    private val preferences: AppPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<ResultData>>(UiState.Loading)
    val state: StateFlow<UiState<ResultData>> = _state.asStateFlow()

    val display: StateFlow<TableDisplay> = preferences.tableDisplay
        .stateIn(viewModelScope, SharingStarted.Eagerly, TableDisplay.Decimals)

    private val _events = Channel<ResultEvent>(Channel.BUFFERED)
    val events: Flow<ResultEvent> = _events.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            attempt { forecasts.forecast(forecastId) }
                .onSuccess { forecast ->
                    // The card is extra evidence: if it can't be loaded the rest of the result is still shown.
                    val card = if (forecast.isCompleted && forecast.model != null) {
                        attempt { stores.activeModel(forecast.storeId) }.getOrNull()?.card
                    } else null
                    _state.value = UiState.Content(ResultData(forecast, card))
                }
                .onFailure { _state.value = it.toUiState() }
        }
    }

    fun setDisplay(value: TableDisplay) {
        viewModelScope.launch { preferences.setTableDisplay(value) }
    }

    /** [open] gives the destination the user picked in the system file dialog. */
    fun export(open: () -> OutputStream?) {
        viewModelScope.launch {
            val result = attempt {
                val sink = withContext(Dispatchers.IO) { open() } ?: throw java.io.IOException("No destination")
                sink.use { forecasts.exportCsv(forecastId, it) }
            }
            _events.send(
                result.fold({ ResultEvent.Exported }, { ResultEvent.ExportFailed(exportMessage(it)) })
            )
        }
    }

    // A local write problem isn't a network problem, so only server errors keep their own wording.
    private fun exportMessage(error: Throwable): UiText =
        if (error is com.example.volatiledemandforecasting.core.network.ApiException) error.toUiText()
        else UiText.Res(com.example.volatiledemandforecasting.R.string.res_export_failed)

    fun delete() {
        viewModelScope.launch {
            val result = attempt { forecasts.delete(forecastId) }
            _events.send(result.fold({ ResultEvent.Deleted }, { ResultEvent.DeleteFailed(it.toUiText()) }))
        }
    }
}
