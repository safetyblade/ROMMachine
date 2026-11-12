package com.swordfish.lemuroid.app.mobile.feature.settings.supported

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * P1-S1: an offline guide, not a parallel importer or an emulator configuration panel.
 * The existing Settings -> ROM folder action is used for adding the player's own files.
 */
@Composable
fun SupportedGamesScreen(
    modifier: Modifier = Modifier,
    onAddGames: () -> Unit,
) {
    val context = LocalContext.current
    val entries = remember(context) { SupportedGamesCatalog.entries(context) }
    var search by rememberSaveable { mutableStateOf("") }
    var expandedSystem by rememberSaveable { mutableStateOf<String?>(null) }
    var runtime by remember { mutableStateOf<GameGuideRuntime?>(null) }

    LaunchedEffect(context) {
        runtime = withContext(Dispatchers.IO) { SupportedGamesCatalog.currentRuntime(context.applicationContext) }
    }

    val visible = remember(entries, search) { SupportedGamesCatalog.search(entries, search) }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.cartdeck_formats_intro),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = onAddGames) {
            Text(stringResource(R.string.cartdeck_formats_choose_folder))
        }
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.cartdeck_formats_search)) },
            singleLine = true,
        )
        Text(
            text = stringResource(R.string.cartdeck_formats_count, visible.size, entries.size),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (visible.isEmpty()) {
            Text(
                text = stringResource(R.string.cartdeck_formats_no_results),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(visible, key = { it.system.id.dbname }) { entry ->
                    SupportedGameRow(
                        entry = entry,
                        runtime = runtime,
                        expanded = expandedSystem == entry.system.id.dbname,
                        onToggle = {
                            expandedSystem =
                                if (expandedSystem == entry.system.id.dbname) {
                                    null
                                } else {
                                    entry.system.id.dbname
                                }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportedGameRow(
    entry: SupportedGameEntry,
    runtime: GameGuideRuntime?,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = entry.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (expanded) "−" else "+",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text =
                    stringResource(
                        R.string.cartdeck_formats_files,
                        entry.extensions.joinToString(", ") { "." + it },
                    ),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (expanded) {
                entry.noteResId?.let { note ->
                    Text(
                        text = stringResource(note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val setupMessage =
                    when (SupportedGamesCatalog.status(entry, runtime)) {
                        GameGuideSetup.CHECKING -> R.string.cartdeck_formats_status_checking
                        GameGuideSetup.DEVICE_UNSUPPORTED -> R.string.cartdeck_formats_status_device
                        GameGuideSetup.ENGINE_MISSING -> R.string.cartdeck_formats_status_engine
                        GameGuideSetup.FIRMWARE_MISSING -> R.string.cartdeck_formats_status_firmware
                        GameGuideSetup.ENGINE_PRESENT -> R.string.cartdeck_formats_status_present
                    }
                Text(
                    text = stringResource(setupMessage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text =
                        stringResource(
                            R.string.cartdeck_formats_engine_details,
                            if (entry.system.nativeBackend == "hakux") {
                                "hakuX (SDL3/Vulkan)"
                            } else {
                                entry.system.systemCoreConfigs.joinToString(
                                    " / ",
                                ) {
                                    it.coreID.coreDisplayName
                                }
                            },
                        ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
