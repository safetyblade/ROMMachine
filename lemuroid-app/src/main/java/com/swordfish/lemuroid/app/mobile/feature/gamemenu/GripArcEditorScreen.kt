package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.mobile.feature.game.GripArcInputCatalog
import com.swordfish.lemuroid.app.mobile.feature.game.GripArcManifest
import com.swordfish.lemuroid.app.mobile.feature.game.GripArcProfile
import com.swordfish.lemuroid.app.mobile.feature.game.GripArcProfileStore
import com.swordfish.lemuroid.app.mobile.feature.game.GripArcSide
import com.swordfish.lemuroid.app.mobile.feature.game.rememberGripArcProfile
import com.swordfish.lemuroid.lib.library.db.entity.Game

@Composable
fun GripArcEditorScreen(
    game: Game,
    embedded: Boolean = false,
) {
    val context = LocalContext.current
    val store = remember(context) { GripArcProfileStore(context) }
    val manifest = remember(context) { GripArcManifest.load(context) }
    val options = remember(game.systemId) { GripArcInputCatalog.forGame(game) }
    val profile by rememberGripArcProfile(game)
    var message by remember(game.cartridgeId) { mutableStateOf<String?>(null) }
    var slot by remember(game.cartridgeId) { mutableStateOf<Int?>(null) }
    var draft by remember(game.cartridgeId) { mutableStateOf(emptyList<Int>()) }
    var showAdvanced by remember { mutableStateOf(false) }
    var resetting by remember { mutableStateOf(false) }

    fun label(keys: List<Int>): String =
        keys.mapNotNull { key -> options.firstOrNull { it.code == key }?.label }
            .joinToString("+").ifEmpty { "Assign" }

    fun save(updated: GripArcProfile) {
        message = if (store.save(game, updated)) "Saved for this cartridge." else "Could not save controls."
    }
    Column(
        modifier =
            Modifier.fillMaxWidth().then(
                if (embedded) Modifier else Modifier.verticalScroll(rememberScrollState()),
            ).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (options.isEmpty()) {
            Text("Programmable inputs are unavailable for this controller.")
            return@Column
        }
        Text("Tap a button to assign an input or a simultaneous combination.")
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
            Box(Modifier.size(222.dp)) {
                for (seat in manifest.slots.take(profile.count)) {
                    val center = if (profile.side == GripArcSide.RIGHT) seat.centerRight else seat.centerLeft
                    val assigned = profile.mappings[seat.n - 1]
                    FilterChip(
                        selected = assigned.isNotEmpty(),
                        onClick = {
                            slot = seat.n - 1
                            draft = assigned
                        },
                        modifier =
                            Modifier.offset((center.x * 1.35f - 34).dp, (center.y * 1.35f - 24).dp)
                                .size(width = 68.dp, height = 48.dp),
                        label = {
                            Text(
                                label(assigned),
                                maxLines = 2,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                    )
                }
            }
        }
        message?.let { Text(it) }
        TextButton(onClick = { resetting = true }) { Text("Restore factory assignments") }
        TextButton(onClick = { showAdvanced = !showAdvanced }) { Text("Programmable control positioning") }
        if (showAdvanced) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (side in GripArcSide.entries) {
                    FilterChip(
                        profile.side == side,
                        { save(profile.copy(side = side)) },
                        { Text(side.name.lowercase()) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (count in 1..4) {
                    FilterChip(profile.count == count, { save(profile.copy(count = count)) }, { Text("$count") })
                }
            }
            Row {
                TextButton(onClick = { save(profile.copy(dx = profile.dx - 12f)) }) { Text("←") }
                TextButton(onClick = { save(profile.copy(dx = profile.dx + 12f)) }) { Text("→") }
                TextButton(onClick = { save(profile.copy(dy = profile.dy - 12f)) }) { Text("↑") }
                TextButton(onClick = { save(profile.copy(dy = profile.dy + 12f)) }) { Text("↓") }
            }
            TextButton(onClick = { save(profile.copy(dx = 0f, dy = 0f)) }) { Text("Reset position") }
        }
    }
    slot?.let { selected ->
        AlertDialog(
            onDismissRequest = { slot = null },
            title = { Text("Assign button") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(label(draft))
                    for (option in options) {
                        FilterChip(
                            selected = option.code in draft,
                            onClick = {
                                draft = if (option.code in draft) draft - option.code else (draft + option.code).take(4)
                            },
                            enabled = draft.size < 4 || option.code in draft,
                            label = { Text(option.label) },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    save(
                        profile.copy(
                            mappings =
                                profile.mappings.mapIndexed { index, keys ->
                                    if (index == selected) draft else keys
                                },
                        ),
                    )
                    slot = null
                }) { Text("Save assignment") }
            },
            dismissButton = { TextButton(onClick = { slot = null }) { Text("Cancel") } },
        )
    }
    if (resetting) {
        AlertDialog(
            onDismissRequest = { resetting = false },
            title = { Text("Restore factory assignments?") },
            text = { Text("This replaces the programmable assignments on this cartridge.") },
            confirmButton = {
                TextButton(onClick = {
                    save(profile.copy(mappings = store.defaultFor(game).mappings))
                    resetting = false
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { resetting = false }) { Text("Cancel") } },
        )
    }
}
