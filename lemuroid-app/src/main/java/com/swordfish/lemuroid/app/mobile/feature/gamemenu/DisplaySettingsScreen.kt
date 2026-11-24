package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.shared.game.DisplayEngineOptions
import com.swordfish.lemuroid.app.shared.game.DisplaySettingsStore
import com.swordfish.lemuroid.app.shared.game.ImageQuality
import com.swordfish.lemuroid.app.shared.game.SmartVisual
import com.swordfish.lemuroid.app.shared.game.engine.EngineSelectionPreferences
import com.swordfish.lemuroid.app.shared.game.engine.NativeEngineRegistry
import com.swordfish.lemuroid.lib.core.CoreVariablesManager
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun rememberDisplaySettings(cartridge: String?): DisplaySettingsStore.Snapshot {
    val context = LocalContext.current.applicationContext
    val store = remember(context) { DisplaySettingsStore(context) }
    return remember(store, cartridge) { store.observe(cartridge?.takeIf { it.isNotBlank() }) }
        .collectAsState(
            DisplaySettingsStore.Snapshot(DisplaySettingsStore.Choices(), DisplaySettingsStore.Choices()),
        ).value
}

/** Same screen and persistence from Settings, cartridge Setup and the active host menu. */
@Composable
fun DisplaySettingsScreen(
    game: Game? = null,
    libretroHost: Boolean? = null,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    coreConfig: SystemCoreConfig? = null,
) {
    val context = LocalContext.current.applicationContext
    val store = remember(context) { DisplaySettingsStore(context) }
    val cartridge = game?.cartridgeId?.takeIf { it.isNotBlank() }
    val snapshot = rememberDisplaySettings(cartridge)
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var helpOpen by remember { mutableStateOf(false) }
    var advancedOpen by remember { mutableStateOf(false) }
    val selectedEngine =
        remember(game, libretroHost, coreConfig) {
            game?.let { runCatching { EngineSelectionPreferences(context).select(it, null).engine }.getOrNull() }
        }
    val libretro = libretroHost ?: (game == null || selectedEngine?.host == NativeEngineRegistry.Host.LIBRETRO)
    val selectedConfig = coreConfig ?: selectedEngine?.config
    val namespace =
        game?.let {
                current ->
            selectedConfig?.let { DisplayEngineOptions.namespace(current.systemId, it) }
        }
    val engineOptions =
        produceState(emptyList<DisplayEngineOptions.Option>(), namespace) {
            value = withContext(Dispatchers.IO) { namespace?.let { DisplayEngineOptions(context).read(it) }.orEmpty() }
        }.value

    fun save(
        field: String,
        value: String?,
    ) {
        scope.launch {
            message =
                withContext(Dispatchers.IO) {
                    runCatching { store.set(cartridge, field, value) }
                        .fold(
                            { "Saved. Border changes apply immediately; renderer options apply on next launch." },
                            { "Could not save display preferences. Existing values were retained." },
                        )
                }
        }
    }
    Column(
        (if (scrollable) modifier.verticalScroll(rememberScrollState()) else modifier).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (game == null) "Display defaults" else "Display · ${game.displayName()}",
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
            )
            TextButton(onClick = { helpOpen = true }) { Text("Help") }
        }
        snapshot.error?.let { Text(it) }
        if (game != null && cartridge == null) {
            Text(
                "This cartridge needs a stable library identity before an override can be saved.",
            )
        }
        val editable = snapshot.error == null && (game == null || cartridge != null)
        if (cartridge != null) {
            GameMenuLink("Use Global Default", enabled = editable) {
                scope.launch {
                    message =
                        withContext(Dispatchers.IO) {
                            runCatching { store.useGlobal(cartridge) }.fold(
                                { "Using global defaults." },
                                { "Existing preferences were retained." },
                            )
                        }
                }
            }
        }
        Text("Image Quality", style = MaterialTheme.typography.titleMedium)
        Text(
            "Set the mood of your game. Your cartridge keeps its own look when you choose an override.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (libretro) {
            val legacy = SmartVisual.usesLegacyQuality(snapshot.effective)
            val current = snapshot.effective.imageQuality ?: ImageQuality.AUTOMATIC.takeUnless { legacy }
            if (legacy) {
                val previous = snapshot.effective
                val detail = if (previous.hd == true) "HD enhancement" else previous.filter ?: "Console default"
                Text("Custom · existing preference ($detail)")
            }
            for ((quality, label, detail) in listOf(
                Triple(
                    ImageQuality.AUTOMATIC, "Automatic (recommended)",
                    "CARTDECK picks a safe console-aware finish.",
                ),
                Triple(
                    ImageQuality.ORIGINAL, "Original",
                    "The unfiltered picture, just as the renderer outputs it.",
                ),
                Triple(
                    ImageQuality.ENHANCED, "Enhanced",
                    "A crisp display treatment when this renderer supports it.",
                ),
            )) {
                val active = current == quality
                Card(
                    modifier = Modifier.fillMaxWidth().semantics { selected = active },
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                if (active) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                        ),
                ) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        GameMenuLink(label, enabled = editable) {
                            save("imageQuality", quality.name)
                        }
                        Text(
                            detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (active) {
                            Text(
                                "CURRENT LOOK",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            Text("Applies when the game is opened again.")
            if (game == null) Text("Applies to supported renderers.")
        } else {
            Text("Image Quality is controlled by this game's native renderer.")
        }
        Text("Screen fit and surrounds", style = MaterialTheme.typography.titleMedium)
        for ((landscape, field) in listOf(true to "landscapeBorder", false to "portraitBorder")) {
            Text(if (landscape) "Landscape border" else "Portrait border", style = MaterialTheme.typography.titleMedium)
            val options =
                listOf(
                    DisplaySettingsStore.Border.STANDARD,
                    DisplaySettingsStore.Border.MINIMAL,
                    DisplaySettingsStore.Border.OFF,
                )
            for (border in options) {
                val selected = snapshot.effective.border(landscape) == border
                val label =
                    when (border) {
                        DisplaySettingsStore.Border.STANDARD -> "Hardware"
                        DisplaySettingsStore.Border.MINIMAL -> "Minimal"
                        DisplaySettingsStore.Border.OFF -> "Off"
                    }
                GameMenuLink(
                    label + (if (selected) " · selected" else ""),
                    enabled = editable,
                ) {
                    save(field, border.name)
                }
            }
        }
        if (libretro) {
            GameMenuLink(
                if (advancedOpen) "Hide advanced display" else "Advanced display",
            ) { advancedOpen = !advancedOpen }
        }
        if (libretro && advancedOpen && namespace != null && game != null) {
            if (engineOptions.isEmpty()) Text("Launch this core once to verify its console-specific display options.")
            engineOptions.forEach { option ->
                val legacy =
                    SharedPreferencesHelper.getSharedPreferences(context).all[
                        CoreVariablesManager.computeSharedPreferenceKey(option.key, game.systemId),
                    ]
                val legacyValue =
                    if (legacy is Boolean) {
                        if (legacy) "enabled" else "disabled"
                    } else {
                        legacy as? String
                    }
                val value =
                    snapshot.effective.engineOptions["$namespace:${option.key}"]
                        ?: legacyValue ?: selectedConfig?.defaultSettings?.firstOrNull { it.key == option.key }?.value
                        ?: option.values.first()
                val index = option.values.indexOf(value).coerceAtLeast(0)
                GameMenuLink(
                    "${option.title}: ${option.labels.getOrElse(index) { option.values[index] }}",
                    enabled = editable,
                ) {
                    scope.launch {
                        message =
                            withContext(Dispatchers.IO) {
                                runCatching {
                                    store.setEngineOption(
                                        cartridge, namespace, option.key,
                                        option.values[(index + 1) % option.values.size], option.values.toSet(),
                                    )
                                }
                                    .fold(
                                        { "Saved. The core applies supported changes on return or next launch." },
                                        { "Display option was not saved. Existing values retained." },
                                    )
                            }
                    }
                }
            }
        }
        if (libretro && advancedOpen && snapshot.effective.imageQuality != null &&
            (if (cartridge == null) snapshot.effective else snapshot.own).hasLegacyQualityOverride()
        ) {
            GameMenuLink("Use previous image preference", enabled = editable) { save("imageQuality", null) }
        }
        if (libretro) {
            val alignments = listOf("top", "center", "bottom")
            GameMenuLink("Position: ${snapshot.effective.alignment ?: "center"}", enabled = editable) {
                save(
                    "alignment",
                    alignments[(alignments.indexOf(snapshot.effective.alignment).coerceAtLeast(0) + 1) % 3],
                )
            }
        }
        message?.let { Text(it) }
    }
    if (helpOpen) {
        AlertDialog(
            onDismissRequest = { helpOpen = false },
            title = { Text("Display help") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Global defaults apply unless a game has an override.")
                    Text("Original aspect preserves proportions. Border Off does not stretch or crop.")
                    Text("Original uses unenhanced rendering. Enhanced uses a display shader where supported.")
                    Text("Widescreen and other engine options appear only when supported.")
                    Text("Native renderers control their own scaling and positioning.")
                    Text("Border changes apply immediately. Renderer changes may require reopening the game.")
                    Text("Games never restart automatically.")
                }
            },
            confirmButton = { TextButton(onClick = { helpOpen = false }) { Text("Done") } },
        )
    }
}
