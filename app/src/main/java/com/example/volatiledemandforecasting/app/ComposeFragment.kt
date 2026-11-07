package com.example.volatiledemandforecasting.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.volatiledemandforecasting.core.datastore.ThemeMode
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.example.volatiledemandforecasting.core.ui.theme.VolatileDemandForecastingTheme

/**
 * Every destination is a Fragment that hosts one [ComposeView] (spec §4.2).
 * Subclasses only implement [Content]; no XML layout is involved.
 */
abstract class ComposeFragment : Fragment() {

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        // `this` is the ComposeView, which has its own Content(); name the Fragment explicitly.
        setContent {
            val mode by appContainer.preferences.themeMode.collectAsState(initial = ThemeMode.System)
            val dark = when (mode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            VolatileDemandForecastingTheme(darkTheme = dark) { this@ComposeFragment.Content() }
        }
    }

    @Composable
    protected abstract fun Content()
}
