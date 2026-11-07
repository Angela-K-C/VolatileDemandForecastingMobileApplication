package com.example.volatiledemandforecasting.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.volatiledemandforecasting.R

/** One row of a menu. Hidden entries are simply left out of the list by the caller. */
data class MenuEntry(
    val label: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    val enabled: Boolean = true,
    val destructive: Boolean = false,
)

@Composable
private fun MenuEntries(entries: List<MenuEntry>, dismiss: () -> Unit) {
    entries.forEach { entry ->
        DropdownMenuItem(
            text = {
                Text(
                    entry.label,
                    color = if (entry.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
            },
            leadingIcon = entry.icon?.let { icon ->
                {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (entry.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            enabled = entry.enabled,
            onClick = {
                dismiss()
                entry.onClick()
            },
        )
    }
}

/**
 * Options menu (spec §6.1): the overflow icon in the top app bar.
 * Screen- and application-level actions only, never the primary action of a screen.
 */
@Composable
fun OptionsMenu(entries: List<MenuEntry>, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.options_menu_description))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MenuEntries(entries) { expanded = false }
        }
    }
}

/**
 * Context menu (spec §6.2): a long-press on one specific item opens a menu anchored to that item.
 * A plain tap still runs [onClick]. Destructive entries must open a [ConfirmationDialog].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContextMenuBox(
    entries: List<MenuEntry>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    Box(
        modifier.combinedClickable(
            onClick = onClick,
            onLongClickLabel = stringResource(R.string.context_menu_long_press_label),
            onLongClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                expanded = true
            },
        )
    ) {
        content()
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MenuEntries(entries) { expanded = false }
        }
    }
}

/**
 * Popup menu (spec §6.3): a tap on a filter, sort or view control opens quick choices.
 * It changes a temporary view option; it is not an action menu for an object.
 */
@Composable
fun <T> PopupMenuButton(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp)) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                val isSelected = option == selected
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    trailingIcon = if (isSelected) {
                        { Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.popup_menu_selected)) }
                    } else null,
                    onClick = {
                        expanded = false
                        onSelected(option)
                    },
                )
            }
        }
    }
}
