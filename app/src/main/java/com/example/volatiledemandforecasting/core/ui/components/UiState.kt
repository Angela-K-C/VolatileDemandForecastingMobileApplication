package com.example.volatiledemandforecasting.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.volatiledemandforecasting.core.network.toUiText
import com.example.volatiledemandforecasting.core.ui.UiText
import com.example.volatiledemandforecasting.core.ui.asString

/**
 * What a data screen is showing. ViewModels expose this; composables only render it (spec §4.2, §15).
 * Every network screen defines all five states.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>

    /** [message] is already plain language: either a string resource or the API's own `message`. */
    data class Error(val message: UiText) : UiState<Nothing>
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
        is UiState.Error -> ErrorState(state.message.asString(), onRetry, modifier)
        UiState.Offline -> OfflineState(onRetry, modifier)
        is UiState.Content -> content(state.data)
    }
}

/** A failed load: no connection becomes the Offline state, anything else an Error with a plain-language reason. */
fun Throwable.toUiState(): UiState<Nothing> =
    if (this is java.io.IOException) UiState.Offline else UiState.Error(toUiText())
