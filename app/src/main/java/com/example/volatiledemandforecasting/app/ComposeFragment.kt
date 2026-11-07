package com.example.volatiledemandforecasting.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
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
        setContent { VolatileDemandForecastingTheme { Content() } }
    }

    @Composable
    protected abstract fun Content()
}
