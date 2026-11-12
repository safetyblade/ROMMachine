package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
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
import com.swordfish.lemuroid.app.mobile.feature.game.GripArcProfile
import com.swordfish.lemuroid.app.mobile.feature.game.GripArcProfileStore
import com.swordfish.lemuroid.app.mobile.feature.game.GripArcSide
import com.swordfish.lemuroid.lib.library.db.entity.Game

/**
 * Game Menu > Controls > GripArc.
 * Store writes are per-game and atomic, and the running ViewModel reloads
 * them on activity resume without restarting or modifying emulator saves.
 */
@Composable
fun GripArcEditorScreen(game: Game) {
    val context = LocalContext.current.applicationContext
    val store = remember(context) { GripArcProfileStore(context) }
    val options = remember(game.systemId) { GripArcInputCatalog.forGame(game) }
    var profile by remember(game.systemId, game.fileUri) { mutableStateOf(store.read(game)) }
    var saveFailed by remember { mutableStateOf(false) }

    fun save(updated: GripArcProfile) {
        val valid = updated.validated(options.map { it.code }.toSet())
        if (store.save(game, valid)) {
            profile = valid
            saveFailed = false
        } else {
            saveFailed = true
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("GripArc · ${game.displayName()}")
        if (saveFailed) Text("Could not save the control profile. Previous settings remain available.")
        if (!profile.mode.hasGrip) {
            Text("Choose Standard + GripArc or PivotArc + GripArc in the cartridge's Hold Controls menu.")
        }
        if (options.isEmpty()) {
            Text("Programmable button identities are not available for this system.")
            return@Column
        }
        HorizontalDivider()
        Text("Attachment side")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GripArcChoice("Left", profile.side == GripArcSide.LEFT) {
                save(profile.copy(side = GripArcSide.LEFT))
            }
            GripArcChoice("Right", profile.side == GripArcSide.RIGHT) {
                save(profile.copy(side = GripArcSide.RIGHT))
            }
        }
        Text("Active buttons")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..4).forEach { count ->
                GripArcChoice("$count", profile.count == count) { save(profile.copy(count = count)) }
            }
        }
        Text("Full L housing and fixed G1–G4 positions remain unchanged for every count.")
        HorizontalDivider()
        val names = listOf("upper side", "lower side", "bottom corner", "bottom inward")
        for (slot in 1..profile.count) {
            val current = profile.mappings.getOrNull(slot - 1).orEmpty()
            Text("G$slot · ${names[slot - 1]}")
            Text(
                if (current.isEmpty()) {
                    "Not assigned"
                } else {
                    "Current: " + options.filter { it.code in current }.joinToString(" + ") { it.label }
                },
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                options.forEach { option ->
                    GripArcChoice(option.label, option.code in current) {
                        val newKeys =
                            if (option.code in current) {
                                current.filterNot { it == option.code }
                            } else {
                                (current + option.code).distinct().take(4)
                            }
                        save(
                            profile.copy(
                                mappings =
                                    profile.mappings.mapIndexed { index, keys ->
                                        if (index == slot - 1) newKeys else keys
                                    },
                            ),
                        )
                    }
                }
            }
        }
        Text("Select multiple inputs to create a simultaneous combination. No timed sequences.")
        HorizontalDivider()
        Text("Position · whole attachment only")
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedButton(onClick = { save(profile.copy(dx = profile.dx - 12f)) }) { Text("←") }
            OutlinedButton(onClick = { save(profile.copy(dx = profile.dx + 12f)) }) { Text("→") }
            OutlinedButton(onClick = { save(profile.copy(dy = profile.dy - 12f)) }) { Text("↑") }
            OutlinedButton(onClick = { save(profile.copy(dy = profile.dy + 12f)) }) { Text("↓") }
        }
        Text("Offset: ${profile.dx.toInt()}dp X, ${profile.dy.toInt()}dp Y")
        TextButton(onClick = { save(profile.copy(dx = 0f, dy = 0f)) }) {
            Text("Reset position")
        }
        TextButton(
            onClick = {
                save(store.defaultFor(game).copy(mode = profile.mode))
            },
        ) { Text("Reset GripArc settings") }
        Text("Changes are saved to this cartridge and take effect when you return to gameplay.")
    }
}

@Composable
private fun GripArcChoice(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(text) })
}
