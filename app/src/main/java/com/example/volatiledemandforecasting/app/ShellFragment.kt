package com.example.volatiledemandforecasting.app

import android.content.pm.ApplicationInfo
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.navigation.MainTab
import com.example.volatiledemandforecasting.app.navigation.navigateToPlaceholder
import com.example.volatiledemandforecasting.app.navigation.navigateToTab
import com.example.volatiledemandforecasting.core.ui.components.AppScaffold
import com.example.volatiledemandforecasting.core.ui.components.MenuEntry

/**
 * A [ComposeFragment] that sits inside the app shell: top bar with the workspace chip and
 * Options menu, plus the bottom navigation bar when [tab] is set (spec §5).
 */
abstract class ShellFragment : ComposeFragment() {

    private val shell: ShellViewModel by activityViewModels()

    @get:StringRes
    protected abstract val titleRes: Int

    /** The bottom-navigation tab this screen belongs to, or null for a supporting screen. */
    protected open val tab: MainTab? = null

    /** Supporting screens (no bottom bar) get a back arrow. */
    protected open val showBackButton: Boolean get() = tab == null

    /** A contextual action for the top bar, left of the Options menu. */
    @Composable
    protected open fun RowScope.ContextualAction() {}

    @Composable
    protected abstract fun ScreenContent(padding: PaddingValues)

    @Composable
    final override fun Content() {
        val navController = findNavController()
        val state by shell.state.collectAsStateWithLifecycle()
        var showAbout by rememberSaveable { mutableStateOf(false) }

        // Screens that are not built yet open a placeholder that names their build step.
        val openPlaceholder = { title: Int, step: Int -> navController.navigateToPlaceholder(title, step) }

        val isDebuggable = (LocalContext.current.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        val optionsMenu = buildList {
            add(MenuEntry(stringResource(R.string.menu_switch_workspace), { openPlaceholder(R.string.screen_workspace_picker, 3) }, Icons.Outlined.SwapHoriz))
            add(MenuEntry(stringResource(R.string.menu_model_info), { openPlaceholder(R.string.screen_model_info, 3) }, Icons.Outlined.AccountTree))
            if (state.canManageTeam) {
                add(MenuEntry(stringResource(R.string.menu_manage_team), { openPlaceholder(R.string.screen_team, 5) }, Icons.Outlined.Groups))
            }
            add(MenuEntry(stringResource(R.string.menu_settings), { openPlaceholder(R.string.screen_settings, 5) }, Icons.Outlined.Settings))
            add(MenuEntry(stringResource(R.string.menu_help), { openPlaceholder(R.string.screen_help, 5) }, Icons.AutoMirrored.Outlined.HelpOutline))
            add(MenuEntry(stringResource(R.string.menu_about), { showAbout = true }, Icons.Outlined.Info))
            // Enabled in build step 2, when there is a session to end.
            add(MenuEntry(stringResource(R.string.menu_sign_out), {}, Icons.AutoMirrored.Outlined.Logout, enabled = false))
            if (isDebuggable) {
                add(MenuEntry(stringResource(R.string.menu_dev_gallery), { navController.navigate(R.id.devGalleryFragment) }, Icons.Outlined.Widgets))
            }
        }

        AppScaffold(
            title = stringResource(titleRes),
            workspace = state.workspace,
            onWorkspaceClick = { openPlaceholder(R.string.screen_workspace_picker, 3) },
            optionsMenu = optionsMenu,
            selectedTab = tab,
            onTabSelected = navController::navigateToTab,
            onBack = if (showBackButton) ({ navController.navigateUp() }) else null,
            actions = { ContextualAction() },
            content = { padding -> ScreenContent(padding) },
        )

        if (showAbout) AboutDialog(onDismiss = { showAbout = false })
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_title)) },
        text = { Text(stringResource(R.string.about_body, stringResource(R.string.app_name), version)) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
}
