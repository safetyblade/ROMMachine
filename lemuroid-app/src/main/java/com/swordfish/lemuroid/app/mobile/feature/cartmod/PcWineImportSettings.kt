package com.swordfish.lemuroid.app.mobile.feature.cartmod

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
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
import com.swordfish.lemuroid.lib.library.pc.PcWineFolderBundle
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
    var folder by remember { mutableStateOf<PcWineFolderTree.Selection?>(null) }
    var folderExe by remember { mutableStateOf<String?>(null) }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) selected = uri
        }

    val folderPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
            if (tree != null && !busy) {
                busy = true
                scope.launch {
                    try {
                        val discovered = withContext(Dispatchers.IO) { PcWineFolderTree.scan(context, tree) }
                        folder = discovered
                        folderExe = discovered.exePaths.singleOrNull()
                        message = "Select the game EXE. All files in the folder will be copied."
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        folder = null
                        folderExe = null
                        message = "Folder cannot be read or contains unsupported file names."
                    } finally {
                        busy = false
                    }
                }
            }
        }

    fun importFolderGame() {
        val source = folder ?: return
        val executable = folderExe ?: return
        if (busy) return
        busy = true
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    check(!GameProcessLock.isHeldByAnotherProcess(context))
                    PcWineFolderBundle.importFolder(
                        context.filesDir,
                        source.title,
                        source.files,
                        executable,
                    ) { coroutineContext.ensureActive() }
                }
                LibraryIndexScheduler.scheduleLibrarySync(context)
                folder = null
                folderExe = null
                onImported()
                message = "Full PC game folder imported. Open the cartridge from Shelf."
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                message = "Folder import failed. Existing cartridges and files are unchanged."
            } finally {
                busy = false
            }
        }
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
        Text(
            "Preferred: select a game folder, choose its EXE and retain DLLs, assets, and subdirectories.",
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled && !busy,
            onClick = { folderPicker.launch(null) },
        ) {
            Text("Select game folder")
        }
        folder?.let { game ->
            Text(game.title + " · " + game.files.size + " files · " + game.exePaths.size + " executables")
            for (exe in game.exePaths.take(24)) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled && !busy,
                    onClick = { folderExe = exe },
                ) {
                    Text((if (folderExe == exe) "Selected: " else "Choose: ") + exe)
                }
            }
            if (game.exePaths.size > 24) {
                Text("Folder has more than 24 EXEs. Use a smaller game folder.")
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled && !busy && folderExe != null,
                onClick = { importFolderGame() },
            ) {
                Text("Create complete PC cartridge")
            }
        }
        Text("Use only trusted, lawful local executables. This candidate is isolated from your normal CARTDECK app.")
        Text("Legacy single-file PE32 import (does not copy supporting files).")
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
