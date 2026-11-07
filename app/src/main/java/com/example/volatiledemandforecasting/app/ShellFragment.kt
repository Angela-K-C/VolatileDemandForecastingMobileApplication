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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.navigation.MainTab
import com.example.volatiledemandforecasting.app.navigation.navigateToPlaceholder
import com.example.volatiledemandforecasting.app.navigation.navigateToTab
import com.example.volatiledemandforecasting.core.ui.components.AppScaffold
import com.example.volatiledemandforecasting.core.ui.components.ConfirmationDialog
import com.example.volatiledemandforecasting.core.ui.components.MenuEntry
import com.example.volatiledemandforecasting.data.workspace.Selection

/**
 * A [ComposeFragment] that sits inside the app shell: top bar with the workspace chip and
 * Options menu, plus the bottom navigation bar when [tab] is set (spec §5).
 */
abstract class ShellFragment : ComposeFragment() {

    protected val shell: ShellViewModel by appActivityViewModels {
        ShellViewModel(it.session, it.userRepository, it.authRepository, it.activeStore)
    }

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

    /** The workspace and store the user is working in. Store-scoped screens pass this to [StoreGate]. */
    @Composable
    protected fun collectSelection(): State<Selection> = appContainer.activeStore.state.collectAsStateWithLifecycle()

    @Composable
    final override fun Content() {
        val navController = findNavController()
        val state by shell.state.collectAsStateWithLifecycle()
        var showAbout by rememberSaveable { mutableStateOf(false) }
        var showSignOutConfirm by rememberSaveable { mutableStateOf(false) }

        // Screens that are not built yet open a placeholder that names their build step.
        val openPlaceholder = { title: Int, step: Int -> navController.navigateToPlaceholder(title, step) }

        val isDebuggable = (LocalContext.current.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        val optionsMenu = buildList {
            add(MenuEntry(stringResource(R.string.menu_switch_workspace), { navController.navigate(R.id.workspacePickerFragment) }, Icons.Outlined.SwapHoriz))
            add(MenuEntry(stringResource(R.string.menu_model_info), { navController.navigate(R.id.modelInfoFragment) }, Icons.Outlined.AccountTree))
            if (state.canManageTeam) {
                add(MenuEntry(stringResource(R.string.menu_manage_team), { openPlaceholder(R.string.screen_team, 5) }, Icons.Outlined.Groups))
            }
            add(MenuEntry(stringResource(R.string.menu_settings), { openPlaceholder(R.string.screen_settings, 5) }, Icons.Outlined.Settings))
            add(MenuEntry(stringResource(R.string.menu_help), { openPlaceholder(R.string.screen_help, 5) }, Icons.AutoMirrored.Outlined.HelpOutline))
            add(MenuEntry(stringResource(R.string.menu_about), { showAbout = true }, Icons.Outlined.Info))
            add(MenuEntry(stringResource(R.string.menu_sign_out), { showSignOutConfirm = true }, Icons.AutoMirrored.Outlined.Logout))
            if (isDebuggable) {
                add(MenuEntry(stringResource(R.string.menu_dev_gallery), { navController.navigate(R.id.devGalleryFragment) }, Icons.Outlined.Widgets))
            }
        }

        AppScaffold(
            title = stringResource(titleRes),
            workspace = state.workspace,
            onWorkspaceClick = { navController.navigate(R.id.workspacePickerFragment) },
            optionsMenu = optionsMenu,
            selectedTab = tab,
            onTabSelected = navController::navigateToTab,
            onBack = if (showBackButton) ({ navController.navigateUp() }) else null,
            actions = { ContextualAction() },
            content = { padding -> ScreenContent(padding) },
        )

        if (showAbout) AboutDialog(onDismiss = { showAbout = false })
        if (showSignOutConfirm) {
            ConfirmationDialog(
                title = stringResource(R.string.sign_out_title),
                body = stringResource(R.string.sign_out_body),
                confirmLabel = stringResource(R.string.menu_sign_out),
                onConfirm = {
                    showSignOutConfirm = false
                    shell.signOut()
                },
                onDismiss = { showSignOutConfirm = false },
            )
        }
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
