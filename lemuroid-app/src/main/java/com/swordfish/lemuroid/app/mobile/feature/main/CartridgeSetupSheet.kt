package com.swordfish.lemuroid.app.mobile.feature.main

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.ControllerSettingsActivity
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameMenuLink
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.UniversalControls
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.EngineFallbackControls
import com.swordfish.lemuroid.app.shared.game.PS2LaunchJournal
import com.swordfish.lemuroid.app.shared.game.engine.EngineSelectionPreferences
import com.swordfish.lemuroid.app.shared.game.engine.NativeEngineRegistry
import com.swordfish.lemuroid.app.shared.game.xbox.XboxEngineStore
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** UI only: all choices use the existing stores; opening/dismissing Setup writes nothing. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CartridgeSetupSheet(
    game: Game,
    onDismiss: () -> Unit,
    onChangeCover: () -> Unit,
    onResetCover: () -> Unit,
    emulator: @Composable () -> Unit,
    onUseOnlineCover: (suspend (Game, Bitmap) -> Boolean)? = null,
) {
    var section by rememberSaveable(game.id) { mutableStateOf<String?>(null) }
    var artworkOpen by rememberSaveable(game.id) { mutableStateOf(false) }
    val system = GameSystem.findById(game.systemId)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.testTag("cartridge_setup").fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Cartridge Setup", style = MaterialTheme.typography.headlineSmall)
            Text(game.displayName(), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { if (section == null) onDismiss() else section = null }) {
                Text(if (section == null) "Close Setup" else "Back to Setup")
            }
            if (section == null) {
                (
                    listOf("Emulator", "Controls", "Display", "Cartridge artwork", "Advanced / Troubleshooting") +
                        if (system.id == SystemID.PLAYSTATION_3) listOf("Manage Saves / PS3 users") else emptyList()
                ).forEach {
                        name ->
                    TextButton(
                        onClick = { section = name },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("cartridge_setup_section_$name"),
                    ) { Text(name) }
                }
            } else {
                Text(section.orEmpty(), style = MaterialTheme.typography.titleLarge)
                when (section) {
                    "Emulator" -> {
                        emulator()
                        if (system.nativeBackend == "hakux") XboxVersionChoice(game)
                    }
                    "Display" ->
                        com.swordfish.lemuroid.app.mobile.feature.gamemenu.DisplaySettingsScreen(
                            game,
                            scrollable = false,
                        )
                    "Controls" -> CartridgeControlChoices(game, system)
                    "Manage Saves / PS3 users" -> com.swordfish.lemuroid.app.mobile.feature.settings.ps3.Ps3Management()
                    "Cartridge artwork" -> {
                        if (onUseOnlineCover != null) {
                            TextButton(
                                onClick = { artworkOpen = true },
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) { Text("Get artwork") }
                        }
                        TextButton(onClick = onChangeCover, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("Upload / replace image")
                        }
                        TextButton(
                            onClick = onResetCover,
                            enabled = game.customCoverUri != null,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) { Text(stringResource(R.string.cartdeck_reset_cover)) }
                    }
                    "Advanced / Troubleshooting" -> {
                        EngineFallbackControls(system)
                        CartridgeDiagnostics(game, system)
                    }
                }
            }
        }
    }
    if (artworkOpen && onUseOnlineCover != null) {
        CoverArtworkPicker(
            game = game,
            onUse = onUseOnlineCover,
            onDismiss = { artworkOpen = false },
            onLocal = {
                artworkOpen = false
                onChangeCover()
            },
        )
    }
}

@Composable
private fun CartridgeControlChoices(
    game: Game,
    system: GameSystem,
) {
    val context = LocalContext.current
    val preferred = EngineSelectionPreferences(context).preferred(system.id, game)
    val config = NativeEngineRegistry.engines(system).firstOrNull { it.id == preferred }?.config
    UniversalControls(game, core = config, physical = {
        GameMenuLink("Open controller mapper") {
            context.startActivity(
                android.content.Intent(context, ControllerSettingsActivity::class.java)
                    .putExtra("cartdeck.game", game).putExtra("cartdeck.mapping_only", true),
            )
        }
    })
}

@Composable
private fun XboxVersionChoice(game: Game) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val store = remember(context) { XboxEngineStore(File(context.filesDir, "native-engines/xbox")) }
    var previous by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var revision by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(revision) { previous = withContext(Dispatchers.IO) { store.canRollback() } }
    if (previous) {
        TextButton(enabled = !busy, onClick = {
            busy = true
            scope.launch {
                val changed = withContext(Dispatchers.IO) { runCatching { store.rollback() }.getOrDefault(false) }
                if (changed) {
                    EngineSelectionPreferences(context).setPreferred(GameSystem.findById(game.systemId), "hakux", game)
                    message = "Previous version selected. HDD and saves retained."
                }
                revision++
                busy = false
            }
        }) { Text("Use previous version") }
    }
    message?.let { Text(it) }
}

@Composable
private fun CartridgeDiagnostics(
    game: Game,
    system: GameSystem,
) {
    val context = LocalContext.current
    var report by remember(game.fileUri) {
        mutableStateOf("No saved diagnostic report. Use the in-game report action if needed.")
    }
    LaunchedEffect(game.fileUri) {
        report =
            withContext(Dispatchers.IO) {
                if (system.id == SystemID.PLAYSTATION_2) {
                    val savedReport = File(context.filesDir, "native/ps2/last-launch-report.txt")
                    val saved =
                        runCatching {
                            savedReport.takeIf { it.length() in 1..16384 }?.readText()
                        }.getOrNull()
                    val journal = PS2LaunchJournal(context).read()
                    saved ?: listOf(
                        "stage", "error", "selectedVersion", "selectedSHA256",
                        "glVendor", "glRenderer", "glVersion",
                    )
                        .mapNotNull { key -> journal.getProperty(key)?.let { "$key=$it" } }
                        .joinToString("\n").ifBlank { "No PS2 launch recorded yet." }
                } else if (system.id == SystemID.XBOX360) {
                    com.swordfish.lemuroid.app.shared.game.xbox360.Xbox360Reports.read(context.filesDir)
                } else if (system.id == SystemID.XBOX) {
                    runCatching {
                        File(context.filesDir, "native/xbox/last-launch-report.txt").inputStream().use {
                            val buffer = ByteArray(16384)
                            var count = 0
                            while (count < buffer.size) {
                                val read = it.read(buffer, count, buffer.size - count)
                                if (read <= 0) break
                                count += read
                            }
                            String(buffer, 0, count, Charsets.UTF_8)
                        }
                    }.getOrDefault("No saved Xbox report. Use Copy engine/GPU report from the in-game menu.")
                } else {
                    "Last failure: " + (EngineSelectionPreferences(context).failureStage(game) ?: "none recorded")
                }
            }
    }
    NativeEngineRegistry.engines(system).forEach { engine ->
        Text(engine.name + ": " + (NativeEngineRegistry.version(context, engine) ?: "Existing bundled engine"))
    }
    Text(report, style = MaterialTheme.typography.bodySmall)
    TextButton(onClick = {
        (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("CARTDECK launch details", report))
    }) { Text("Copy launch details") }
}
