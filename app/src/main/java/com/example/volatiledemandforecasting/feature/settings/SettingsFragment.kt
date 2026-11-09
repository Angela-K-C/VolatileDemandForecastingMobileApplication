package com.example.volatiledemandforecasting.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.appContainer
import com.example.volatiledemandforecasting.core.datastore.TableDisplay
import com.example.volatiledemandforecasting.core.datastore.ThemeMode
import com.example.volatiledemandforecasting.core.network.attempt
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.data.workspace.StoreDto
import com.example.volatiledemandforecasting.feature.team.roleName
import kotlinx.coroutines.launch

class SettingsFragment : ShellFragment() {
    override val titleRes = R.string.screen_settings

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val navController = findNavController()
        val shellState by shell.state.collectAsStateWithLifecycle()
        val selection by collectSelection()
        val prefs = appContainer.preferences
        val theme by prefs.themeMode.collectAsStateWithLifecycle(ThemeMode.System)
        val display by prefs.tableDisplay.collectAsStateWithLifecycle(TableDisplay.Decimals)
        val scope = rememberCoroutineScope()
        var storeDetail by remember { mutableStateOf<StoreDto?>(null) }

        val selected = (selection as? com.example.volatiledemandforecasting.data.workspace.Selection.Selected)?.store
        // Time zone and currency belong to the store; a failed lookup just leaves them out.
        LaunchedEffect(selected?.storeId) {
            storeDetail = null
            selected?.let { s -> attempt { appContainer.workspaceRepository.store(s.storeId) }.onSuccess { storeDetail = it } }
        }
        LaunchedEffect(Unit) { shell.refreshInvitations() }

        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Section(R.string.set_profile) {
                shellState.user?.let {
                    Value(stringResource(R.string.set_name), it.displayName)
                    Value(stringResource(R.string.auth_email), it.email)
                }
                LinkRow(stringResource(R.string.set_change_password)) { navController.navigate(R.id.changePasswordFragment) }
                LinkRow(
                    if (shellState.pendingInvitations > 0) stringResource(R.string.set_my_invitations_count, shellState.pendingInvitations)
                    else stringResource(R.string.set_my_invitations),
                ) { navController.navigate(R.id.myInvitationsFragment) }
            }

            Section(R.string.set_workspace) {
                if (selected == null) {
                    Text(stringResource(R.string.set_no_store), style = MaterialTheme.typography.bodyMedium)
                } else {
                    Value(stringResource(R.string.set_workspace), if (selected.isReference) stringResource(R.string.sample_shop_name) else selected.workspaceName)
                    Value(stringResource(R.string.set_role), roleName(if (selected.isReference) "REFERENCE" else selected.role))
                    Value(stringResource(R.string.set_store), selected.storeName)
                    storeDetail?.let {
                        Value(stringResource(R.string.set_timezone), it.timezone)
                        Value(stringResource(R.string.set_currency), it.currency)
                    }
                }
                LinkRow(stringResource(R.string.set_switch)) { navController.navigate(R.id.workspacePickerFragment) }
            }

            Section(R.string.set_appearance) {
                Text(stringResource(R.string.set_theme), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = theme == mode,
                            onClick = { scope.launch { prefs.setThemeMode(mode) } },
                            label = {
                                Text(
                                    stringResource(
                                        when (mode) {
                                            ThemeMode.System -> R.string.set_theme_system
                                            ThemeMode.Light -> R.string.set_theme_light
                                            ThemeMode.Dark -> R.string.set_theme_dark
                                        }
                                    )
                                )
                            },
                        )
                    }
                }
                Text(stringResource(R.string.set_numbers), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    TableDisplay.entries.forEach { mode ->
                        FilterChip(
                            selected = display == mode,
                            onClick = { scope.launch { prefs.setTableDisplay(mode) } },
                            label = { Text(stringResource(if (mode == TableDisplay.Decimals) R.string.set_numbers_decimals else R.string.set_numbers_whole)) },
                        )
                    }
                }
            }

            Section(R.string.set_about) {
                LinkRow(stringResource(R.string.screen_help)) { navController.navigate(R.id.helpFragment) }
                Value(stringResource(R.string.set_version), appVersion())
                Text(stringResource(R.string.set_notifications_later), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    private fun appVersion(): String =
        runCatching { requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName }.getOrNull().orEmpty()
}

@Composable
private fun Section(title: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        content()
        HorizontalDivider()
    }
}

@Composable
private fun Value(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    Text(
        label,
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = Spacing.sm),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}
