package com.swordfish.lemuroid.app.mobile.feature.cartmod

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.swordfish.lemuroid.app.shared.library.LibraryIndexScheduler
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
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var game by remember { mutableStateOf<Uri?>(null) }
    var windows by remember { mutableStateOf<Uri?>(null) }
    var executable by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var options by remember { mutableStateOf(false) }
    var message by remember {
        mutableStateOf("")
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
    WorkshopPanel("01 · Set up a PC game", "DOSBox Pure · choose your own game media") {
        LemuroidSettingsMenuLink(
            title = { Text("Select game ISO") },
            subtitle = { Text(if (game == null) "No game selected" else "Game selected") },
            enabled = enabled && !busy,
            onClick = { gamePicker.launch(arrayOf("*/*")) },
        )
        TextButton(onClick = { options = !options }, enabled = !busy) {
            Text(if (options) "Hide installation options" else "Windows media & EXE · optional")
        }
        if (options) {
            LemuroidSettingsMenuLink(
                title = { Text("Choose Windows installation ISO (optional)") },
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
                label = { Text("Installed Windows EXE (optional)") },
                supportingText = { Text("Use the guest path, e.g. C:\\MAGIC\\MAGIC.EXE.") },
                enabled = !busy,
                singleLine = true,
            )
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
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
        ) {
            Text(if (busy) "Importing…" else "Create PC cartridge")
        }
        Text("Original media is unchanged. Installation is verified separately.")
        if (message.isNotBlank()) Text(message)
    }
}
