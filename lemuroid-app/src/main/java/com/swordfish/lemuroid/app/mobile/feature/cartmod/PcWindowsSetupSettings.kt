package com.swordfish.lemuroid.app.mobile.feature.cartmod

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.library.pc.PcCartridgeStore
import com.swordfish.lemuroid.lib.library.pc.PcGuestDiskStore
import com.swordfish.lemuroid.lib.library.pc.PcWindowsLaunchRecipe
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
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember(context) { PcCartridgeStore(context.filesDir) }
    var entries by remember { mutableStateOf<List<Pair<PcCartridgeStore.Entry, Int>>>(emptyList()) }
    var selected by remember { mutableStateOf<PcCartridgeStore.Entry?>(null) }
    var recipeEntry by remember { mutableStateOf<PcCartridgeStore.Entry?>(null) }
    var recipePath by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }

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
    WorkshopPanel("Manage installations", "Manage guest disks and launch setup") {
        LemuroidSettingsMenuLink(
            title = { Text("Refresh guest disk status") },
            enabled = !busy,
            onClick = { refresh++ },
        )
        if (entries.isEmpty()) {
            Text(if (cartridgeId == null) "No managed PC cartridges yet." else "Installation not found.")
        }
        for ((entry, count) in entries) {
            val canonical =
                games.singleOrNull {
                    it.cartridgeId == entry.id.toString() && it.fileUri == Uri.fromFile(entry.content).toString()
                }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(entry.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (count == 0) "Setup needed" else "Guest disk present · boot not verified",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    LemuroidSettingsMenuLink(
                        title = { Text("Open guest setup") },
                        subtitle = { Text("Start the same cartridge used by Shelf") },
                        enabled = enabled && !busy && canonical != null,
                        onClick = { canonical?.let(onOpenGuest) },
                    )
                    LemuroidSettingsMenuLink(
                        title = { Text("Configure installed executable") },
                        subtitle = { Text("Persist the guest EXE path without modifying installation media") },
                        enabled = enabled && !busy,
                        onClick = {
                            scope.launch {
                                try {
                                    recipePath =
                                        withContext(Dispatchers.IO) {
                                            store.launchRecipe(entry)?.executable.orEmpty()
                                        }
                                    recipeEntry = entry
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    message = "Existing launch recipe could not be read."
                                }
                            }
                        },
                    )
                    if (recipeEntry?.id == entry.id) {
                        OutlinedTextField(
                            value = recipePath,
                            onValueChange = { recipePath = it },
                            label = { Text("Windows executable path") },
                            singleLine = true,
                            enabled = !busy,
                        )
                        TextButton(
                            enabled = !busy,
                            onClick = {
                                busy = true
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            check(!GameProcessLock.isHeldByAnotherProcess(context))
                                            store.setLaunchRecipe(entry, PcWindowsLaunchRecipe(recipePath))
                                        }
                                        message = "Executable path saved. Launch it after Windows boots."
                                        recipeEntry = null
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (_: Exception) {
                                        message = "Could not save executable path. Close games and check the path."
                                    } finally {
                                        busy = false
                                    }
                                }
                            },
                        ) {
                            Text("Save executable")
                        }
                    }
                    LemuroidSettingsMenuLink(
                        title = { Text("Import prepared Windows IMG") },
                        subtitle = {
                            Text(
                                if (count == 0) "Copy an existing guest disk" else "Existing guest disk is protected",
                            )
                        },
                        enabled = enabled && !busy && count == 0,
                        onClick = {
                            selected = entry
                            imagePicker.launch(arrayOf("*/*"))
                        },
                    )
                }
            }
        }
        if (message.isNotBlank()) Text(message)
    }
}
