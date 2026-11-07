package com.example.volatiledemandforecasting.feature.team

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.appViewModels
import com.example.volatiledemandforecasting.core.ui.Formatting
import com.example.volatiledemandforecasting.core.ui.asString
import com.example.volatiledemandforecasting.core.ui.components.EmptyState
import com.example.volatiledemandforecasting.core.ui.components.PrimaryButton
import com.example.volatiledemandforecasting.core.ui.components.UiStateHost
import com.example.volatiledemandforecasting.core.ui.theme.Spacing

class MyInvitationsFragment : ShellFragment() {
    override val titleRes = R.string.screen_my_invitations

    private val viewModel: MyInvitationsViewModel by appViewModels { MyInvitationsViewModel(it.teamRepository) }

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val busy by viewModel.busy.collectAsStateWithLifecycle()
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            viewModel.events.collect { event ->
                when (event) {
                    is InvitationEvent.Message -> Toast.makeText(context, event.text.asString(context), Toast.LENGTH_LONG).show()
                    // New access: reload the profile (workspace list) and the badge.
                    InvitationEvent.Accepted -> shell.refreshProfile()
                }
            }
        }
        // The badge on Settings counts what is left after an answer.
        LaunchedEffect(state) { if (state is com.example.volatiledemandforecasting.core.ui.components.UiState.Empty) shell.refreshInvitations() }

        UiStateHost(
            state, onRetry = viewModel::load, modifier = Modifier.padding(padding),
            empty = { EmptyState(stringResource(R.string.inv_empty_title), stringResource(R.string.inv_empty_body)) },
        ) { invitations ->
            LazyColumn(
                Modifier.padding(padding),
                contentPadding = PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                items(invitations, key = { it.id }) { invitation ->
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Text(invitation.workspaceName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(R.string.inv_as_role, roleName(invitation.role)) +
                                    invitation.expiresAt?.let { " · " + stringResource(R.string.team_expires, Formatting.date(it)) }.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            val working = invitation.id in busy
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                                PrimaryButton(stringResource(R.string.inv_accept), { viewModel.accept(invitation) }, Modifier.weight(1f), enabled = !working)
                                OutlinedButton(onClick = { viewModel.decline(invitation) }, modifier = Modifier.weight(1f), enabled = !working) {
                                    Text(stringResource(R.string.inv_decline))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
