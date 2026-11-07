package com.example.volatiledemandforecasting.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * What a data screen is showing. ViewModels expose this; composables only render it (spec §4.2, §15).
 * Every network screen defines all five states.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>

    /** [message] is already plain language: either a string resource or the API's own `message`. */
    data class Error(val message: String) : UiState<Nothing>
    data object Offline : UiState<Nothing>
}

@Composable
fun <T> UiStateHost(
    state: UiState<T>,
    onRetry: () -> Unit,
    empty: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    when (state) {
        UiState.Loading -> LoadingState(modifier)
        UiState.Empty -> empty()
        is UiState.Error -> ErrorState(state.message, onRetry, modifier)
        UiState.Offline -> OfflineState(onRetry, modifier)
        is UiState.Content -> content(state.data)
    }
}
