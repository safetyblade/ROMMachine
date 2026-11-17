package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.game.DisplayEngineOptions
import com.swordfish.lemuroid.app.shared.game.DisplaySettingsStore
import com.swordfish.lemuroid.app.shared.game.engine.EngineSelectionPreferences
import com.swordfish.lemuroid.app.shared.game.engine.NativeEngineRegistry
import com.swordfish.lemuroid.lib.core.CoreVariablesManager
import com.swordfish.lemuroid.lib.library.GameSystem
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
    val libretro =
        libretroHost ?: remember(game?.systemId, cartridge) {
            game == null ||
                runCatching {
                    val host = EngineSelectionPreferences(context).select(game, null).engine.host
                    host == NativeEngineRegistry.Host.LIBRETRO
                }.getOrDefault(GameSystem.findById(game.systemId).nativeBackend == null)
        }

    val selectedConfig =
        remember(game?.systemId, cartridge, coreConfig) {
            coreConfig ?: game?.let {
                runCatching { EngineSelectionPreferences(context).select(it, null).engine.config }
                    .getOrNull()
            }
        }
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
        Text(
            if (game == null) "Display defaults" else "Display · ${game.displayName()}",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            if (game == null) {
                "Defaults apply to games without an override."
            } else {
                "This game uses global defaults unless you choose an override."
            },
        )
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
        Text("Screen aspect: Auto / Original", style = MaterialTheme.typography.titleMedium)
        Text(
            "The renderer preserves source proportions. Border Off never stretches or crops gameplay.",
        )
        if (engineOptions.none { "widescreen" in it.key || "aspect_ratio" in it.key }) {
            GameMenuLink("Widescreen — unavailable for this engine", enabled = false) {}
        }
        Text(
            "Additional display choices appear when supported by the selected engine.",
        )
        for ((landscape, field) in listOf(true to "landscapeBorder", false to "portraitBorder")) {
            Text(if (landscape) "Landscape border" else "Portrait border", style = MaterialTheme.typography.titleMedium)
            for (border in DisplaySettingsStore.Border.entries) {
                val selected = snapshot.effective.border(landscape) == border
                GameMenuLink(
                    (if (border == DisplaySettingsStore.Border.OFF) "Off" else "Standard") +
                        (if (selected) " · selected" else ""),
                    enabled = editable,
                ) {
                    save(field, border.name)
                }
            }
        }
        if (libretro && namespace != null && game != null) {
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
        if (libretro) {
            Text("Renderer options · apply on next launch", style = MaterialTheme.typography.titleMedium)
            GameMenuLink("HD scaling: ${if (snapshot.effective.hd == true) "On" else "Off"}", enabled = editable) {
                save("hd", (snapshot.effective.hd != true).toString())
            }
            GameMenuLink(
                "HD quality: ${listOf(
                    "Low",
                    "Medium",
                    "High",
                ).getOrElse(snapshot.effective.quality ?: 1) { "Medium" }}",
                enabled = editable && snapshot.effective.hd == true,
            ) {
                save("quality", (((snapshot.effective.quality ?: 1) + 1) % 3).toString())
            }
            val filters = context.resources.getStringArray(R.array.pref_key_shader_filter_values).toList()
            val labels = context.resources.getStringArray(R.array.pref_key_shader_filter_display_names).toList()
            val selected = filters.indexOf(snapshot.effective.filter).coerceAtLeast(0)
            GameMenuLink(
                "Screen filter: ${labels.getOrElse(selected) { filters[selected] }}",
                enabled = editable && snapshot.effective.hd != true,
            ) {
                save("filter", filters[(selected + 1) % filters.size])
            }
            val alignments = listOf("top", "center", "bottom")
            GameMenuLink("Position: ${snapshot.effective.alignment ?: "center"}", enabled = editable) {
                save(
                    "alignment",
                    alignments[(alignments.indexOf(snapshot.effective.alignment).coerceAtLeast(0) + 1) % 3],
                )
            }
        } else {
            Text(
                "Scaling and positioning are controlled by this native renderer. " +
                    "Screen and touch arrangements remain unchanged.",
            )
        }
        Text(
            "No game is restarted automatically. Exit safely and reopen when a renderer option requires a new session.",
        )
        message?.let { Text(it) }
    }
}
