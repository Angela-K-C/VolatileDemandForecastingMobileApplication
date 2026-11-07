package com.example.volatiledemandforecasting.app

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.ui.components.InlineMessage
import com.example.volatiledemandforecasting.core.ui.components.StatusKind
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val syncedFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

fun formatLastSynced(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    syncedFormat.format(Instant.ofEpochMilli(epochMillis).atZone(zone))

/** Says plainly that this is a saved copy and when it was fetched (spec §13.3). */
@Composable
fun OfflineCopyNotice(savedAtMillis: Long) {
    InlineMessage(
        stringResource(R.string.offline_copy, formatLastSynced(savedAtMillis)),
        StatusKind.Warning,
        Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    )
}
