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
            SystemID.PSX, SystemID.PSP, SystemID.PLAYSTATION_2 ->
                listOf(
                    a.copy(label = "Cross"),
                    b.copy(label = "Circle"),
                    x.copy(label = "Square"),
                    y.copy(label = "Triangle"),
                    l, r, l2, r2, start, select,
                )
            SystemID.NGC -> listOf(a, b, x, y, l, r, l2, r2, start)
            SystemID.DREAMCAST -> listOf(a, b, x, y, start)
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
    private val preferences =
        context.applicationContext.getSharedPreferences("cartdeck_arc_cartridge_profiles_v1", Context.MODE_PRIVATE)

    fun read(game: Game): GripArcProfile {
        val fallback =
            if (PivotArcSettingsStoreContext.readMode(preferences, game, context = null)) {
                GripArcProfile(mode = ArcControlMode.PIVOTARC)
            } else {
                GripArcProfile()
            }
        val raw = preferences.getString(key(game), null) ?: return fallback
        return decode(raw, fallback, GripArcInputCatalog.forGame(game).map { it.code }.toSet())
    }

    /** Saved mode overrides global PivotArc; older unconfigured cartridges retain the existing global choice. */
    fun hasSelection(game: Game): Boolean = preferences.contains(key(game))

    fun save(game: Game, profile: GripArcProfile): Boolean {
        val checked = profile.validated(GripArcInputCatalog.forGame(game).map { it.code }.toSet())
        return preferences.edit().putString(key(game), encode(checked)).commit()
    }

    fun listen(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unlisten(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun key(game: Game): String = "game:" + shelfGameKey(game.systemId, game.fileUri)

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

/** Transitional reader keeps previously configured global PivotArc for untouched games. */
private object PivotArcSettingsStoreContext {
    fun readMode(
        ignored: SharedPreferences,
        game: Game,
        context: Context?,
    ): Boolean = false
}

@Composable
fun rememberGripArcProfile(game: Game): State<GripArcProfile> {
    val context = LocalContext.current.applicationContext
    val store = remember(context) { GripArcProfileStore(context) }
    val state = remember(game.systemId, game.fileUri, store) { mutableStateOf(store.read(game)) }
    DisposableEffect(game.systemId, game.fileUri, store) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            state.value = store.read(game)
        }
        store.listen(listener)
        onDispose { store.unlisten(listener) }
    }
    return state
}
