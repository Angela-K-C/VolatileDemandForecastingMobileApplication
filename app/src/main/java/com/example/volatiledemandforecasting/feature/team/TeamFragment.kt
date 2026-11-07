package com.example.volatiledemandforecasting.feature.team

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.appViewModels
import com.example.volatiledemandforecasting.core.ui.Formatting
import com.example.volatiledemandforecasting.core.ui.asString
import com.example.volatiledemandforecasting.core.ui.components.AppTextField
import com.example.volatiledemandforecasting.core.ui.components.ConfirmationDialog
import com.example.volatiledemandforecasting.core.ui.components.ContextMenuBox
import com.example.volatiledemandforecasting.core.ui.components.InlineMessage
import com.example.volatiledemandforecasting.core.ui.components.MenuEntry
import com.example.volatiledemandforecasting.core.ui.components.PrimaryButton
import com.example.volatiledemandforecasting.core.ui.components.SecondaryButton
import com.example.volatiledemandforecasting.core.ui.components.StatusChip
import com.example.volatiledemandforecasting.core.ui.components.StatusKind
import com.example.volatiledemandforecasting.core.ui.components.UiStateHost
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.data.team.AssignableRoles
import com.example.volatiledemandforecasting.data.team.InvitationDto
import com.example.volatiledemandforecasting.data.team.MemberDto
import com.example.volatiledemandforecasting.data.workspace.Roles
import com.example.volatiledemandforecasting.feature.workspace.StoreGate

class TeamFragment : ShellFragment() {
    override val titleRes = R.string.screen_team

    private val viewModel: TeamViewModel by appViewModels { TeamViewModel(it.teamRepository, it.userRepository, it.activeStore) }

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val navController = findNavController()
        val selection by collectSelection()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val invite by viewModel.invite.collectAsStateWithLifecycle()
        val context = LocalContext.current
        var roleFor by remember { mutableStateOf<MemberDto?>(null) }
        var removing by remember { mutableStateOf<MemberDto?>(null) }
        var revoking by remember { mutableStateOf<InvitationDto?>(null) }
        var leaving by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            viewModel.events.collect { event ->
                when (event) {
                    is TeamEvent.Message -> Toast.makeText(context, event.text.asString(context), Toast.LENGTH_LONG).show()
                    TeamEvent.Left -> {
                        shell.refreshProfile()
                        // The workspace is gone for this user: back to choosing another one.
                        navController.popBackStack(R.id.dashboardFragment, false)
                        navController.navigate(R.id.workspacePickerFragment)
                    }
                }
            }
        }

        roleFor?.let { member ->
            RoleDialog(member, onPick = { role -> viewModel.changeRole(member, role); roleFor = null }, onDismiss = { roleFor = null })
        }
        removing?.let { member ->
            ConfirmationDialog(
                stringResource(R.string.team_remove_title),
                stringResource(R.string.team_remove_body, member.displayName),
                stringResource(R.string.team_remove),
                onConfirm = { viewModel.remove(member); removing = null },
                onDismiss = { removing = null },
                destructive = true,
            )
        }
        revoking?.let { invitation ->
            ConfirmationDialog(
                stringResource(R.string.team_revoke_title),
                stringResource(R.string.team_revoke_body, invitation.email),
                stringResource(R.string.team_revoke),
                onConfirm = { viewModel.revoke(invitation); revoking = null },
                onDismiss = { revoking = null },
                destructive = true,
            )
        }
        if (leaving) {
            ConfirmationDialog(
                stringResource(R.string.team_leave_title),
                stringResource(R.string.team_leave_body),
                stringResource(R.string.team_leave),
                onConfirm = { leaving = false; viewModel.leave() },
                onDismiss = { leaving = false },
                destructive = true,
            )
        }

        StoreGate(selection, onChoose = { navController.navigate(R.id.workspacePickerFragment) }, modifier = Modifier.padding(padding)) { store ->
            if (store.isReference) {
                // The demo workspace has no team.
                androidx.compose.foundation.layout.Box(Modifier.padding(padding).padding(Spacing.lg)) {
                    InlineMessage(stringResource(R.string.team_reference), StatusKind.Neutral)
                }
                return@StoreGate
            }
            UiStateHost(state, onRetry = { viewModel.load() }, modifier = Modifier.padding(padding), empty = {}) { data ->
                LazyColumn(
                    Modifier.padding(padding),
                    contentPadding = PaddingValues(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    item { Text(data.store.workspaceName, style = MaterialTheme.typography.titleLarge) }
                    if (data.isOwner) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                Text(stringResource(R.string.team_invite_title), style = MaterialTheme.typography.titleMedium)
                                AppTextField(
                                    invite.email, viewModel::setInviteEmail, stringResource(R.string.auth_email), Modifier.fillMaxWidth(),
                                    error = invite.emailError, keyboardType = KeyboardType.Email, enabled = !invite.sending,
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                    AssignableRoles.all.forEach { role ->
                                        FilterChip(selected = invite.role == role, onClick = { viewModel.setInviteRole(role) }, label = { Text(roleName(role)) })
                                    }
                                }
                                PrimaryButton(stringResource(R.string.team_invite_send), viewModel::sendInvite, Modifier.fillMaxWidth(), loading = invite.sending)
                            }
                        }
                    }
                    item { Text(stringResource(R.string.team_members), style = MaterialTheme.typography.titleMedium) }
                    items(data.members, key = { it.userId }) { member ->
                        val isMe = member.userId == data.myUserId
                        val entries = buildList {
                            if (data.isOwner) {
                                add(MenuEntry(stringResource(R.string.team_change_role), { roleFor = member }, Icons.Outlined.SwapHoriz))
                                if (!isMe) add(MenuEntry(stringResource(R.string.team_remove), { removing = member }, Icons.Outlined.Delete, destructive = true))
                            }
                            if (isMe) add(MenuEntry(stringResource(R.string.team_leave), { leaving = true }, Icons.AutoMirrored.Outlined.ExitToApp, destructive = true))
                        }
                        val row = @Composable { MemberRow(member, isMe) }
                        if (entries.isEmpty()) row() else ContextMenuBox(entries, onClick = {}) { row() }
                    }
                    if (data.isOwner) {
                        item { Text(stringResource(R.string.team_pending), style = MaterialTheme.typography.titleMedium) }
                        if (data.invitations.isEmpty()) {
                            item { Text(stringResource(R.string.team_no_pending), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        items(data.invitations, key = { it.id }) { invitation ->
                            Card(
                                Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            ) {
                                Row(Modifier.padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                        Text(invitation.email, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            roleName(invitation.role) + invitation.expiresAt?.let { " · " + stringResource(R.string.team_expires, Formatting.date(it)) }.orEmpty(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    TextButton(onClick = { revoking = invitation }) { Text(stringResource(R.string.team_revoke), color = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                    if (!data.isOwner) {
                        item {
                            SecondaryButton(stringResource(R.string.team_leave), { leaving = true }, Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun roleName(role: String): String = stringResource(
    when (role) {
        Roles.OWNER -> R.string.role_owner
        Roles.MANAGER -> R.string.role_manager
        Roles.VIEWER -> R.string.role_viewer
        else -> R.string.role_reference
    }
)

@Composable
private fun MemberRow(member: MemberDto, isMe: Boolean) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(member.displayName + if (isMe) " " + stringResource(R.string.team_you) else "", style = MaterialTheme.typography.titleSmall)
            Text(member.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                StatusChip(roleName(member.role), StatusKind.Neutral)
                member.joinedAt?.let { Text(stringResource(R.string.team_joined, Formatting.date(it)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun RoleDialog(member: MemberDto, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(member.role) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.team_change_role_title, member.displayName)) },
        text = {
            Column {
                AssignableRoles.all.forEach { role ->
                    Row(
                        Modifier.fillMaxWidth().selectable(selected == role, role = Role.RadioButton, onClick = { selected = role }).padding(vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected == role, onClick = null)
                        Text(roleName(role), Modifier.padding(start = Spacing.md))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(selected) }) { Text(stringResource(R.string.action_ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
