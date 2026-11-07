package com.example.volatiledemandforecasting.feature.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.navigation.MainTab
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.feature.placeholder.NotBuiltYet

class DashboardFragment : ShellFragment() {
    override val titleRes = R.string.nav_dashboard
    override val tab = MainTab.Dashboard

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val user = shell.state.collectAsStateWithLifecycle().value.user
        Column(Modifier.padding(padding)) {
            // Real data from GET /v1/me; it shows the sign-in worked. The dashboard proper is step 3.
            if (user != null) {
                Text(
                    stringResource(R.string.signed_in_as, user.displayName, user.email),
                    modifier = Modifier.padding(Spacing.lg),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            NotBuiltYet(step = 3)
        }
    }
}
