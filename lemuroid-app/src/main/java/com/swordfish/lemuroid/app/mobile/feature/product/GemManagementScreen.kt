package com.swordfish.lemuroid.app.mobile.feature.product

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
import androidx.compose.material3.Checkbox
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
import com.swordfish.lemuroid.app.gemrating.GemCatalogueVersions
import com.swordfish.lemuroid.app.gemrating.GemProfileBackup
import com.swordfish.lemuroid.app.gemrating.GemRatingLocalStore
import com.swordfish.lemuroid.app.gemrating.GemSafeSaveComponent
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.CartdeckMenuRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun GemManagementScreen(
    onCatalogueChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val versions = remember(context) { GemCatalogueVersions(context) }
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<GemCatalogueVersions.Status?>(null) }
    var candidate by remember { mutableStateOf<GemCatalogueVersions.Candidate?>(null) }
    var backup by remember { mutableStateOf<GemProfileBackup.Preview?>(null) }
    var rollback by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var replace by remember { mutableStateOf(false) }
    var preferences by remember { mutableStateOf(false) }
    var copy by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }

    fun <T> operation(
        failure: String,
        work: () -> T,
        success: (T) -> Unit,
    ) {
        if (busy) return
        busy = true
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching(work) }
            result.onSuccess(success).onFailure { message = failure }
            busy = false
        }
    }

    LaunchedEffect(refresh) {
        status = withContext(Dispatchers.IO) { runCatching { versions.status() }.getOrNull() }
    }

    val updatePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                operation(
                    "Update rejected or unreadable. Existing catalogue retained.",
                    { requireNotNull(context.contentResolver.openInputStream(uri)).use(versions::preview) },
                    { candidate = it },
                )
            }
        }

    val exportPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            if (uri != null) {
                operation(
                    "GEM export failed. Personal data retained.",
                    {
                        val store = GemRatingLocalStore.open(context)
                        val bytes = GemSafeSaveComponent.capture(store.repository, store.session).bytes
                        requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).use { it.write(bytes) }
                        runCatching {
                            requireNotNull(context.contentResolver.openInputStream(uri)).use {
                                GemProfileBackup.read(it).contentEquals(bytes)
                            }
                        }.getOrDefault(false)
                    },
                    {
                        message =
                            if (it) {
                                "GEM backup exported and verified."
                            } else {
                                "Written to provider; readback verification unavailable or failed."
                            }
                    },
                )
            }
        }

    val importPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                operation(
                    "Invalid or unreadable GEM backup. Existing data retained.",
                    {
                        val source =
                            requireNotNull(context.contentResolver.openInputStream(uri)).use {
                                GemProfileBackup.decode(GemProfileBackup.read(it))
                            }
                        val store = GemRatingLocalStore.open(context)
                        store.repository.previewRestore(store.session, source)
                    },
                    {
                        replace = false
                        preferences = false
                        copy = false
                        backup = it
                    },
                )
            }
        }

    val current = status
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("GEM management", style = MaterialTheme.typography.titleLarge)
        if (message.isNotBlank()) Text(message)
        if (busy) Text("Operation in progress…")
        Text("Catalogue", style = MaterialTheme.typography.titleMedium)
        Text("Active: ${current?.active ?: "Unknown"}")
        Text("Loaded: ${current?.loaded ?: "Checking"}")
        Text("Previous: ${current?.previous ?: "None"}")
        current?.warning?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        CartdeckMenuRow(
            title = { Text("Import approved catalogue update") },
            supporting = {
                Text(if (current?.signingConfigured == true) "Signed local package" else "Signing not configured")
            },
            enabled = !busy && current?.signingConfigured == true,
            onClick = { updatePicker.launch(arrayOf("application/octet-stream", "*/*")) },
        )
        CartdeckMenuRow(
            title = { Text("Check online for catalogue update") },
            supporting = {
                Text(if (current?.onlineConfigured == true) "Manual check only" else "Update source not configured")
            },
            enabled = !busy && current?.onlineConfigured == true,
            onClick = {
                operation("No verified update available. Existing catalogue retained.", versions::fetchPreview) {
                    candidate = it
                }
            },
        )
        CartdeckMenuRow(
            title = { Text("Roll back to previous catalogue") },
            supporting = { Text(current?.previous ?: "No previous version") },
            enabled = !busy && current?.previous != null,
            onClick = { rollback = true },
        )
        Text("Personal GEM backup", style = MaterialTheme.typography.titleMedium)
        Text("Ratings, cleared ratings, Wildcards, Surprise Me history and shelf-rating preference.")
        Text("A GEM backup is not a game-save archive. Local identification links stay on this installation.")
        CartdeckMenuRow(
            title = { Text("Export GEM profile") },
            enabled = !busy,
            onClick = { exportPicker.launch("CARTDECK-profile.gembackup") },
        )
        CartdeckMenuRow(
            title = { Text("Import GEM profile") },
            enabled = !busy,
            onClick = { importPicker.launch(arrayOf("application/octet-stream", "application/json", "*/*")) },
        )
        Text("SafeSave: GEM metadata component ready; full game-save archive feature not yet connected.")
    }

    val update = candidate
    if (update != null) {
        AlertDialog(
            onDismissRequest = { candidate = null },
            title = { Text("Activate catalogue update") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(update.verified.catalogue.manifest.catalogueVersion)
                    Text("${update.added} added · ${update.retired} retired entries")
                    Text("Previous version retained. Ratings, corrections, cartridge identities and saves stay unchanged.")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        candidate = null
                        operation("Update activation failed. Previous data retained.", { versions.activate(update) }) {
                            message = "Active catalogue: $it"
                            refresh++
                            onCatalogueChanged()
                        }
                    },
                ) { Text("Activate") }
            },
            dismissButton = { TextButton(onClick = { candidate = null }) { Text("Cancel") } },
        )
    }

    if (rollback) {
        AlertDialog(
            onDismissRequest = { rollback = false },
            title = { Text("Restore previous catalogue?") },
            text = { Text("Only catalogue metadata changes. Your personal ratings, links and game saves stay intact.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        rollback = false
                        operation("Rollback failed. Existing data retained.", versions::rollback) {
                            message = "Active catalogue: $it"
                            refresh++
                            onCatalogueChanged()
                        }
                    },
                ) { Text("Roll back") }
            },
            dismissButton = { TextButton(onClick = { rollback = false }) { Text("Cancel") } },
        )
    }

    val preview = backup
    if (preview != null) {
        val different = preview.local.profileId != preview.source.profileId
        AlertDialog(
            onDismissRequest = { backup = null },
            title = { Text("Review GEM restore") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("${preview.additions} new · ${preview.conflicts} conflicts · ${preview.unchanged} unchanged")
                    Text("Ratings map by exact cartridge UUID only. Unmatched records remain in your rating history.")
                    Text("Keep existing ratings by default, including previously cleared ratings.")
                    if (different) {
                        Checkbox(checked = copy, onCheckedChange = { copy = it })
                        Text("Copy this backup into my current player (different source player).")
                    }
                    Checkbox(checked = replace, onCheckedChange = { replace = it })
                    Text("Replace conflicting ratings, including cleared ratings.")
                    Checkbox(checked = preferences, onCheckedChange = { preferences = it })
                    Text("Restore discovery and shelf preferences.")
                    Text("A local recovery copy is written before any replacement. Game saves are not touched.")
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !different || copy,
                    onClick = {
                        backup = null
                        val replaceChoice = replace
                        val preferenceChoice = preferences
                        val copyChoice = copy
                        operation(
                            "Restore rejected or changed since preview. Reopen the backup.",
                            {
                                val store = GemRatingLocalStore.open(context)
                                store.repository.restore(
                                    store.session,
                                    preview,
                                    replaceChoice,
                                    preferenceChoice,
                                    copyChoice,
                                )
                            },
                            { message = "GEM profile restored. Existing game saves unchanged." },
                        )
                    },
                ) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { backup = null }) { Text("Cancel") } },
        )
    }
}
