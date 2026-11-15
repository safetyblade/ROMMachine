package com.swordfish.lemuroid.app.mobile.feature.settings.ps3

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.swordfish.lemuroid.app.shared.game.ps3.Ps3Engine
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import com.swordfish.lemuroid.lib.storage.ps3.Ps3Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Used by Settings, cartridge details and the common game menu. All mutations use real engine storage. */
@Composable
fun Ps3Management(initialEngine: String = "armsx3") {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var engine by remember { mutableStateOf(initialEngine) }
    val storage = remember(engine) { Ps3Storage(context.filesDir, engine) }
    var revision by remember { mutableStateOf(0) }
    var profiles by remember { mutableStateOf(emptyList<Ps3Storage.Profile>()) }
    var saves by remember { mutableStateOf(emptyList<Ps3Storage.Save>()) }
    var firmware by remember { mutableStateOf(false) }
    var usage by remember { mutableStateOf(0L) }
    var readiness by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var rename by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var exportName by remember { mutableStateOf<String?>(null) }
    var restoreName by remember { mutableStateOf<String?>(null) }
    val active = profiles.firstOrNull { it.selected }?.id

    fun mutate(action: () -> Unit) {
        scope.launch {
            busy = true
            runCatching {
                withContext(Dispatchers.IO) {
                    check(
                        !GameProcessLock.isHeldByAnotherProcess(context),
                    ) { "Exit the game before changing PS3 profiles or saves." }
                    action()
                }
            }.onFailure { message = it.message ?: "Operation failed; original data was retained." }
            busy = false
            revision++
        }
    }
    val export =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            val name = exportName
            val user = active
            if (uri != null && name != null && user != null) {
                mutate {
                    context.contentResolver.openOutputStream(uri)?.let {
                        storage.export(user, name, it)
                    } ?: error("Export destination cannot be written")
                }
            }
        }
    val restore =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val name = restoreName
            val user = active
            if (uri != null && name != null && user != null) {
                mutate {
                    context.contentResolver.openInputStream(uri)?.let {
                        storage.restore(user, name, it, true)
                    } ?: error("Save backup cannot be opened")
                }
            }
        }
    LaunchedEffect(engine, revision) {
        withContext(Dispatchers.IO) {
            profiles = storage.profiles()
            saves = profiles.firstOrNull { it.selected }?.let { storage.saves(it.id) }.orEmpty()
            firmware = storage.firmwareInstalled()
            usage = storage.usage()
            readiness = Ps3Engine.readiness(context, engine)
        }
    }
    LemuroidCardSettingsGroup(title = { Text("PlayStation 3 setup") }) {
        Text(
            "$engine · Firmware ${if (firmware) "installed" else "missing"} · ${usage / 1048576} MiB",
            Modifier.padding(16.dp),
        )
        Text(readiness ?: "Native engine verified. Device gameplay remains unverified.", Modifier.padding(16.dp))
        LemuroidSettingsMenuLink(title = {
            Text("Storage: $engine")
        }, subtitle = {
            Text("Switch view; each engine retains independent firmware, users and saves.")
        }, onClick = { engine = if (engine == "armsx3") "rpcs3-libretro" else "armsx3" })
        LemuroidSettingsMenuLink(title = {
            Text("Import / replace PS3 firmware")
        }, subtitle = {
            Text("Select your legally supplied firmware in the integrated engine setup.")
        }, enabled = readiness == null && !busy, onClick = {
            context.startActivity(
                Intent().setClassName(
                    context,
                    Ps3Engine.ACTIVITY,
                ).putExtra("cartdeck.engine", engine).putExtra("cartdeck.setup", true),
            )
        })
        Text("PS3 virtual users", Modifier.padding(16.dp))
        profiles.forEach { profile ->
            LemuroidSettingsMenuLink(title = {
                Text("${profile.name}${if (profile.selected) " (selected)" else ""}")
            }, subtitle = {
                Text(
                    "User ${profile.id}",
                )
            }, enabled = !busy, onClick = { mutate { storage.select(profile.id) } })
            TextButton(onClick = {
                rename = profile.id
                text = profile.name
                editing = true
            }, enabled = !busy) { Text("Rename ${profile.name}") }
            if (profiles.size > 1) {
                TextButton(onClick = {
                    deleting = profile.id to true
                }, enabled = !busy) { Text("Remove ${profile.name}") }
            }
        }
        Button(onClick = {
            rename = null
            text = ""
            editing = true
        }, enabled = !busy) { Text("Create PS3 user") }
        Text("Save Management", Modifier.padding(16.dp))
        if (saves.isEmpty()) Text("No saves found for the selected engine and user.", Modifier.padding(16.dp))
        saves.forEach { save ->
            Column(Modifier.padding(16.dp)) {
                Text("${save.title} · ${save.bytes} bytes")
                Text(java.text.DateFormat.getDateTimeInstance().format(java.util.Date(save.modified)))
                TextButton(onClick = {
                    exportName = save.name
                    export.launch("${save.name}.zip")
                }, enabled = !busy) { Text("Backup / Export") }
                TextButton(onClick = { deleting = save.name to false }, enabled = !busy) { Text("Delete save") }
                TextButton(onClick = { restoreName = save.name }, enabled = !busy) { Text("Restore / Import") }
            }
        }
        Text(
            "PS3 in-game saves are not emulator save states. Imports require complete PARAM.SFO / PARAM.PFD data; account-bound saves may need their original user. Engine switches never copy saves automatically.",
            Modifier.padding(16.dp),
        )
        if (message.isNotBlank()) Text(message, Modifier.padding(16.dp))
    }
    if (editing) {
        AlertDialog(onDismissRequest = {
            editing = false
        }, title = {
            Text(if (rename == null) "Create PS3 user" else "Rename PS3 user")
        }, text = {
            OutlinedTextField(value = text, onValueChange = {
                text = it.take(64)
            }, label = { Text("Display name") })
        }, confirmButton = {
            TextButton(onClick = {
                val name = text
                val id = rename
                editing = false
                mutate { if (id == null) storage.create(name) else storage.rename(id, name) }
            }, enabled = text.isNotBlank()) { Text("Save") }
        }, dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel") } })
    }
    deleting?.let { (name, profile) ->
        AlertDialog(onDismissRequest = {
            deleting = null
        }, title = {
            Text(if (profile) "Remove PS3 user?" else "Delete PS3 save?")
        }, text = {
            Text("The original data will be retained in an automatic backup. This affects only $engine.")
        }, confirmButton = {
            TextButton(onClick = {
                deleting = null
                mutate {
                    if (profile) {
                        storage.removeProfile(
                            name,
                            true,
                        )
                    } else {
                        storage.deleteSave(requireNotNull(active), name, true)
                    }
                }
            }) { Text("Confirm") }
        }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } })
    }
    restoreName?.let {
            name ->
        AlertDialog(onDismissRequest = {
            restoreName = null
        }, title = {
            Text("Restore $name?")
        }, text = {
            Text(
                "Your current save is backed up before replacement. Import only a complete save for the selected PS3 user.",
            )
        }, confirmButton = {
            TextButton(onClick = {
                restore.launch(arrayOf("application/zip", "application/octet-stream"))
            }) { Text("Choose backup") }
        }, dismissButton = { TextButton(onClick = { restoreName = null }) { Text("Cancel") } })
    }
}
