package com.swordfish.lemuroid.app.mobile.feature.cartmod

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.swordfish.lemuroid.app.shared.library.LibraryIndexScheduler
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import com.swordfish.lemuroid.lib.library.pc.PcCartridgeStore
import com.swordfish.lemuroid.lib.library.pc.PcWindowsLaunchRecipe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun PcImportSettings(
    enabled: Boolean,
    onImported: () -> Unit,
    visible: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var game by remember { mutableStateOf<Uri?>(null) }
    var windows by remember { mutableStateOf<Uri?>(null) }
    var executable by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember {
        mutableStateOf("Select your own PC game ISO. Optional: a licensed Windows installer ISO.")
    }

    fun name(uri: Uri): String =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: "PC game.iso"

    val gamePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) game = uri
        }
    val windowsPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) windows = uri
        }
    if (!visible) return
    LemuroidCardSettingsGroup(title = { Text("Set up PC game · DOSBox Pure") }) {
        LemuroidSettingsMenuLink(
            title = { Text("1 · Choose PC game ISO") },
            subtitle = { Text(if (game == null) "No game selected" else "Game selected") },
            enabled = enabled && !busy,
            onClick = { gamePicker.launch(arrayOf("*/*")) },
        )
        LemuroidSettingsMenuLink(
            title = { Text("2 · Windows installation ISO (optional)") },
            subtitle = { Text(if (windows == null) "No OS media selected" else "Windows media selected") },
            enabled = enabled && !busy,
            onClick = { windowsPicker.launch(arrayOf("*/*")) },
        )
        if (windows != null) {
            LemuroidSettingsMenuLink(
                title = { Text("Clear Windows selection") },
                enabled = !busy,
                onClick = { windows = null },
            )
        }
        OutlinedTextField(
            value = executable,
            onValueChange = { executable = it },
            label = { Text("3 · Installed Windows EXE (optional)") },
            supportingText = { Text("Use the guest path, e.g. C:\\MAGIC\\MAGIC.EXE.") },
            enabled = !busy,
            singleLine = true,
        )
        LemuroidSettingsMenuLink(
            title = { Text("4 · Import cartridge") },
            subtitle = {
                Text("Copies selected media read-only. Original files and console cartridges remain unchanged.")
            },
            enabled = enabled && !busy && game != null,
            onClick = {
                val gameUri = game
                val osUri = windows
                val executablePath = executable
                if (gameUri != null) {
                    busy = true
                    message = "Importing PC media…"
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                val checkCancellation = { coroutineContext.ensureActive() }
                                val resolver = context.contentResolver
                                require(osUri != gameUri) { "Select different game and Windows media" }
                                val title = name(gameUri).substringBeforeLast('.')
                                val store = PcCartridgeStore(context.filesDir)
                                val recipe = executablePath.takeIf { it.isNotBlank() }?.let(::PcWindowsLaunchRecipe)
                                checkNotNull(resolver.openInputStream(gameUri)).use { source ->
                                    if (osUri == null) {
                                        store.importIso(
                                            title,
                                            source,
                                            launchRecipe = recipe,
                                            checkCancellation = checkCancellation,
                                        )
                                    } else {
                                        checkNotNull(resolver.openInputStream(osUri)).use { os ->
                                            store.importIso(title, source, os, recipe, checkCancellation)
                                        }
                                    }
                                }
                            }
                            LibraryIndexScheduler.scheduleLibrarySync(context)
                            game = null
                            windows = null
                            executable = ""
                            onImported()
                            message = "PC cartridge imported. Refresh the shelf if it is not yet visible."
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            message =
                                "PC import failed. Check format, space and access. Original media is unchanged."
                        } finally {
                            busy = false
                        }
                    }
                }
            },
        )
        Text(message)
        Text("Import creates a cartridge, not a verified installation. Open guest setup to continue.")
    }
}
