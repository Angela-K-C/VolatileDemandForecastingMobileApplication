package com.example.volatiledemandforecasting.feature.dashboard

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.navigation.MainTab
import com.example.volatiledemandforecasting.feature.placeholder.NotBuiltYet

class DashboardFragment : ShellFragment() {
    override val titleRes = R.string.nav_dashboard
    override val tab = MainTab.Dashboard

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        NotBuiltYet(step = 3, modifier = Modifier.padding(padding))
    }
}
