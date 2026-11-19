package com.swordfish.lemuroid.app.mobile.feature.settings.advanced

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import com.swordfish.lemuroid.lib.storage.cache.StorageReviewCategories
import com.swordfish.lemuroid.lib.storage.cache.StorageUsageReview

@Composable
internal fun StorageSettings(viewModel: AdvancedSettingsViewModel) {
    val state = viewModel.storageState.collectAsState().value
    val context = LocalContext.current
    val confirmation = remember { mutableStateOf(false) }
    val details = remember { mutableStateOf(false) }
    val report = state.report
    val categories = remember(report) { report?.let(StorageReviewCategories::summarize).orEmpty() }

    fun size(bytes: Long) = Formatter.formatShortFileSize(context, bytes)

    LemuroidCardSettingsGroup(title = { Text("Storage review") }) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (report == null) {
                Text(
                    "See how much space your games, emulators and saves use.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    size(report.internalBytes + report.externalBytes),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    "App data and cache",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StorageValue("Reclaimable game copies", size(report.reclaimableBytes))
                Text(
                    if (report.reclaimableBytes == 0L) "No staged copies to clear." else "Original games are retained.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (report.incompleteEntries > 0) {
                    Text("Some storage could not be inspected.", style = MaterialTheme.typography.bodySmall)
                }
            }
            state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        LemuroidSettingsMenuLink(
            enabled = !state.busy,
            title = {
                Text(
                    when {
                        state.busy -> "Reviewing storage…"
                        report == null -> "Review storage"
                        else -> "Refresh"
                    },
                )
            },
            onClick = { viewModel.reviewStorage() },
        )
        if (report != null) {
            LemuroidSettingsMenuLink(
                title = { Text("Storage breakdown") },
                onClick = { details.value = true },
            )
            LemuroidSettingsMenuLink(
                enabled = !state.busy && report.reclaimableBytes > 0,
                title = { Text("Clear staged game copies") },
                onClick = { confirmation.value = true },
            )
        }
    }
    if (details.value && report != null) {
        AlertDialog(
            onDismissRequest = { details.value = false },
            title = { Text("Storage breakdown") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    categories.forEach { entry ->
                        StorageValue(entry.category.label, size(entry.bytes))
                    }
                    Text(
                        "File-size estimates may differ from Android's totals. Only staged copies can be cleared.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { details.value = false }) { Text("Done") }
            },
            dismissButton = {
                TextButton(onClick = { copyStorageReport(context, report) }) { Text("Copy details") }
            },
        )
    }
    if (confirmation.value) {
        AlertDialog(
            onDismissRequest = { confirmation.value = false },
            title = { Text("Clear staged game copies?") },
            text = {
                Text("Games may take longer to reopen. Original media must remain accessible. Saves are retained.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmation.value = false
                        viewModel.reviewStorage(clear = true)
                    },
                ) { Text("Clear copies") }
            },
            dismissButton = {
                TextButton(onClick = { confirmation.value = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun StorageValue(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}

private fun copyStorageReport(
    context: Context,
    report: StorageUsageReview.Report,
) {
    val content =
        buildString {
            appendLine("CARTDECK storage review (logical bytes)")
            appendLine("Internal: ${report.internalBytes}")
            appendLine("App-specific: ${report.externalBytes}")
            appendLine("Reclaimable: ${report.reclaimableBytes}")
            appendLine("Incomplete entries: ${report.incompleteEntries}")
            report.rows.forEach { row ->
                appendLine("${row.label}: ${row.bytes} bytes; reclaimable=${row.reclaimableBytes}")
            }
        }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("CARTDECK storage review", content))
}
