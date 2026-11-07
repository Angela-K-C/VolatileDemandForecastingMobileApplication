package com.example.volatiledemandforecasting.feature.forecasts.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volatiledemandforecasting.core.network.ApiException
import com.example.volatiledemandforecasting.core.network.attempt
import com.example.volatiledemandforecasting.core.network.toUiText
import com.example.volatiledemandforecasting.core.ui.UiText
import com.example.volatiledemandforecasting.data.forecast.ApiErrorDto
import com.example.volatiledemandforecasting.data.forecast.ForecastRepository
import com.example.volatiledemandforecasting.data.forecast.JobDto
import com.example.volatiledemandforecasting.data.forecast.JobStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

sealed interface ProgressState {
    /** [status] is the backend's own status. We show no percentage because none is reported (spec §8.9). */
    data class Running(val status: String) : ProgressState
    data class Failed(val message: UiText, val retryable: Boolean) : ProgressState
    /** Polling stopped after 2 minutes. The job is still on the server. */
    data object TimedOut : ProgressState
    /** Several polls in a row couldn't reach the server. */
    data class Unreachable(val message: UiText) : ProgressState
}

/** Polling pace from api.md: every 1 s for the first 10 s, then every 3 s, stopping after 2 minutes. */
internal object PollingSchedule {
    val fastPhase = 10.seconds
    val limit = 120.seconds
    fun delayAfter(elapsed: Duration): Duration = if (elapsed < fastPhase) 1.seconds else 3.seconds
    fun isExpired(elapsed: Duration) = elapsed >= limit
}

internal fun ApiErrorDto.toApiException() = ApiException(httpStatus = 0, code = code, message = message)

class ForecastProgressViewModel(
    private var jobId: String,
    val forecastId: String,
    private val forecasts: ForecastRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ProgressState>(ProgressState.Running(JobStatus.QUEUED))
    val state: StateFlow<ProgressState> = _state.asStateFlow()

    private val _completed = Channel<String>(Channel.BUFFERED)
    /** Emits the forecast id once, when the job completes. */
    val completed: Flow<String> = _completed.receiveAsFlow()

    private var pollJob: Job? = null

    init {
        startPolling()
    }

    fun startPolling() {
        pollJob?.cancel()
        _state.value = ProgressState.Running(JobStatus.QUEUED)
        pollJob = viewModelScope.launch {
            val start = kotlin.time.TimeSource.Monotonic.markNow()
            var failedPolls = 0
            while (true) {
                val elapsed = start.elapsedNow()
                if (PollingSchedule.isExpired(elapsed)) {
                    _state.value = ProgressState.TimedOut
                    return@launch
                }
                val result = attempt { forecasts.job(jobId) }
                result.onSuccess { job ->
                    failedPolls = 0
                    when (job.status) {
                        JobStatus.COMPLETED -> {
                            _completed.send(job.forecastId)
                            return@launch
                        }
                        JobStatus.FAILED -> {
                            _state.value = failure(job)
                            return@launch
                        }
                        else -> _state.value = ProgressState.Running(job.status)
                    }
                }.onFailure { error ->
                    failedPolls++
                    if (failedPolls >= 3) {
                        _state.value = ProgressState.Unreachable(error.toUiText())
                        return@launch
                    }
                }
                delay(PollingSchedule.delayAfter(elapsed))
            }
        }
    }

    private fun failure(job: JobDto): ProgressState.Failed {
        val message = job.error?.toApiException()?.toUiText() ?: UiText.Res(com.example.volatiledemandforecasting.R.string.error_unknown)
        return ProgressState.Failed(message, job.retryable)
    }

    /** Offered only when the job says it is retryable. */
    fun retry() {
        viewModelScope.launch {
            _state.value = ProgressState.Running(JobStatus.QUEUED)
            attempt { forecasts.retry(jobId) }
                .onSuccess {
                    jobId = it.jobId
                    startPolling()
                }
                .onFailure { _state.value = ProgressState.Failed(it.toUiText(), retryable = false) }
        }
    }
}
