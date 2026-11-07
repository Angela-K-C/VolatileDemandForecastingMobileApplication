package com.example.volatiledemandforecasting.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.navigation.MainTab

/**
 * The shell every screen sits in (spec §5.1): title, workspace chip, a contextual action,
 * the Options menu and, on the four main destinations, the bottom navigation bar.
 */
@Composable
fun AppScaffold(
    title: String,
    workspace: WorkspaceChipUi?,
    onWorkspaceClick: () -> Unit,
    optionsMenu: List<MenuEntry>,
    selectedTab: MainTab?,
    onTabSelected: (MainTab) -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title, workspace, onWorkspaceClick, optionsMenu, onBack, actions) },
        bottomBar = { if (selectedTab != null) AppBottomNavigationBar(selectedTab, onTabSelected) },
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    workspace: WorkspaceChipUi?,
    onWorkspaceClick: () -> Unit,
    optionsMenu: List<MenuEntry>,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
        TopAppBar(
            title = { Text(title, maxLines = 1) },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                }
            },
            actions = {
                actions()
                OptionsMenu(optionsMenu)
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        )
        WorkspaceChip(workspace, onWorkspaceClick)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun AppBottomNavigationBar(selected: MainTab, onSelected: (MainTab) -> Unit, modifier: Modifier = Modifier) {
    NavigationBar(modifier, containerColor = MaterialTheme.colorScheme.surface) {
        MainTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { if (tab != selected) onSelected(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.labelRes)) },
                alwaysShowLabel = true,
            )
        }
    }
}
