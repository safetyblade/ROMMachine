package com.swordfish.lemuroid.app.mobile.feature.cartmod

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.swordfish.lemuroid.app.shared.game.GameProcessLock
import com.swordfish.lemuroid.app.shared.library.LibraryIndexScheduler
import com.swordfish.lemuroid.lib.library.pc.PcWineCartridges
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun PcWineImportSettings(
    enabled: Boolean,
    onImported: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) selected = uri
        }

    fun importGame(uri: Uri?) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    check(!GameProcessLock.isHeldByAnotherProcess(context))
                    val cancellation = { coroutineContext.ensureActive() }
                    if (uri == null) {
                        context.assets.open("cartdeck-wine/probe32.exe").use {
                            PcWineCartridges.importExecutable(context.filesDir, "Wine runtime check", it, cancellation)
                        }
                    } else {
                        val name =
                            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                                ?.use { if (it.moveToFirst()) it.getString(0) else null } ?: "Windows game.exe"
                        checkNotNull(context.contentResolver.openInputStream(uri)).use {
                            PcWineCartridges.importExecutable(
                                context.filesDir,
                                name.substringBeforeLast('.'),
                                it,
                                cancellation,
                            )
                        }
                    }
                }
                LibraryIndexScheduler.scheduleLibrarySync(context)
                selected = null
                onImported()
                message = "Cartridge created. Open it from Shelf after indexing."
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                message = "Could not import PE32 executable. Existing cartridges and files are unchanged."
            } finally {
                busy = false
            }
        }
    }

    WorkshopPanel("Windows game · Wine", "Experimental native compatibility, no Windows ISO") {
        Text("Use only trusted, lawful local executables. This candidate is isolated from your normal CARTDECK app.")
        Text(
            "Single PE32 file only. Multi-file installations, 16-bit installers and gameplay are not yet verified.",
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled && !busy,
            onClick = { picker.launch(arrayOf("*/*")) },
        ) {
            Text(if (selected == null) "Select Windows EXE" else "Change selected EXE")
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled && !busy && selected != null,
            onClick = { importGame(selected) },
        ) {
            Text("Create Wine cartridge")
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled && !busy,
            onClick = { importGame(null) },
        ) {
            Text("Create runtime verification cartridge")
        }
        if (message.isNotBlank()) Text(message)
    }
}
