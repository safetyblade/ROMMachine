package com.swordfish.lemuroid.app.mobile.feature.settings.general

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.swordfish.lemuroid.app.shared.game.GameProcessLock
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
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
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember(context) { PcCartridgeStore(context.filesDir) }
    var entries by remember { mutableStateOf<List<Pair<PcCartridgeStore.Entry, Int>>>(emptyList()) }
    var selected by remember { mutableStateOf<PcCartridgeStore.Entry?>(null) }
    var busy by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(revision, refresh) {
        try {
            entries =
                withContext(Dispatchers.IO) {
                    store.entries().map { it to PcGuestDiskStore(store.workspace(it)).disks().size }
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
    LemuroidCardSettingsGroup(title = { Text("PC Windows setup") }) {
        Text("1. Open a PC cartridge. Boot and install Windows from your own installation media.")
        Text("2. Run the installed operating system, install the game and shut Windows down normally.")
        Text("3. In Windows, create a Startup shortcut to the installed EXE with the game folder as Start in.")
        Text("For new imports with an EXE path, copy the CARTPLAY shortcut from the content drive to Startup.")
        Text("4. Set the installed OS as auto-start in the core menu. The shelf keeps opening the same guest.")
        Text("To recover setup, remove the guest startup shortcut and reopen the core menu.")
        LemuroidSettingsMenuLink(
            title = { Text("Refresh guest disk status") },
            enabled = !busy,
            onClick = { refresh++ },
        )
        for ((entry, count) in entries) {
            LemuroidSettingsMenuLink(
                title = { Text(entry.title) },
                subtitle = {
                    Text(
                        if (count == 0) "Import a prepared Windows IMG" else "Guest disk present; boot unverified",
                    )
                },
                enabled = enabled && !busy && count == 0,
                onClick = {
                    selected = entry
                    imagePicker.launch(arrayOf("*/*"))
                },
            )
        }
        if (message.isNotBlank()) Text(message)
    }
}
