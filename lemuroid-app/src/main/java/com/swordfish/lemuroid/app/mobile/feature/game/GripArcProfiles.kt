package com.swordfish.lemuroid.app.mobile.feature.game

import android.content.Context
import android.content.SharedPreferences
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
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
            SystemID.GB, SystemID.GBC, SystemID.NES ->
                listOf(a, b, start, select)
            SystemID.PC_ENGINE -> listOf(a.copy(label = "II"), b.copy(label = "I"), start.copy(label = "Run"), select)
            SystemID.GENESIS, SystemID.SEGACD ->
                listOf(
                    y.copy(label = "A"),
                    b,
                    a.copy(label = "C"),
                    l.copy(label = "X"),
                    x.copy(label = "Y"),
                    r.copy(label = "Z"),
                    select.copy(label = "Mode"),
                    start,
                )
            SystemID.SMS -> listOf(a.copy(label = "2"), b.copy(label = "1"), start.copy(label = "Pause"))
            SystemID.GBA -> listOf(a, b, l, r, start, select)
            // Labels follow the existing PadKit touch layouts, not guessed console ABXY.
            // Game Gear uses Button 2 on Android A and Button 1 on Android B.
            SystemID.GG -> listOf(a.copy(label = "2"), b.copy(label = "1"), start)
            SystemID.ATARI2600 ->
                listOf(
                    b.copy(label = "Fire"),
                    l.copy(label = "Diff A"),
                    l2.copy(label = "Diff B"),
                    start.copy(label = "Reset"),
                    select,
                )
            SystemID.ATARI7800 -> listOf(a.copy(label = "2"), b.copy(label = "1"), start, select)
            // Lynx Options are genuine guest L1/R1 inputs; Start is the Pause key.
            SystemID.LYNX ->
                listOf(a, b, l.copy(label = "Option 1"), r.copy(label = "Option 2"), start.copy(label = "Pause"))
            // NGC is Neo Geo Pocket Color (not Nintendo GameCube); both use the NGP PadKit layout.
            // PadKit shows B on Android A and A on Android B. Preserve the existing guest key codes.
            SystemID.NGP, SystemID.NGC -> listOf(a.copy(label = "B"), b.copy(label = "A"), start.copy(label = "Option"))
            SystemID.SNES, SystemID.NDS, SystemID.NINTENDO_3DS ->
                listOf(a, b, x, y, l, r, start, select)
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
            SystemID.PSX, SystemID.PLAYSTATION_2, SystemID.PLAYSTATION_3 ->
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
            SystemID.XBOX, SystemID.XBOX360 ->
                listOf(
                    a, b, x, y,
                    l.copy(label = if (system == SystemID.XBOX) "White" else "LB"),
                    r.copy(label = if (system == SystemID.XBOX) "Black" else "RB"),
                    l2.copy(label = "LT"), r2.copy(label = "RT"), start, select.copy(label = "Back"),
                )
            else -> emptyList()
        }

    fun forGame(game: Game): List<GripArcInput> =
        forSystem(SystemID.values().firstOrNull { it.dbname == game.systemId })
}

/** The legacy integer lists are retained as the native guest identities for all four slots. */
enum class GripArcBehavior { MOMENTARY, HOLD, TURBO, MENU }

sealed interface GripArcAction {
    data object OpenMenu : GripArcAction

    data class Native(val keys: Set<Int>) : GripArcAction

    data class Hold(val key: Int) : GripArcAction

    data class Turbo(val key: Int, val rate: Int = 10) : GripArcAction
}

/** All four slot mappings persist, even if only one button is currently visible. */
data class GripArcProfile(
    val mode: ArcControlMode = ArcControlMode.STANDARD,
    val side: GripArcSide = GripArcSide.RIGHT,
    val count: Int = 4,
    val mappings: List<List<Int>> = List(4) { emptyList() },
    val dx: Float = 0f,
    val dy: Float = 0f,
    val behaviors: List<GripArcBehavior> = List(4) { GripArcBehavior.MOMENTARY },
    val turboRate: Int = 10,
) {
    fun actionFor(slot: Int): GripArcAction {
        val keys = mappings.getOrNull(slot - 1).orEmpty()
        val behavior = behaviors.getOrElse(slot - 1) { GripArcBehavior.MOMENTARY }
        if (behavior == GripArcBehavior.MENU) return GripArcAction.OpenMenu
        val key = keys.singleOrNull() ?: return GripArcAction.Native(keys.toSet())
        return when (behavior) {
            GripArcBehavior.MOMENTARY -> GripArcAction.Native(setOf(key))
            GripArcBehavior.HOLD -> GripArcAction.Hold(key)
            GripArcBehavior.TURBO -> GripArcAction.Turbo(key, turboRate.coerceIn(3, 15))
            GripArcBehavior.MENU -> GripArcAction.OpenMenu
        }
    }

    fun supportedActionFor(
        slot: Int,
        allowed: Set<Int>,
    ): GripArcAction? {
        val action = actionFor(slot)
        return when (action) {
            GripArcAction.OpenMenu -> action
            is GripArcAction.Native -> action.takeIf { allowed.containsAll(it.keys) }
            is GripArcAction.Hold -> action.takeIf { it.key in allowed }
            is GripArcAction.Turbo -> action.takeIf { it.key in allowed }
        }
    }

    fun labelFor(
        slot: Int,
        labels: Map<Int, String>,
    ): String =
        when (val action = actionFor(slot)) {
            GripArcAction.OpenMenu -> "Game menu"
            is GripArcAction.Native -> action.keys.mapNotNull(labels::get).joinToString("+")
            is GripArcAction.Hold -> labels[action.key]?.let { "Hold $it" }.orEmpty()
            is GripArcAction.Turbo -> labels[action.key]?.let { "Turbo $it" }.orEmpty()
        }

    fun validated(allowed: Set<Int>): GripArcProfile =
        copy(
            count = count.coerceIn(1, 4),
            mappings =
                (0..3).map { slot ->
                    mappings.getOrNull(slot).orEmpty()
                        .filter { it in allowed }.distinct().take(4)
                },
            behaviors = (0..3).map { behaviors.getOrElse(it) { GripArcBehavior.MOMENTARY } },
            turboRate = turboRate.coerceIn(3, 15),
            dx = dx.takeIf { it.isFinite() && it in -1000f..1000f } ?: 0f,
            dy = dy.takeIf { it.isFinite() && it in -1000f..1000f } ?: 0f,
        )
}

internal fun supportsStandaloneGripActions(system: SystemID?): Boolean =
    system in
        setOf(
            SystemID.GBA, SystemID.GB, SystemID.GBC, SystemID.NES, SystemID.SMS,
            SystemID.GG, SystemID.NGP, SystemID.NGC, SystemID.PC_ENGINE, SystemID.SNES,
            SystemID.GENESIS, SystemID.SEGACD, SystemID.ATARI2600, SystemID.ATARI7800, SystemID.LYNX,
            SystemID.DREAMCAST, SystemID.GAMECUBE, SystemID.PSP, SystemID.PSX, SystemID.NINTENDO_3DS,
        )

internal fun CartdeckLayoutMode.forSystem(system: SystemID?): ArcControlMode =
    when (this) {
        CartdeckLayoutMode.STANDARD -> ArcControlMode.STANDARD
        CartdeckLayoutMode.ONE_HAND -> ArcControlMode.PIVOTARC
        CartdeckLayoutMode.GRIPARC ->
            if (supportsStandaloneGripActions(system)) ArcControlMode.STANDARD_GRIPARC else ArcControlMode.STANDARD
    }

internal fun standaloneGripFactory(system: SystemID?): GripArcProfile? {
    if (!supportsStandaloneGripActions(system)) return null
    val a = KeyEvent.KEYCODE_BUTTON_A
    val b = KeyEvent.KEYCODE_BUTTON_B
    val shoulderChord = listOf(KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1)
    if (system == SystemID.SNES) {
        return GripArcProfile(
            mappings =
                listOf(shoulderChord, listOf(KeyEvent.KEYCODE_BUTTON_Y), listOf(b), listOf(KeyEvent.KEYCODE_BUTTON_X)),
            behaviors =
                listOf(GripArcBehavior.MOMENTARY, GripArcBehavior.HOLD, GripArcBehavior.TURBO, GripArcBehavior.HOLD),
        )
    }
    if (system == SystemID.GENESIS || system == SystemID.SEGACD) {
        return GripArcProfile(
            mappings = listOf(listOf(b), listOf(b), listOf(a), listOf(b, a)),
            behaviors =
                listOf(
                    GripArcBehavior.HOLD,
                    GripArcBehavior.TURBO,
                    GripArcBehavior.TURBO,
                    GripArcBehavior.MOMENTARY,
                ),
        )
    }
    if (system == SystemID.ATARI2600) {
        return GripArcProfile(
            count = 2,
            mappings = listOf(listOf(b), listOf(b), emptyList(), emptyList()),
            behaviors = listOf(GripArcBehavior.HOLD, GripArcBehavior.TURBO),
        )
    }
    if (system == SystemID.LYNX) {
        return GripArcProfile(
            mappings = listOf(shoulderChord, listOf(a), listOf(b), listOf(a, b)),
            behaviors =
                listOf(
                    GripArcBehavior.MOMENTARY,
                    GripArcBehavior.HOLD,
                    GripArcBehavior.HOLD,
                    GripArcBehavior.MOMENTARY,
                ),
        )
    }
    if (system == SystemID.DREAMCAST || system == SystemID.GAMECUBE) {
        val face = if (system == SystemID.DREAMCAST) listOf(b, a) else listOf(a, b)
        val keys = listOf(KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2) + face
        return GripArcProfile(mappings = keys.map { listOf(it) }, behaviors = List(4) { GripArcBehavior.HOLD })
    }
    if (system == SystemID.NINTENDO_3DS) {
        return GripArcProfile(
            count = 2,
            mappings = listOf(listOf(KeyEvent.KEYCODE_BUTTON_Y), listOf(b), emptyList(), emptyList()),
            behaviors = listOf(GripArcBehavior.HOLD, GripArcBehavior.HOLD),
        )
    }
    if (system == SystemID.PSX) {
        return GripArcProfile(
            mappings =
                listOf(shoulderChord, listOf(KeyEvent.KEYCODE_BUTTON_Y), listOf(b), listOf(KeyEvent.KEYCODE_BUTTON_R2)),
            behaviors =
                listOf(GripArcBehavior.MOMENTARY, GripArcBehavior.HOLD, GripArcBehavior.HOLD, GripArcBehavior.HOLD),
        )
    }
    if (system == SystemID.PSP) {
        return GripArcProfile(
            mappings =
                listOf(shoulderChord, listOf(KeyEvent.KEYCODE_BUTTON_Y), listOf(b), listOf(KeyEvent.KEYCODE_BUTTON_X)),
            behaviors =
                listOf(
                    GripArcBehavior.MOMENTARY,
                    GripArcBehavior.HOLD,
                    GripArcBehavior.TURBO,
                    GripArcBehavior.HOLD,
                ),
        )
    }
    val gba = system == SystemID.GBA
    val alternateLegendOrder =
        system in setOf(SystemID.SMS, SystemID.GG, SystemID.NGP, SystemID.NGC, SystemID.PC_ENGINE, SystemID.ATARI7800)
    return GripArcProfile(
        mappings =
            listOf(
                if (gba) shoulderChord else listOf(b),
                listOf(a),
                listOf(b),
                if (alternateLegendOrder) listOf(b, a) else listOf(a, b),
            ),
        behaviors =
            listOf(
                if (gba) GripArcBehavior.MOMENTARY else GripArcBehavior.HOLD,
                GripArcBehavior.TURBO,
                GripArcBehavior.TURBO,
                GripArcBehavior.MOMENTARY,
            ),
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
        val system = SystemID.entries.firstOrNull { it.dbname == game.systemId }
        standaloneGripFactory(system)?.let { return it }
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
        val system = SystemID.entries.firstOrNull { it.dbname == game.systemId }
        val fallback = defaultFor(game).copy(mode = PivotArcSettingsStore(appContext).read().mode.forSystem(system))
        val raw =
            preferences.getString(key(game), null)
                ?: preferences.getString(legacyKey(game, game.legacySourceUri ?: game.fileUri), null)
                ?: preferences.getString(legacyKey(game, game.fileUri), null)
                ?: return fallback
        val profile = decode(raw, fallback, allowedCodes(game))
        return if (preferences.getBoolean(layoutInheritanceKey(game), false)) {
            profile.copy(mode = fallback.mode)
        } else {
            profile
        }
    }

    /** Saved mode overrides global PivotArc; older unconfigured cartridges retain the existing global choice. */
    fun hasSelection(game: Game): Boolean =
        !preferences.getBoolean(layoutInheritanceKey(game), false) &&
            (
                preferences.contains(key(game)) ||
                    preferences.contains(legacyKey(game, game.legacySourceUri ?: game.fileUri)) ||
                    preferences.contains(legacyKey(game, game.fileUri))
            )

    fun useGlobalLayout(game: Game): Boolean = preferences.edit().putBoolean(layoutInheritanceKey(game), true).commit()

    fun saveLayout(
        game: Game,
        mode: ArcControlMode,
    ): Boolean =
        preferences.edit().putString(key(game), encode(read(game).copy(mode = mode)))
            .remove(layoutInheritanceKey(game)).commit()

    private fun layoutInheritanceKey(game: Game) = "${key(game)}:inherit_layout"

    fun save(
        game: Game,
        profile: GripArcProfile,
    ): Boolean {
        val checked = profile.validated(allowedCodes(game))
        return preferences.edit().putString(key(game), encode(checked)).commit()
    }

    private fun allowedCodes(game: Game): Set<Int> {
        val existing = GripArcInputCatalog.forGame(game).mapTo(mutableSetOf()) { it.code }
        if (game.systemId == SystemID.GAMECUBE.dbname) existing += KeyEvent.KEYCODE_BUTTON_L2
        return existing
    }

    fun orientationOverride(game: Game): ControlsOrientation? =
        preferences.getString("${key(game)}:orientation", null)?.let { value ->
            ControlsOrientation.entries.firstOrNull { it.name == value }
        }

    fun saveOrientation(
        game: Game,
        value: ControlsOrientation?,
    ): Boolean = preferences.edit().putString("${key(game)}:orientation", value?.name).commit()

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
                put("behaviors", JSONArray(profile.behaviors.map { it.name }))
                put("turboRate", profile.turboRate)
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
                    behaviors =
                        (0..3).map { slot ->
                            val value = root.optJSONArray("behaviors")?.optString(slot)
                            GripArcBehavior.entries.firstOrNull { it.name == value } ?: GripArcBehavior.MOMENTARY
                        },
                    turboRate = root.optInt("turboRate", 10),
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
    val global by rememberPivotArcSettings()
    val state = remember(game.cartridgeId, store, global.mode) { mutableStateOf(store.read(game)) }
    DisposableEffect(game.cartridgeId, store, state) {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                state.value = store.read(game)
            }
        store.listen(listener)
        onDispose { store.unlisten(listener) }
    }
    return state
}
