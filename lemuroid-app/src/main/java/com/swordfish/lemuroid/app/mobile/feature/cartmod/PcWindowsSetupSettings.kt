package com.swordfish.lemuroid.app.mobile.feature.cartmod

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.shared.game.GameProcessLock
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.library.pc.PcCartridgeStore
import com.swordfish.lemuroid.lib.library.pc.PcGuestDiskStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun PcWindowsSetupSettings(
    enabled: Boolean,
    revision: Int,
    cartridgeId: String?,
    games: List<Game>,
    onOpenGuest: (Game) -> Unit,
    visible: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember(context) { PcCartridgeStore(context.filesDir) }
    var entries by remember { mutableStateOf<List<Pair<PcCartridgeStore.Entry, Int>>>(emptyList()) }
    var selected by remember { mutableStateOf<PcCartridgeStore.Entry?>(null) }
    var busy by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf(false) }

    LaunchedEffect(revision, refresh, cartridgeId) {
        try {
            entries =
                withContext(Dispatchers.IO) {
                    store.entries()
                        .filter { cartridgeId == null || it.id.toString() == cartridgeId }
                        .map { it to PcGuestDiskStore(store.workspace(it)).disks().size }
                }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            message = "PC cartridge list could not be read. Existing guest files are retained."
        }
    }
    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val target = selected
            if (uri != null && target != null && !busy) {
                busy = true
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            check(!GameProcessLock.isHeldByAnotherProcess(context)) { "Close the running game first" }
                            val cancel = { coroutineContext.ensureActive() }
                            checkNotNull(context.contentResolver.openInputStream(uri)).use { input ->
                                PcGuestDiskStore(store.workspace(target)).importPrepared(input, cancel)
                            }
                        }
                        message = "Guest disk copied. Select it in the cartridge's startup menu."
                        refresh++
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        message = "Guest import failed. Close games and check the image and storage. No disk replaced."
                    } finally {
                        busy = false
                    }
                }
            }
        }
    if (!visible) return
    LemuroidCardSettingsGroup(title = { Text("Manage PC installations") }) {
        Text("Select a cartridge to continue its setup.", Modifier.padding(horizontal = 16.dp))
        LemuroidSettingsMenuLink(
            title = { Text("Windows setup guide") },
            subtitle = { Text("Installation, startup and recovery instructions") },
            onClick = { instructions = true },
        )
        LemuroidSettingsMenuLink(
            title = { Text("Refresh guest disk status") },
            enabled = !busy,
            onClick = { refresh++ },
        )
        if (entries.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (cartridgeId == null) "No PC installations yet" else "Installation not found",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text("Set up a PC game from the workshop to begin.", style = MaterialTheme.typography.bodySmall)
            }
        }
        for ((entry, count) in entries) {
            val canonical =
                games.singleOrNull {
                    it.cartridgeId == entry.id.toString() && it.fileUri == Uri.fromFile(entry.content).toString()
                }
            ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(entry.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (count == 0) "Setup needed" else "Guest disk present · boot not verified",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            LemuroidSettingsMenuLink(
                title = { Text("Open guest setup") },
                subtitle = { Text("Start the same cartridge used by Shelf") },
                enabled = enabled && !busy && canonical != null,
                onClick = { canonical?.let(onOpenGuest) },
            )
            LemuroidSettingsMenuLink(
                title = { Text("Import prepared Windows IMG") },
                subtitle = {
                    Text(
                        if (count == 0) "Optional: copy an existing guest disk" else "Existing guest disk is protected",
                    )
                },
                enabled = enabled && !busy && count == 0,
                onClick = {
                    selected = entry
                    imagePicker.launch(arrayOf("*/*"))
                },
            )
        }
        if (message.isNotBlank()) Text(message, Modifier.padding(16.dp))
    }
    if (instructions) {
        AlertDialog(
            onDismissRequest = { instructions = false },
            title = { Text("Windows setup guide") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("1 · Install Windows", style = MaterialTheme.typography.titleSmall)
                    Text("Open a PC cartridge and boot your own installation media.")
                    Text("2 · Install the game", style = MaterialTheme.typography.titleSmall)
                    Text("Run the installed OS, install the game and shut down Windows normally.")
                    Text("3 · Configure startup", style = MaterialTheme.typography.titleSmall)
                    Text("Create a Startup shortcut to the EXE; set Start in to the game folder.")
                    Text("For imports with an EXE path, copy the CARTPLAY shortcut from the content drive to Startup.")
                    Text("4 · Select guest auto-start", style = MaterialTheme.typography.titleSmall)
                    Text("Set the installed OS as auto-start in the core menu. Shelf opens the same guest.")
                    Text("Recover setup", style = MaterialTheme.typography.titleSmall)
                    Text("Remove the guest startup shortcut and reopen the core menu.")
                }
            },
            confirmButton = { TextButton(onClick = { instructions = false }) { Text("Close") } },
        )
    }
}
