package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.swordfish.lemuroid.app.shared.game.EmulatorModernAssetsProvisioner
import com.swordfish.lemuroid.app.shared.game.engine.EngineSelectionPreferences
import com.swordfish.lemuroid.app.shared.game.engine.NativeEngineRegistry
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.db.entity.Game

/** The same choices govern console defaults and cartridge overrides. Saves are never moved here. */
@Composable
fun EngineSelectionControls(
    system: GameSystem,
    game: Game? = null,
    assetsReady: Boolean = false,
    showFallbackPolicy: Boolean = true,
) {
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { EngineSelectionPreferences(context) }
    var revision by remember(system.id, game?.fileUri) { mutableIntStateOf(0) }
    LaunchedEffect(system.id) {
        EmulatorModernAssetsProvisioner.installIfBundled(context)
        revision++
    }
    val selected = remember(system.id, game?.fileUri, revision) { preferences.preferred(system.id, game) }
    val engines = remember(system.id, assetsReady) { NativeEngineRegistry.engines(system) }
    val eligible = engines.filter { NativeEngineRegistry.eligibility(context, it).eligible }
    if (eligible.size <= 1) {
        Column {
            engines.forEach { engine ->
                val status = NativeEngineRegistry.eligibility(context, engine)
                Text(engine.name, style = MaterialTheme.typography.titleMedium)
                Text(if (status.eligible) "Installed engine" else status.reason.orEmpty())
            }
            if (engines.size > 1 && eligible.size == 1) {
                TextButton(onClick = {
                    preferences.setPreferred(system, eligible.single().id, game)
                    revision++
                }) {
                    Text("Use available engine")
                }
            }
            val stage = game?.let { preferences.failureStage(it) }
            if (stage != null) {
                Text("Last launch: $stage")
                eligible.singleOrNull()?.let { engine ->
                    TextButton(onClick = {
                        preferences.setPreferred(system, engine.id, game)
                        revision++
                    }) {
                        Text("Retry installed engine")
                    }
                }
            } else if (selected != null && selected != EngineSelectionPreferences.AUTOMATIC &&
                eligible.none { it.id == selected } && eligible.isNotEmpty()
            ) {
                TextButton(onClick = {
                    preferences.setPreferred(system, null, game)
                    revision++
                }) {
                    Text("Reset unavailable preference")
                }
            }
        }
        return
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            if (selected == null || selected == EngineSelectionPreferences.AUTOMATIC) {
                "Automatic engine selection"
            } else {
                "Preferred engine: ${engines.firstOrNull { it.id == selected }?.name ?: "unavailable"}"
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Automatic keeps a successful engine. Changes use separate save states; existing game saves stay in place.",
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton(onClick = {
            preferences.setPreferred(system, null, game)
            revision++
        }) {
            Text("Automatic / reset to recommended")
        }
        engines.forEach { engine ->
            val eligibility = NativeEngineRegistry.eligibility(context, engine)
            OutlinedButton(
                enabled = eligibility.eligible,
                onClick = {
                    preferences.setPreferred(system, engine.id, game)
                    revision++
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (engine.id == selected) "${engine.name} · selected" else "Use ${engine.name}") }
            if (!eligibility.eligible) Text(eligibility.reason.orEmpty(), style = MaterialTheme.typography.bodySmall)
        }
        if (game != null) {
            TextButton(onClick = {
                preferences.inheritConsole(game)
                revision++
            }) { Text("Use console preference") }
            val previous = remember(system.id, game?.fileUri, revision) { preferences.previous(game) }
            engines.firstOrNull { it.id == previous }?.let { engine ->
                TextButton(
                    onClick = {
                        preferences.setPreferred(system, engine.id, game)
                        revision++
                    },
                ) { Text("Previous successful engine: ${engine.name}") }
            }
            val stage = remember(system.id, game?.fileUri, revision) { preferences.failureStage(game) }
            if (stage != null) {
                Text(
                    "Last launch stopped at $stage. Select an engine above to retry.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (showFallbackPolicy) EngineFallbackControls(system)
    }
}

@Composable
fun EngineFallbackControls(system: GameSystem) {
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { EngineSelectionPreferences(context) }
    var revision by remember(system.id) { mutableIntStateOf(0) }
    if (NativeEngineRegistry.engines(system).count { NativeEngineRegistry.eligibility(context, it).eligible } < 2) {
        Text("No alternative engine is currently eligible. Retained versions are managed separately.")
        return
    }
    Column {
        val policy = remember(system.id, revision) { preferences.fallbackPolicy(system.id) }
        Text("Fallback policy for this console", style = MaterialTheme.typography.bodyMedium)
        EngineSelectionPreferences.FallbackPolicy.values().forEach { option ->
            TextButton(onClick = {
                preferences.setFallbackPolicy(system.id, option)
                revision++
            }) {
                val label =
                    when (option) {
                        EngineSelectionPreferences.FallbackPolicy.ASK -> "Ask me after failure"
                        EngineSelectionPreferences.FallbackPolicy.NEXT_MANUAL_LAUNCH ->
                            "Use a proven alternative on next Open"
                        EngineSelectionPreferences.FallbackPolicy.NEVER -> "Keep my engine; never switch automatically"
                    }
                Text(if (option == policy) "$label · selected" else label)
            }
        }
    }
}
