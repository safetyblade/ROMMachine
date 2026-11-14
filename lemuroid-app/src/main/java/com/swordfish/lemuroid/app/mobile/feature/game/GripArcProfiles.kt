package com.swordfish.lemuroid.app.mobile.feature.game

import android.content.Context
import android.content.SharedPreferences
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.swordfish.lemuroid.app.mobile.feature.shelf.shelfGameKey
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import org.json.JSONArray
import org.json.JSONObject

/** Exactly one selected control layout per cartridge. */
enum class ArcControlMode(val label: String, val hasPivot: Boolean, val hasGrip: Boolean) {
    STANDARD("Standard", false, false),
    PIVOTARC("PivotArc", true, false),
    STANDARD_GRIPARC("Standard + GripArc", false, true),
    PIVOTARC_GRIPARC("PivotArc + GripArc", true, true),
}

enum class GripArcSide { LEFT, RIGHT }

data class GripArcInput(val label: String, val code: Int)

/** Same Android/PadKit button identities used by the native touch controls. */
object GripArcInputCatalog {
    private val a = GripArcInput("A", KeyEvent.KEYCODE_BUTTON_A)
    private val b = GripArcInput("B", KeyEvent.KEYCODE_BUTTON_B)
    private val x = GripArcInput("X", KeyEvent.KEYCODE_BUTTON_X)
    private val y = GripArcInput("Y", KeyEvent.KEYCODE_BUTTON_Y)
    private val l = GripArcInput("L1", KeyEvent.KEYCODE_BUTTON_L1)
    private val r = GripArcInput("R1", KeyEvent.KEYCODE_BUTTON_R1)
    private val l2 = GripArcInput("L2", KeyEvent.KEYCODE_BUTTON_L2)
    private val r2 = GripArcInput("R2", KeyEvent.KEYCODE_BUTTON_R2)
    private val start = GripArcInput("Start", KeyEvent.KEYCODE_BUTTON_START)
    private val select = GripArcInput("Select", KeyEvent.KEYCODE_BUTTON_SELECT)

    fun forSystem(system: SystemID?): List<GripArcInput> =
        when (system) {
            SystemID.GB, SystemID.GBC -> listOf(a, b, start, select)
            SystemID.GBA -> listOf(a, b, l, r, start, select)
            SystemID.SNES, SystemID.NDS -> listOf(a, b, x, y, l, r, start, select)
            SystemID.PSP ->
                listOf(
                    a.copy(label = "Circle"),
                    b.copy(label = "Cross"),
                    x.copy(label = "Triangle"),
                    y.copy(label = "Square"),
                    l,
                    r,
                    start,
                    select,
                )
            SystemID.PSX, SystemID.PLAYSTATION_2 ->
                listOf(
                    a.copy(label = "Circle"),
                    b.copy(label = "Cross"),
                    x.copy(label = "Triangle"),
                    y.copy(label = "Square"),
                    l, r, l2, r2, start, select,
                )
            SystemID.GAMECUBE ->
                listOf(
                    a, b, x, y,
                    l.copy(label = "L"),
                    r.copy(label = "R"),
                    r2.copy(label = "Z"),
                    GripArcInput("L half", KeyEvent.KEYCODE_BUTTON_THUMBL),
                    GripArcInput("R half", KeyEvent.KEYCODE_BUTTON_THUMBR),
                    start,
                )
            // NGC refers to Neo Geo Pocket Color, not Nintendo GameCube.
            SystemID.NGC -> listOf(a, b, start, select)
            // Flycast: DC A/B/X/Y = RetroPad B/A/Y/X; triggers = L2/R2.
            // Preserve persisted RetroPad keycodes, change only labels and
            // expose previously missing trigger choices to GripArc.
            SystemID.DREAMCAST ->
                listOf(
                    b.copy(label = "A"),
                    a.copy(label = "B"),
                    y.copy(label = "X"),
                    x.copy(label = "Y"),
                    l2.copy(label = "L"),
                    r2.copy(label = "R"),
                    start,
                )
            SystemID.XBOX360 ->
                listOf(
                    a, b, x, y, l.copy(label = "LB"), r.copy(label = "RB"),
                    l2.copy(label = "LT"), r2.copy(label = "RT"), start, select.copy(label = "Back"),
                )
            else -> emptyList()
        }

    fun forGame(game: Game): List<GripArcInput> =
        forSystem(SystemID.values().firstOrNull { it.dbname == game.systemId })
}

/** All four slot mappings persist, even if only one button is currently visible. */
data class GripArcProfile(
    val mode: ArcControlMode = ArcControlMode.STANDARD,
    val side: GripArcSide = GripArcSide.RIGHT,
    val count: Int = 1,
    val mappings: List<List<Int>> = List(4) { emptyList() },
    val dx: Float = 0f,
    val dy: Float = 0f,
) {
    fun validated(allowed: Set<Int>): GripArcProfile =
        copy(
            count = count.coerceIn(1, 4),
            mappings =
                (0..3).map { slot ->
                    mappings.getOrNull(slot).orEmpty()
                        .filter { it in allowed }.distinct().take(4)
                },
            dx = dx.takeIf { it.isFinite() && it in -1000f..1000f } ?: 0f,
            dy = dy.takeIf { it.isFinite() && it in -1000f..1000f } ?: 0f,
        )
}

/**
 * Additive, versioned per-cartridge profile. SharedPreferences provides atomic
 * durable value replacement; corrupt or future JSON falls back without writing
 * to ROMs, save RAM, shelf layout, or the original settings files.
 */
class GripArcProfileStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences =
        appContext.getSharedPreferences("cartdeck_arc_cartridge_profiles_v1", Context.MODE_PRIVATE)

    fun defaultFor(game: Game): GripArcProfile {
        val supported = GripArcInputCatalog.forGame(game)
        val possible = supported.map { it.code }
        val preferred =
            listOf(
                KeyEvent.KEYCODE_BUTTON_L1,
                KeyEvent.KEYCODE_BUTTON_R1,
                KeyEvent.KEYCODE_BUTTON_A,
                KeyEvent.KEYCODE_BUTTON_B,
            )
        val assigned = preferred.filter { it in possible }.map { listOf(it) }
        val mappings = assigned + List(4 - assigned.size) { emptyList() }
        return GripArcProfile(mappings = mappings)
    }

    fun read(game: Game): GripArcProfile {
        val previousGlobal = PivotArcSettingsStore(appContext).read().mode == CartdeckLayoutMode.ONE_HAND
        val fallback =
            defaultFor(game).copy(
                mode = if (previousGlobal) ArcControlMode.PIVOTARC else ArcControlMode.STANDARD,
            )
        val raw =
            preferences.getString(key(game), null)
                ?: preferences.getString(legacyKey(game, game.legacySourceUri ?: game.fileUri), null)
                ?: preferences.getString(legacyKey(game, game.fileUri), null)
                ?: return fallback
        return decode(raw, fallback, GripArcInputCatalog.forGame(game).map { it.code }.toSet())
    }

    /** Saved mode overrides global PivotArc; older unconfigured cartridges retain the existing global choice. */
    fun hasSelection(game: Game): Boolean =
        preferences.contains(key(game)) ||
            preferences.contains(legacyKey(game, game.legacySourceUri ?: game.fileUri)) ||
            preferences.contains(legacyKey(game, game.fileUri))

    fun save(
        game: Game,
        profile: GripArcProfile,
    ): Boolean {
        val checked = profile.validated(GripArcInputCatalog.forGame(game).map { it.code }.toSet())
        return preferences.edit().putString(key(game), encode(checked)).commit()
    }

    fun listen(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unlisten(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    /** UUID becomes the durable key; retain read-only compatibility with old URI keys. */
    private fun key(game: Game): String = "cartridge:" + game.cartridgeId

    private fun legacyKey(
        game: Game,
        uri: String,
    ): String = "game:" + shelfGameKey(game.systemId, uri)

    companion object {
        const val VERSION = 1

        internal fun encode(profile: GripArcProfile): String =
            JSONObject().apply {
                put("schema", VERSION)
                put("mode", profile.mode.name)
                put("side", profile.side.name)
                put("count", profile.count)
                put("dx", profile.dx.toDouble())
                put("dy", profile.dy.toDouble())
                put(
                    "mappings",
                    JSONArray().apply {
                        profile.mappings.forEach { keys ->
                            put(JSONArray().apply { keys.forEach { put(it) } })
                        }
                    },
                )
            }.toString()

        internal fun decode(
            raw: String,
            fallback: GripArcProfile,
            allowed: Set<Int>,
        ): GripArcProfile =
            runCatching {
                val root = JSONObject(raw)
                require(root.getInt("schema") == VERSION)
                val mappings = root.optJSONArray("mappings") ?: JSONArray()
                GripArcProfile(
                    mode = ArcControlMode.valueOf(root.getString("mode")),
                    side = GripArcSide.valueOf(root.optString("side", "RIGHT")),
                    count = root.optInt("count", 1),
                    dx = root.optDouble("dx", 0.0).toFloat(),
                    dy = root.optDouble("dy", 0.0).toFloat(),
                    mappings =
                        (0..3).map { slot ->
                            val codes = mappings.optJSONArray(slot) ?: JSONArray()
                            (0 until codes.length()).mapNotNull { i ->
                                codes.optInt(i, -1).takeIf { it >= 0 }
                            }
                        },
                ).validated(allowed)
            }.getOrDefault(fallback)
    }
}

@Composable
fun rememberGripArcProfile(game: Game): State<GripArcProfile> {
    val context = LocalContext.current.applicationContext
    val store = remember(context) { GripArcProfileStore(context) }
    val state = remember(game.cartridgeId, store) { mutableStateOf(store.read(game)) }
    DisposableEffect(game.cartridgeId, store) {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                state.value = store.read(game)
            }
        store.listen(listener)
        onDispose { store.unlisten(listener) }
    }
    return state
}
