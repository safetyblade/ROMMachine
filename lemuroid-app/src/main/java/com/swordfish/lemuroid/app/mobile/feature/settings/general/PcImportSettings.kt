package com.swordfish.lemuroid.app.mobile.feature.settings.general

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun PcImportSettings(enabled: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var game by remember { mutableStateOf<Uri?>(null) }
    var windows by remember { mutableStateOf<Uri?>(null) }
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
    LemuroidCardSettingsGroup(title = { Text("PC Classics · Experimental") }) {
        LemuroidSettingsMenuLink(
            title = { Text("Choose PC game ISO") },
            subtitle = { Text(if (game == null) "No game selected" else "Game selected") },
            enabled = enabled && !busy,
            onClick = { gamePicker.launch(arrayOf("*/*")) },
        )
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
        LemuroidSettingsMenuLink(
            title = { Text("Import as PC cartridge") },
            subtitle = {
                Text("Copies selected media read-only. Original files and console cartridges remain unchanged.")
            },
            enabled = enabled && !busy && game != null,
            onClick = {
                val gameUri = game
                val osUri = windows
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
                                checkNotNull(resolver.openInputStream(gameUri)).use { source ->
                                    if (osUri == null) {
                                        store.importIso(title, source, checkCancellation = checkCancellation)
                                    } else {
                                        checkNotNull(resolver.openInputStream(osUri)).use { os ->
                                            store.importIso(title, source, os, checkCancellation)
                                        }
                                    }
                                }
                            }
                            LibraryIndexScheduler.scheduleLibrarySync(context)
                            game = null
                            windows = null
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
        Text("Open the cartridge from the shelf. Guest boot and installed Windows games are experimental.")
    }
}
