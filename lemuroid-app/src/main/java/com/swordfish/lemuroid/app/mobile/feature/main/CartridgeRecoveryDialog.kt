package com.swordfish.lemuroid.app.mobile.feature.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import com.swordfish.lemuroid.app.shared.library.CartridgeRecoveryManager
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.saves.CartridgeSaveBinding
import kotlinx.coroutines.launch

/**
 * An explicit recovery choice, never a matching heuristic or a background
 * assignment. The app's existing Add Cartridges flow indexes replacement
 * sources before they appear here.
 */
@Composable
fun CartridgeRecoveryDialog(
    database: RetrogradeDatabase,
    binding: CartridgeSaveBinding,
    onDismiss: () -> Unit,
    onUpdated: () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val manager = remember(context, database) { CartridgeRecoveryManager(context, database) }
    val scope = rememberCoroutineScope()
    var inventory by remember { mutableStateOf<CartridgeRecoveryManager.Inventory?>(null) }
    var legacyMode by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var retiredId by remember { mutableStateOf<String?>(null) }
    var selectedGameId by remember { mutableStateOf<Int?>(null) }
    var legacy by remember { mutableStateOf<CartridgeSaveBinding.LegacySramCandidate?>(null) }
    var acknowledged by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(manager) { inventory = manager.inventory() }
    val retired = inventory?.retired.orEmpty()
    val active = inventory?.available.orEmpty()
    val chosenRetired = retired.firstOrNull { it.cartridgeId == retiredId }
    val chosenGame = active.firstOrNull { it.id == selectedGameId }
    LaunchedEffect(legacyMode, chosenGame?.cartridgeId, chosenGame?.fileUri) {
        legacy = if (legacyMode && chosenGame != null) binding.pendingSharedSram(chosenGame) else null
        acknowledged = false
    }

    val eligible =
        active.filter { game ->
            if (legacyMode) {
                true
            } else {
                chosenRetired != null && game.systemId == chosenRetired.systemId &&
                    game.lastPlayedAt == null && !game.isFavorite &&
                    game.customName == null && game.customCoverUri == null
            }
        }.filter {
            query.isBlank() || it.displayName().contains(query, ignoreCase = true) ||
                it.fileName.contains(query, ignoreCase = true)
        }

    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        title = { Text("Cartridge recovery") },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row {
                    TextButton(
                        onClick = {
                            legacyMode = false
                            selectedGameId = null
                            query = ""
                            acknowledged = false
                        },
                    ) { Text(if (legacyMode) "Sources" else "Sources ✓") }
                    TextButton(
                        onClick = {
                            legacyMode = true
                            selectedGameId = null
                            query = ""
                            acknowledged = false
                        },
                    ) { Text(if (legacyMode) "Legacy saves ✓" else "Legacy saves") }
                }
                if (!legacyMode) {
                    Text(
                        "Choose an unavailable cartridge and an already scanned ROM. " +
                            "Only you can confirm the connection; same names do not prove identity.",
                    )
                    if (retired.isEmpty()) Text("No unavailable cartridges recorded.")
                    retired.take(45).forEach { old ->
                        TextButton(
                            enabled = !working,
                            onClick = {
                                retiredId = old.cartridgeId
                                selectedGameId = null
                                acknowledged = false
                            },
                        ) {
                            Text(
                                (if (retiredId == old.cartridgeId) "✓ " else "") +
                                    "${old.displayName()} · ${old.systemId}",
                            )
                        }
                    }
                    if (retired.size > 45) Text("Showing the first 45 retired cartridges.")
                } else {
                    Text(
                        "Select a cartridge, review the exact historical shared SRAM and fingerprint, " +
                            "then confirm. Originals remain untouched. Engine-owned saves and states " +
                            "are not included in this checkpoint.",
                    )
                }
                if (legacyMode || chosenRetired != null) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it; selectedGameId = null; acknowledged = false },
                        label = { Text("Find indexed cartridge") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    eligible.take(45).forEach { game ->
                        TextButton(
                            enabled = !working,
                            onClick = { selectedGameId = game.id; acknowledged = false },
                        ) {
                            Text(
                                (if (selectedGameId == game.id) "✓ " else "") +
                                    "${game.displayName()} · ${game.systemId} · ${game.fileName}",
                            )
                        }
                    }
                    if (eligible.isEmpty()) Text("No eligible indexed cartridges.")
                    if (eligible.size > 45) Text("Refine the search to see additional cartridges.")
                }
                if (legacyMode && chosenGame != null) {
                    if (legacy == null) {
                        Text("No matching historical shared SRAM is available for this cartridge.")
                    } else {
                        Text("Legacy file: ${legacy!!.fileName}")
                        Text("Size: ${legacy!!.bytes} bytes")
                        Text("SHA-256: ${legacy!!.sha256.take(20)}…")
                    }
                }
                if (chosenGame != null && (!legacyMode || legacy != null)) {
                    Text(
                        if (legacyMode) {
                            "Confirm only if this progress belongs to the selected cartridge. " +
                                "Different existing cartridge progress will not be overwritten."
                        } else {
                            "Confirm only if this ROM is the previous cartridge. " +
                                "The retired identity, shelf position and saved preferences will be restored. " +
                                "The selected scanned source must have no existing player progress."
                        },
                    )
                    TextButton(
                        enabled = !working,
                        onClick = { acknowledged = !acknowledged },
                    ) { Text(if (acknowledged) "✓ Ownership confirmed" else "I confirm this association") }
                }
                error?.let { Text(it) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !working && acknowledged && chosenGame != null &&
                    (if (legacyMode) legacy != null else chosenRetired != null),
                onClick = {
                    val target = chosenGame ?: return@TextButton
                    working = true
                    error = null
                    scope.launch {
                        val result =
                            runCatching {
                                if (legacyMode) {
                                    binding.confirmSharedSramRecovery(target, requireNotNull(legacy).sha256)
                                } else {
                                    manager.restore(requireNotNull(chosenRetired).cartridgeId, target.id)
                                }
                            }
                        working = false
                        if (result.isSuccess) {
                            onUpdated()
                            onDismiss()
                        } else {
                            error = result.exceptionOrNull()?.message ?: "Recovery was not completed. Data is retained."
                            acknowledged = false
                            inventory = manager.inventory()
                        }
                    }
                },
            ) { Text(if (working) "Verifying…" else "Confirm recovery") }
        },
        dismissButton = {
            TextButton(enabled = !working, onClick = onDismiss) { Text("Keep separate / Close") }
        },
    )
}
