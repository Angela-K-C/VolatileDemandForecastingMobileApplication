package com.example.volatiledemandforecasting.feature.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volatiledemandforecasting.core.datastore.StoreSelection
import com.example.volatiledemandforecasting.core.network.attempt
import com.example.volatiledemandforecasting.core.network.toUiText
import com.example.volatiledemandforecasting.core.ui.UiText
import com.example.volatiledemandforecasting.core.ui.components.UiState
import com.example.volatiledemandforecasting.core.ui.components.toUiState
import com.example.volatiledemandforecasting.data.training.EligibilityDto
import com.example.volatiledemandforecasting.data.training.TrainingRepository
import com.example.volatiledemandforecasting.data.training.TrainingRunDto
import com.example.volatiledemandforecasting.data.training.TrainingStatus
import com.example.volatiledemandforecasting.data.workspace.ActiveStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

data class TrainingData(
    val store: StoreSelection,
    val eligibility: EligibilityDto,
    /** The run being shown: the one in progress, or the latest finished one. */
    val run: TrainingRunDto?,
    val starting: Boolean = false,
    val error: UiText? = null,
) {
    val inProgress: Boolean
        get() = starting || (run != null && !TrainingStatus.isFinished(run.status))
}

/** Training takes about 20 to 30 seconds for a small shop (api.md), so poll every 2.5 s and give up after 10 minutes. */
internal object TrainingPolling {
    val interval = 2500.milliseconds
    val limit = 10.minutes
}

class TrainingViewModel(
    private val repository: TrainingRepository,
    private val activeStore: ActiveStore,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<TrainingData>>(UiState.Loading)
    val state: StateFlow<UiState<TrainingData>> = _state.asStateFlow()

    private var store: StoreSelection? = null
    private var pollJob: Job? = null

    init {
        viewModelScope.launch {
            combine(activeStore.selected, activeStore.dataRevision) { s, _ -> s }.collect {
                store = it
                if (it.isReference) _state.value = UiState.Empty else load()
            }
        }
    }

    fun load() {
        val s = store ?: return
        pollJob?.cancel()
        _state.value = UiState.Loading
        viewModelScope.launch {
            attempt { repository.eligibility(s.storeId) }
                .onSuccess { e ->
                    val run = e.latestRun
                    _state.value = UiState.Content(TrainingData(s, e, run))
                    if (run != null && !TrainingStatus.isFinished(run.status)) poll(run.id)
                }
                .onFailure { _state.value = it.toUiState() }
        }
    }

    fun train() {
        val s = store ?: return
        val current = (_state.value as? UiState.Content)?.data ?: return
        if (current.inProgress) return
        mutate { it.copy(starting = true, error = null) }
        viewModelScope.launch {
            attempt { repository.train(s.storeId) }
                .onSuccess { run ->
                    mutate { it.copy(starting = false, run = run) }
                    if (!TrainingStatus.isFinished(run.status)) poll(run.id) else finished()
                }
                .onFailure { e -> mutate { it.copy(starting = false, error = e.toUiText()) } }
        }
    }

    fun retry() {
        val run = (_state.value as? UiState.Content)?.data?.run ?: return
        mutate { it.copy(starting = true, error = null) }
        viewModelScope.launch {
            attempt { repository.retry(run.id) }
                .onSuccess { next ->
                    mutate { it.copy(starting = false, run = next) }
                    poll(next.id)
                }
                .onFailure { e -> mutate { it.copy(starting = false, error = e.toUiText()) } }
        }
    }

    private fun poll(runId: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            val start = TimeSource.Monotonic.markNow()
            while (start.elapsedNow() < TrainingPolling.limit) {
                delay(TrainingPolling.interval)
                val result = attempt { repository.run(runId) }
                result.onSuccess { run ->
                    mutate { it.copy(run = run) }
                    if (TrainingStatus.isFinished(run.status)) {
                        finished()
                        return@launch
                    }
                }
                // A failed poll is just tried again; the job keeps running on the server.
            }
            mutate { it.copy(error = UiText.Res(com.example.volatiledemandforecasting.R.string.train_timeout)) }
        }
    }

    /** The outcome changes what the store can do (AI forecasts), so everything that shows it reloads. */
    private fun finished() = activeStore.dataChanged()

    private fun mutate(transform: (TrainingData) -> TrainingData) {
        _state.update { state -> if (state is UiState.Content) UiState.Content(transform(state.data)) else state }
    }
}
