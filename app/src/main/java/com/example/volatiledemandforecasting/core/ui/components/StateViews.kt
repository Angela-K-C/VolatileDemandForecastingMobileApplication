package com.example.volatiledemandforecasting.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.ui.theme.Spacing

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.state_loading)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.xl)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Indeterminate: we only show progress the backend actually reports (spec §8.9).
        CircularProgressIndicator()
        Text(
            description,
            modifier = Modifier.padding(top = Spacing.lg),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Inbox,
    action: (@Composable () -> Unit)? = null,
) = MessageState(icon, title, body, modifier, action)

@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) = MessageState(
    icon = Icons.Outlined.ErrorOutline,
    title = stringResource(R.string.state_error_title),
    body = message,
    modifier = modifier,
    action = onRetry?.let { retry -> { SecondaryButton(stringResource(R.string.state_retry), retry) } },
    isAlert = true,
)

/** Full-screen offline state, for when there is nothing cached to show. */
@Composable
fun OfflineState(onRetry: () -> Unit, modifier: Modifier = Modifier) = MessageState(
    icon = Icons.Outlined.CloudOff,
    title = stringResource(R.string.state_offline_title),
    body = stringResource(R.string.state_offline_body),
    modifier = modifier,
    action = { SecondaryButton(stringResource(R.string.state_retry), onRetry) },
)

/** Slim banner above cached content. Stale data is never shown as freshly loaded (spec §13.3). */
@Composable
fun OfflineBanner(lastSynced: String?, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(Icons.Outlined.CloudOff, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = if (lastSynced != null) {
                    stringResource(R.string.state_offline_banner_synced, lastSynced)
                } else {
                    stringResource(R.string.state_offline_banner)
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun MessageState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier,
    action: (@Composable () -> Unit)?,
    isAlert: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.xl)
            .then(if (isAlert) Modifier.semantics { liveRegion = LiveRegionMode.Assertive } else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            title,
            modifier = Modifier.padding(top = Spacing.lg),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            body,
            modifier = Modifier.padding(top = Spacing.sm),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Column(Modifier.padding(top = Spacing.lg)) { action() }
        }
    }
}
