package com.example.volatiledemandforecasting.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volatiledemandforecasting.core.datastore.StoreSelection
import com.example.volatiledemandforecasting.core.network.attempt
import com.example.volatiledemandforecasting.core.ui.components.UiState
import com.example.volatiledemandforecasting.core.ui.components.toUiState
import com.example.volatiledemandforecasting.data.catalog.ProductRepository
import com.example.volatiledemandforecasting.data.forecast.BatchRepository
import com.example.volatiledemandforecasting.data.forecast.ForecastRepository
import com.example.volatiledemandforecasting.data.forecast.ForecastSummaryDto
import com.example.volatiledemandforecasting.data.store.CapabilitiesDto
import com.example.volatiledemandforecasting.data.store.HistorySummaryDto
import com.example.volatiledemandforecasting.data.store.ReadinessDto
import com.example.volatiledemandforecasting.data.store.StoreRepository
import com.example.volatiledemandforecasting.data.training.EligibilityCheckDto
import com.example.volatiledemandforecasting.data.training.TrainingRepository
import com.example.volatiledemandforecasting.data.workspace.ActiveStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** How the dashboard describes AI availability (spec §8.4). */
enum class AiStatus { Available, Training, NotEligible, BaselinesOnly }

data class DashboardData(
    val store: StoreSelection,
    val activeProducts: Int,
    val summary: HistorySummaryDto,
    val aiStatus: AiStatus,
    val readiness: ReadinessDto?,
    val recent: List<ForecastSummaryDto>,
    /** "60 of 120 days" toward AI eligibility, from the API's own check. Null when not applicable. */
    val eligibilityProgress: EligibilityCheckDto? = null,
    /** Whether the shop has had a store-wide forecast. */
    val hasStoreForecast: Boolean = false,
) {
    /** The getting-started checklist, for business shops only. */
    val setup: SetupProgress? get() = if (store.isReference) null else SetupProgress(
        hasSales = summary.historyEnd != null,
        hasForecast = hasStoreForecast || recent.isNotEmpty(),
        aiReady = aiStatus == AiStatus.Available,
    )

    val latestForecast: ForecastSummaryDto? get() = recent.firstOrNull()

    /** The demo store's history ends in 2016, so "stale" would always be true there and mean nothing. */
    val showStaleNotice: Boolean get() = !store.isReference && readiness?.stale == true
}

internal fun aiStatusOf(capabilities: CapabilitiesDto): AiStatus = when {
    capabilities.aiAvailable -> AiStatus.Available
    capabilities.aiUnavailableReason == "TRAINING" -> AiStatus.Training
    capabilities.aiUnavailableReason in setOf("NOT_ELIGIBLE", "NOT_TRAINED", "NO_HISTORY") -> AiStatus.NotEligible
    else -> AiStatus.BaselinesOnly
}

class DashboardViewModel(
    private val stores: StoreRepository,
    private val products: ProductRepository,
    private val forecasts: ForecastRepository,
    private val training: TrainingRepository,
    private val batches: BatchRepository,
    private val activeStore: ActiveStore,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<DashboardData>>(UiState.Loading)
    val state: StateFlow<UiState<DashboardData>> = _state.asStateFlow()

    private var current: StoreSelection? = null
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            // Reloads by itself when the user switches workspace.
            combine(activeStore.selected, activeStore.dataRevision) { store, _ -> store }.collect {
                current = it
                load()
            }
        }
    }

    fun load() {
        val store = current ?: return
        loadJob?.cancel()
        _state.value = UiState.Loading
        loadJob = viewModelScope.launch {
            attempt {
                coroutineScope {
                    val summary = async { stores.historySummary(store.storeId) }
                    val capabilities = async { stores.capabilities(store.storeId) }
                    val productCount = async { products.products(store.storeId, "", null, page = 1, pageSize = 1).total }
                    val readiness = async { runCatching { stores.readiness(store.storeId) }.getOrNull() }
                    val recent = async { forecasts.list(store.storeId, replay = null, status = null, page = 1, pageSize = 3, batch = false).items }
                    val storeForecast = async {
                        if (store.isReference) false else runCatching { batches.latest(store.storeId) != null }.getOrDefault(false)
                    }
                    val ai = aiStatusOf(capabilities.await())
                    // Only a business store that isn't eligible yet needs the progress line.
                    val progress = if (!store.isReference && ai == AiStatus.NotEligible) {
                        runCatching { training.eligibility(store.storeId).checks["storeHistoryDays"] }.getOrNull()
                    } else null
                    DashboardData(
                        store = store,
                        activeProducts = productCount.await(),
                        summary = summary.await(),
                        aiStatus = ai,
                        readiness = readiness.await(),
                        recent = recent.await(),
                        eligibilityProgress = progress,
                        hasStoreForecast = storeForecast.await(),
                    )
                }
            }
                .onSuccess { _state.value = UiState.Content(it) }
                .onFailure { _state.value = it.toUiState() }
        }
    }
}
