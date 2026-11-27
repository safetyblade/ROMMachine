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
import com.swordfish.lemuroid.app.mobile.feature.game.GripArcBehavior
import com.swordfish.lemuroid.app.mobile.feature.game.GripArcInput
import com.swordfish.lemuroid.app.mobile.feature.game.GripArcInputCatalog
import com.swordfish.lemuroid.app.mobile.feature.game.GripArcManifest
import com.swordfish.lemuroid.app.mobile.feature.game.GripArcProfile
import com.swordfish.lemuroid.app.mobile.feature.game.GripArcProfileStore
import com.swordfish.lemuroid.app.mobile.feature.game.GripArcSide
import com.swordfish.lemuroid.app.mobile.feature.game.StandaloneGripArcLayout
import com.swordfish.lemuroid.app.mobile.feature.game.rememberGripArcProfile
import com.swordfish.lemuroid.app.mobile.feature.game.supportsStandaloneGripActions
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.touchinput.radial.settings.TouchControllerID

@Composable
fun GripArcEditorScreen(
    game: Game,
    embedded: Boolean = false,
    controller: TouchControllerID? = null,
) {
    val context = LocalContext.current
    val store = remember(context) { GripArcProfileStore(context) }
    val manifest = remember(context) { GripArcManifest.load(context) }
    val options =
        remember(game.systemId, controller) {
            controller?.takeIf(StandaloneGripArcLayout::supports)
                ?.let(StandaloneGripArcLayout::inventoryFor)
                ?.let(StandaloneGripArcLayout::actionLabels)
                ?.map { (code, label) -> GripArcInput(label, code) }
                ?: GripArcInputCatalog.forGame(game)
        }
    val profile by rememberGripArcProfile(game)
    var message by remember(game.cartridgeId) { mutableStateOf<String?>(null) }
    var slot by remember(game.cartridgeId) { mutableStateOf<Int?>(null) }
    var draft by remember(game.cartridgeId) { mutableStateOf(emptyList<Int>()) }
    val typed = supportsStandaloneGripActions(SystemID.entries.firstOrNull { it.dbname == game.systemId })
    val slotLimit = if (game.systemId == SystemID.ATARI2600.dbname) 2 else 4
    val labels = options.associate { it.code to it.label }
    var behavior by remember(game.cartridgeId) { mutableStateOf(GripArcBehavior.MOMENTARY) }
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
                for (seat in manifest.slots.take(minOf(profile.count, slotLimit))) {
                    val center = if (profile.side == GripArcSide.RIGHT) seat.centerRight else seat.centerLeft
                    val assigned = profile.mappings[seat.n - 1]
                    FilterChip(
                        selected = assigned.isNotEmpty(),
                        onClick = {
                            slot = seat.n - 1
                            draft = assigned
                            behavior = profile.behaviors.getOrElse(seat.n - 1) { GripArcBehavior.MOMENTARY }
                        },
                        modifier =
                            Modifier.offset((center.x * 1.35f - 34).dp, (center.y * 1.35f - 24).dp)
                                .size(width = 68.dp, height = 48.dp),
                        label = {
                            Text(
                                if (typed) profile.labelFor(seat.n, labels).ifEmpty { "Assign" } else label(assigned),
                                maxLines = 2,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                    )
                }
            }
        }
        if (slotLimit < 4) Text("Only two seats are active here. Assignments for other seats are retained.")
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
                for (count in 1..slotLimit) {
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
            if (typed) {
                Text("Turbo rate")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (rate in listOf(6, 10, 14)) {
                        FilterChip(
                            profile.turboRate == rate,
                            { save(profile.copy(turboRate = rate)) },
                            { Text("$rate Hz") },
                        )
                    }
                }
            }
        }
    }
    slot?.let { selected ->
        AlertDialog(
            onDismissRequest = { slot = null },
            title = { Text("Assign button") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(label(draft))
                    if (typed) {
                        for ((option, name) in listOf(
                            GripArcBehavior.MOMENTARY to "Native input or chord",
                            GripArcBehavior.HOLD to "Hold",
                            GripArcBehavior.TURBO to "Turbo",
                            GripArcBehavior.MENU to "Open game menu",
                        )) {
                            FilterChip(behavior == option, {
                                behavior = option
                                if (option == GripArcBehavior.MENU) {
                                    draft = emptyList()
                                } else if (option != GripArcBehavior.MOMENTARY) {
                                    draft = draft.take(1)
                                }
                            }, { Text(name) })
                        }
                    }
                    for (option in options) {
                        FilterChip(
                            selected = option.code in draft,
                            onClick = {
                                draft =
                                    when {
                                        option.code in draft -> draft - option.code
                                        typed && behavior != GripArcBehavior.MOMENTARY -> listOf(option.code)
                                        else -> (draft + option.code).take(4)
                                    }
                            },
                            enabled = behavior != GripArcBehavior.MENU && (draft.size < 4 || option.code in draft),
                            label = { Text(option.label) },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    save(
                        profile.copy(
                            behaviors =
                                profile.behaviors.mapIndexed { index, current ->
                                    if (typed && index == selected) behavior else current
                                },
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
                    val defaults = store.defaultFor(game)
                    save(
                        profile.copy(
                            mappings = defaults.mappings,
                            behaviors = defaults.behaviors,
                            turboRate = defaults.turboRate,
                        ),
                    )
                    resetting = false
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { resetting = false }) { Text("Cancel") } },
        )
    }
}
