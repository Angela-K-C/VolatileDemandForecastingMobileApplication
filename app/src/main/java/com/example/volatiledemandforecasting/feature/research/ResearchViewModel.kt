package com.example.volatiledemandforecasting.feature.research

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.network.ApiException
import com.example.volatiledemandforecasting.core.network.attempt
import com.example.volatiledemandforecasting.core.network.toUiText
import com.example.volatiledemandforecasting.core.ui.UiText
import com.example.volatiledemandforecasting.core.ui.components.UiState
import com.example.volatiledemandforecasting.core.ui.components.toUiState
import com.example.volatiledemandforecasting.data.research.ResearchRepository
import com.example.volatiledemandforecasting.data.research.ResearchSummaryDto
import com.example.volatiledemandforecasting.data.workspace.ActiveStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** The as-of date range filter. Blank means no limit; otherwise a date as YYYY-MM-DD. */
data class ResearchFilter(
    val from: String = "",
    val to: String = "",
    val fromError: UiText? = null,
    val toError: UiText? = null,
)

object ResearchDates {
    /** Returns the date text to send (null if blank) or an error to show. */
    fun check(value: String): Pair<String?, UiText?> {
        val text = value.trim()
        if (text.isEmpty()) return null to null
        return try {
            LocalDate.parse(text).toString() to null
        } catch (e: DateTimeParseException) {
            null to UiText.Res(R.string.research_date_invalid)
        }
    }
}

sealed interface ResearchEvent {
    data object Exported : ResearchEvent
    data class ExportFailed(val message: UiText) : ResearchEvent
}

class ResearchViewModel(
    private val research: ResearchRepository,
    private val activeStore: ActiveStore,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<ResearchSummaryDto>>(UiState.Loading)
    val state: StateFlow<UiState<ResearchSummaryDto>> = _state.asStateFlow()

    private val _filter = MutableStateFlow(ResearchFilter())
    val filter: StateFlow<ResearchFilter> = _filter.asStateFlow()

    private val _exporting = MutableStateFlow(false)
    val exporting: StateFlow<Boolean> = _exporting.asStateFlow()

    private val _events = Channel<ResearchEvent>(Channel.BUFFERED)
    val events: Flow<ResearchEvent> = _events.receiveAsFlow()

    /** The range that produced the numbers on screen, so an export matches what is shown. */
    private var applied: Pair<String?, String?> = null to null

    init {
        load()
    }

    fun setFrom(v: String) = _filter.update { it.copy(from = v, fromError = null) }
    fun setTo(v: String) = _filter.update { it.copy(to = v, toError = null) }

    fun apply() {
        val f = _filter.value
        val (from, fromError) = ResearchDates.check(f.from)
        val (to, toError) = ResearchDates.check(f.to)
        if (fromError != null || toError != null) {
            _filter.update { it.copy(fromError = fromError, toError = toError) }
            return
        }
        applied = from to to
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            attempt {
                val store = activeStore.selected.first()
                research.summary(store.storeId, applied.first, applied.second)
            }
                .onSuccess { _state.value = UiState.Content(it) }
                .onFailure { _state.value = it.toUiState() }
        }
    }

    fun export(open: () -> OutputStream?) {
        if (_exporting.value) return
        _exporting.value = true
        viewModelScope.launch {
            val result = attempt {
                val store = activeStore.selected.first()
                val sink = withContext(Dispatchers.IO) { open() } ?: throw IOException("No destination")
                sink.use { research.exportCsv(store.storeId, applied.first, applied.second, it) }
            }
            _exporting.value = false
            _events.send(
                result.fold(
                    { ResearchEvent.Exported },
                    { ResearchEvent.ExportFailed(if (it is ApiException) it.toUiText() else UiText.Res(R.string.res_export_failed)) },
                )
            )
        }
    }
}
