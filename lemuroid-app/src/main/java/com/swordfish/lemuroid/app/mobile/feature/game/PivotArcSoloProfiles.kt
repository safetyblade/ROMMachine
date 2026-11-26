package com.swordfish.lemuroid.app.mobile.feature.game

import android.view.KeyEvent
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.touchinput.radial.layouts.shared.ComposeTouchLayouts
import com.swordfish.touchinput.radial.settings.TouchControllerID
import kotlin.math.max

/** An approved solo seat binds geometry to the actual active controller mode. */
data class PivotArcSoloSeat(
    val id: String,
    val x: Float,
    val y: Float,
    val label: String = id,
    val keyCode: Int? = null,
    val direction: Int? = null,
    val analog: Boolean = false,
    val diagonals: Boolean = true,
    val holdOf: String? = null,
    val utility: Boolean = false,
    val target: Float = 48.5f,
    val cap: Float = 36f,
)

data class PivotArcSoloProfile(val seats: List<PivotArcSoloSeat>) {
    init {
        require(seats.map { it.id }.distinct().size == seats.size)
        require(seats.all { it.target >= 48f && it.cap <= it.target })
        require(
            seats.filter { it.holdOf != null }.all { hold ->
                seats.any { it.id == hold.holdOf && it.keyCode != null }
            },
        )
    }

    fun seat(id: String): PivotArcSoloSeat? = seats.firstOrNull { it.id == id }

    val holdKeys: Set<Int>
        get() = seats.mapNotNull { it.holdOf?.let { key -> seat(key)?.keyCode } }.toSet()
}

/**
 * PA-R01 source-backed solo profiles. Legacy GBA/PSP and Joint are not routed
 * through this new solver. Native-host modes require a separate verified bridge.
 */
object PivotArcSoloProfiles {
    private val a = KeyEvent.KEYCODE_BUTTON_A
    private val b = KeyEvent.KEYCODE_BUTTON_B
    private val x = KeyEvent.KEYCODE_BUTTON_X
    private val y = KeyEvent.KEYCODE_BUTTON_Y
    private val l = KeyEvent.KEYCODE_BUTTON_L1
    private val r = KeyEvent.KEYCODE_BUTTON_R1
    private val l2 = KeyEvent.KEYCODE_BUTTON_L2
    private val r2 = KeyEvent.KEYCODE_BUTTON_R2
    private val ls = KeyEvent.KEYCODE_BUTTON_THUMBL
    private val rs = KeyEvent.KEYCODE_BUTTON_THUMBR

    private fun button(
        id: String,
        px: Int,
        py: Int,
        code: Int,
        label: String = id,
    ) = PivotArcSoloSeat(id, px.toFloat(), py.toFloat(), label, keyCode = code)

    private fun hold(
        id: String,
        px: Int,
        py: Int,
    ) = PivotArcSoloSeat("Hold$id", px.toFloat(), py.toFloat(), holdOf = id, cap = 28f)

    private fun dpad(
        px: Int,
        py: Int,
        size: Float = 64f,
        source: Int = ComposeTouchLayouts.MOTION_SOURCE_DPAD,
        id: String = "D",
        diagonals: Boolean = true,
    ) = PivotArcSoloSeat(
        id,
        px.toFloat(),
        py.toFloat(),
        direction = source,
        diagonals = diagonals,
        target = size,
        cap = size - 8f,
    )

    private fun stick(
        id: String,
        px: Int,
        py: Int,
        source: Int,
        size: Float = 72f,
    ) = PivotArcSoloSeat(
        id,
        px.toFloat(),
        py.toFloat(),
        direction = source,
        analog = true,
        target = size,
        cap = size - 8f,
    )

    private fun utility(
        id: String,
        code: Int,
        label: String = id,
    ) = PivotArcSoloSeat(id, 0f, 0f, label, keyCode = code, utility = true, cap = 36f)

    private fun rail(
        select: Boolean = true,
        selectLabel: String = "Select",
        startLabel: String = "Start",
    ): List<PivotArcSoloSeat> =
        buildList {
            add(utility("Menu", KeyEvent.KEYCODE_BUTTON_MODE))
            add(utility("Start", KeyEvent.KEYCODE_BUTTON_START, startLabel))
            if (select) add(utility("Select", KeyEvent.KEYCODE_BUTTON_SELECT, selectLabel))
        }

    private fun sony(
        dual: Boolean,
        dos: Boolean = false,
    ): PivotArcSoloProfile {
        val dx = if (dual) 88 else 0
        val seats =
            buildList {
                add(dpad(22 + dx, 185, 72f))
                add(button("Y", 76 + dx, 84, y, if (dos) "X" else "Square"))
                add(button("X", 136 + dx, 120, x, if (dos) "Y" else "Triangle"))
                add(button("B", 136 + dx, 272, b, if (dos) "A" else "Cross"))
                add(button("A", 76 + dx, 318, a, if (dos) "B" else "Circle"))
                add(hold("Y", 76 + dx, 24))
                add(hold("X", 212 + dx, 110))
                add(hold("B", 212 + dx, 300))
                add(hold("A", 76 + dx, 376))
                add(button("L", 190 + dx, 173, l, "L1"))
                add(button("R", 244 + dx, 173, r, "R1"))
                add(button("L2", 190 + dx, 227, l2))
                add(button("R2", 244 + dx, 227, r2))
                if (dual) {
                    add(stick("AnalogL", 36, 124, ComposeTouchLayouts.MOTION_SOURCE_LEFT_STICK, 64f))
                    add(stick("AnalogR", 36, 248, ComposeTouchLayouts.MOTION_SOURCE_RIGHT_STICK, 64f))
                    if (!dos) {
                        add(button("LS", 36, 42, ls, "L3"))
                        add(button("RS", 36, 330, rs, "R3"))
                    }
                }
                addAll(rail())
                if (dos) add(utility("Keyboard", ls, "Keyboard"))
            }
        return PivotArcSoloProfile(
            if (!dos) {
                seats
            } else {
                // DOS_AUTO retains the host ABXY identities corresponding to the visible legends.
                seats.map {
                    when (it.label) {
                        "X" -> it.copy(keyCode = x)
                        "Y" -> it.copy(keyCode = y)
                        "A" -> it.copy(keyCode = a)
                        "B" -> it.copy(keyCode = b)
                        else -> it
                    }
                }
            },
        )
    }

    private fun snes(): PivotArcSoloProfile =
        PivotArcSoloProfile(
            listOf(
                dpad(136, 153, 72f),
                button("X", 164, 64, x),
                button("A", 217, 103, a),
                button("B", 212, 213, b),
                button("Y", 159, 253, y),
                button("L", 212, 158, l),
                button("R", 262, 158, r),
                hold("X", 212, 64),
                hold("A", 264, 103),
                hold("B", 264, 213),
                hold("Y", 159, 293),
            ) + rail(),
        )

    private fun ds(controller: TouchControllerID): PivotArcSoloProfile {
        val melon = controller == TouchControllerID.MELONDS
        return PivotArcSoloProfile(
            listOf(
                dpad(112, 109, 76f),
                button("X", 166, 32, x),
                button("A", 226, 68, a),
                button("B", 226, 128, b),
                button("Y", 166, 185, y),
                button("L", 300, 38, l),
                button("R", 300, 98, r),
                hold("B", 300, 158),
                hold("Y", 102, 198),
            ) + rail() +
                listOf(
                    utility("Mic", if (melon) l2 else ls, "Mic"),
                    utility("Lid", if (melon) ls else l2, "Layout"),
                ),
        )
    }

    private fun threeDs(): PivotArcSoloProfile =
        PivotArcSoloProfile(
            listOf(
                dpad(47, 113),
                stick("AnalogL", 126, 112, ComposeTouchLayouts.MOTION_SOURCE_LEFT_STICK, 72f),
                button("X", 196, 38, x),
                button("A", 254, 76, a),
                button("B", 254, 134, b),
                button("Y", 196, 184, y),
                button("L", 319, 38, l),
                button("R", 319, 98, r),
                hold("B", 319, 158),
                hold("Y", 130, 201),
            ) + rail(),
        )

    private fun genesis(six: Boolean): PivotArcSoloProfile {
        val seats =
            if (six) {
                listOf(
                    dpad(140, 172, 72f),
                    button("X", 61, 84, l),
                    button("Y", 137, 48, x),
                    button("Z", 215, 72, r),
                    button("A", 257, 140, y),
                    button("B", 242, 222, b),
                    button("C", 177, 285, a),
                    hold("B", 310, 222),
                )
            } else {
                listOf(
                    dpad(136, 153, 76f),
                    button("A", 217, 103, y),
                    button("B", 212, 213, b),
                    button("C", 159, 253, a),
                    hold("B", 274, 213),
                )
            }
        // Standard already emits Select in both modes; retain it (Mode for six-button).
        return PivotArcSoloProfile(seats + rail(selectLabel = if (six) "Mode" else "Select"))
    }

    private fun n64(): PivotArcSoloProfile =
        PivotArcSoloProfile(
            listOf(
                dpad(62, 87),
                stick("AnalogL", 139, 171, ComposeTouchLayouts.MOTION_SOURCE_LEFT_STICK),
                dpad(
                    266,
                    92,
                    80f,
                    ComposeTouchLayouts.MOTION_SOURCE_RIGHT_STICK,
                    id = "CDirection",
                    diagonals = false,
                ),
                button("L", 119, 33, l),
                button("R", 184, 33, r),
                button("Z", 227, 176, l2),
                button("B", 237, 246, y),
                button("A", 159, 281, b),
                hold("Z", 302, 176),
                hold("B", 312, 246),
                hold("A", 159, 339),
            ) + rail(select = false),
        )

    private fun dreamcast(): PivotArcSoloProfile =
        PivotArcSoloProfile(
            listOf(
                stick("AnalogL", 38, 160, ComposeTouchLayouts.MOTION_SOURCE_LEFT_STICK),
                dpad(115, 160, 72f),
                button("X", 174, 70, y),
                button("Y", 230, 110, x),
                button("A", 230, 230, b),
                button("B", 174, 275, a),
                button("L", 284, 150, l2),
                button("R", 338, 150, r2),
                hold("L", 284, 205),
                hold("R", 338, 205),
                hold("A", 300, 260),
                hold("B", 174, 335),
            ) + rail(select = false),
        )

    private fun atari7800(): PivotArcSoloProfile =
        PivotArcSoloProfile(
            listOf(
                dpad(85, 100, 88f),
                button("B", 120, 225, b, "1"),
                button("A", 190, 170, a, "2"),
                hold("B", 120, 281),
                hold("A", 253, 170),
            ) + rail(),
        )

    private fun sidewaysWii(): PivotArcSoloProfile =
        PivotArcSoloProfile(
            listOf(
                dpad(110, 157, 80f),
                button("X", 170, 62, x, "A"),
                button("Y", 235, 104, y, "B"),
                button("A", 235, 214, a, "2"),
                button("B", 170, 261, b, "1"),
                button("Shake", 235, 159, r2, "Shake"),
                hold("A", 302, 214),
                hold("B", 170, 320),
            ) + rail(selectLabel = "−", startLabel = "+") + utility("Home", rs, "Home"),
        )

    private fun gameCube(): PivotArcSoloProfile =
        PivotArcSoloProfile(
            listOf(
                stick("AnalogL", 38, 140, ComposeTouchLayouts.MOTION_SOURCE_LEFT_STICK),
                stick("AnalogR", 38, 263, ComposeTouchLayouts.MOTION_SOURCE_RIGHT_STICK, 64f),
                dpad(115, 169),
                button("Y", 140, 55, y),
                button("L", 200, 105, l),
                button("R", 255, 105, r),
                button("Z", 310, 105, r2),
                button("A", 200, 169, a),
                button("X", 255, 169, x),
                button("B", 200, 235, b),
                hold("L", 200, 45),
                hold("R", 255, 45),
                hold("A", 255, 235),
                hold("B", 200, 295),
                // Dolphin GameCube's existing stick-click IDs are half triggers, not L3/R3.
                button("HalfL", 310, 169, ls, "L½"),
                button("HalfR", 310, 235, rs, "R½"),
            ) + rail(select = false),
        )

    private fun arcade(six: Boolean): PivotArcSoloProfile {
        val seats =
            if (six) {
                listOf(
                    dpad(140, 172, 72f, ComposeTouchLayouts.MOTION_SOURCE_DPAD_AND_LEFT_STICK),
                    button("X", 61, 84, x, "4"),
                    button("Y", 137, 48, y, "5"),
                    button("Z", 215, 72, r, "6"),
                    button("A", 257, 140, a, "1"),
                    button("B", 242, 222, b, "2"),
                    button("C", 177, 285, l, "3"),
                    hold("B", 310, 222),
                )
            } else {
                listOf(
                    dpad(136, 153, 72f, ComposeTouchLayouts.MOTION_SOURCE_DPAD_AND_LEFT_STICK),
                    button("X", 164, 64, x, "3"),
                    button("A", 217, 103, a, "4"),
                    button("B", 212, 213, b, "2"),
                    button("Y", 159, 253, y, "1"),
                    hold("X", 212, 64),
                    hold("A", 264, 103),
                    hold("B", 264, 213),
                    hold("Y", 159, 313),
                )
            }
        // Numbers identify physical seats; each core/game retains its existing RetroPad mapping.
        return PivotArcSoloProfile(seats + rail(selectLabel = "Coin"))
    }

    private fun atari2600(): PivotArcSoloProfile =
        PivotArcSoloProfile(
            listOf(
                dpad(85, 100, 88f),
                button("B", 190, 170, b, "Fire"),
                hold("B", 253, 170),
            ) + rail(selectLabel = "Select", startLabel = "Reset") +
                listOf(
                    utility("DifficultyA", l, "Diff A"),
                    utility("DifficultyB", l2, "Diff B"),
                ),
        )

    private fun classicWii(): PivotArcSoloProfile =
        PivotArcSoloProfile(
            sony(true).seats.filterNot { it.id in setOf("LS", "RS") }.map { seat ->
                when (seat.id) {
                    "Y" -> seat.copy(label = "X", keyCode = x)
                    "X" -> seat.copy(label = "A", keyCode = a)
                    "B" -> seat.copy(label = "B", keyCode = b)
                    "A" -> seat.copy(label = "Y", keyCode = y)
                    "L" -> seat.copy(label = "ZL")
                    "R" -> seat.copy(label = "ZR")
                    "L2" -> seat.copy(label = "L")
                    "R2" -> seat.copy(label = "R")
                    "Start" -> seat.copy(label = "+")
                    "Select" -> seat.copy(label = "−")
                    else -> seat
                }
            },
        )

    private fun nunchukWii(): PivotArcSoloProfile =
        PivotArcSoloProfile(
            listOf(
                stick("AnalogL", 36, 124, ComposeTouchLayouts.MOTION_SOURCE_LEFT_STICK, 64f),
                stick("AnalogR", 36, 248, ComposeTouchLayouts.MOTION_SOURCE_RIGHT_STICK, 64f)
                    .copy(label = "IR pointer"),
                dpad(110, 185, 72f),
                button("X", 164, 84, x, "C"),
                button("B", 224, 120, b, "B"),
                button("A", 224, 272, a, "A"),
                button("Y", 164, 318, y, "Z"),
                hold("B", 300, 110),
                hold("A", 300, 300),
                hold("Y", 164, 376),
                button("Shake", 278, 197, r2, "Shake"),
                button("ShakeN", 332, 197, l2, "Shake N"),
            ) + rail() +
                listOf(
                    utility("Minus", l, "−"),
                    utility("Plus", r, "+"),
                ),
        )

    /** Native host mode uses the same Android button codes as XboxPadState. */
    fun forNativeXbox(controller: TouchControllerID): PivotArcSoloProfile? {
        if (controller != TouchControllerID.XBOX && controller != TouchControllerID.XBOX360) return null
        val bumpers = controller == TouchControllerID.XBOX360
        return PivotArcSoloProfile(
            sony(true).seats.map { seat ->
                when (seat.id) {
                    "Y" -> seat.copy(label = "X", keyCode = x)
                    "X" -> seat.copy(label = "Y", keyCode = y)
                    "B" -> seat.copy(label = "A", keyCode = a)
                    "A" -> seat.copy(label = "B", keyCode = b)
                    "L" -> seat.copy(label = if (bumpers) "LB" else "White")
                    "R" -> seat.copy(label = if (bumpers) "RB" else "Black")
                    "L2" -> seat.copy(label = "LT")
                    "R2" -> seat.copy(label = "RT")
                    "LS" -> seat.copy(label = "LS")
                    "RS" -> seat.copy(label = "RS")
                    "Select" -> seat.copy(label = "Back")
                    else -> seat
                }
            },
        )
    }

    fun supportsSystem(system: SystemID): Boolean =
        system in
            setOf(
                SystemID.SNES, SystemID.NDS, SystemID.NINTENDO_3DS,
                SystemID.GENESIS, SystemID.SEGACD, SystemID.N64,
                SystemID.PSX, SystemID.PLAYSTATION_2, SystemID.DREAMCAST,
                SystemID.ATARI7800, SystemID.ATARI2600, SystemID.WII, SystemID.DOS,
                SystemID.GAMECUBE, SystemID.FBNEO, SystemID.MAME2003PLUS,
                SystemID.XBOX, SystemID.XBOX360,
            )

    fun forMode(
        system: SystemID,
        controller: TouchControllerID?,
    ): PivotArcSoloProfile? =
        when (system) {
            SystemID.SNES -> if (controller == TouchControllerID.SNES) snes() else null
            SystemID.NDS ->
                if (controller == TouchControllerID.DESMUME || controller == TouchControllerID.MELONDS) {
                    ds(controller)
                } else {
                    null
                }
            SystemID.NINTENDO_3DS -> if (controller == TouchControllerID.NINTENDO_3DS) threeDs() else null
            SystemID.GENESIS, SystemID.SEGACD ->
                when (controller) {
                    TouchControllerID.GENESIS_3 -> genesis(false)
                    TouchControllerID.GENESIS_6 -> genesis(true)
                    else -> null
                }
            SystemID.N64 -> if (controller == TouchControllerID.N64) n64() else null
            SystemID.PSX, SystemID.PLAYSTATION_2 ->
                when (controller) {
                    TouchControllerID.PSX -> sony(false)
                    TouchControllerID.PSX_DUALSHOCK -> sony(true)
                    else -> null
                }
            SystemID.DREAMCAST -> if (controller == TouchControllerID.DREAMCAST) dreamcast() else null
            SystemID.ATARI7800 -> if (controller == TouchControllerID.ATARI7800) atari7800() else null
            SystemID.ATARI2600 -> if (controller == TouchControllerID.ATARI2600) atari2600() else null
            SystemID.GAMECUBE -> if (controller == TouchControllerID.GAMECUBE) gameCube() else null
            SystemID.FBNEO, SystemID.MAME2003PLUS ->
                when (controller) {
                    TouchControllerID.ARCADE_4 -> arcade(false)
                    TouchControllerID.ARCADE_6 -> arcade(true)
                    else -> null
                }
            SystemID.WII ->
                when (controller) {
                    TouchControllerID.WII_SIDEWAYS -> sidewaysWii()
                    TouchControllerID.WII_CLASSIC -> classicWii()
                    TouchControllerID.WII_NUNCHUK -> nunchukWii()
                    TouchControllerID.GAMECUBE -> gameCube()
                    else -> null
                }
            SystemID.DOS -> if (controller == TouchControllerID.DOS) sony(true, dos = true) else null
            else -> null
        }
}

/** Uniform centre scaling and whole-body placement. Never scale hit targets down. */
object PivotArcSoloSolver {
    private const val GAP = 4f
    private const val RIM = 8f
    private val scales = listOf(1.5f, 1.4f, 1.3f, 1.2f, 1.1f, 1f, 0.95f, 0.9f, 0.85f, 0.8f)

    fun resolve(
        profile: PivotArcSoloProfile,
        safe: PivotArcBounds,
        viewport: PivotArcBounds,
        leftHanded: Boolean,
        requestedDx: Float,
        requestedDy: Float,
        landscape: Boolean,
    ): PivotArcResolution {
        if (landscape || !requestedDx.isFinite() || !requestedDy.isFinite()) {
            return PivotArcResolution.Unavailable("PivotArc requires a valid portrait layout.")
        }
        val available =
            PivotArcBounds(
                safe.left + RIM,
                max(safe.top, viewport.bottom + GAP) + RIM,
                safe.right - RIM,
                safe.bottom - RIM,
            )
        if (available.width <= 0f || available.height <= 0f) {
            return PivotArcResolution.Unavailable("No safe space below the game screen.")
        }
        val game = profile.seats.filterNot { it.utility }
        val utility = profile.seats.filter { it.utility }
        for (scale in scales) {
            val core =
                game.associate { seat ->
                    val half = seat.target / 2f
                    val cx = seat.x * scale
                    val cy = seat.y * scale
                    seat.id to PivotArcBounds(cx - half, cy - half, cx + half, cy + half)
                }
            val coreHull = hull(core.values)
            val railWidth =
                utility.sumOf { it.target.toDouble() }.toFloat() +
                    (utility.size - 1).coerceAtLeast(0) * GAP
            var cursor = coreHull.centerX - railWidth / 2f
            val railTop = coreHull.bottom + GAP + RIM
            val rail =
                utility.associate { seat ->
                    val rect = PivotArcBounds(cursor, railTop, cursor + seat.target, railTop + seat.target)
                    cursor += seat.target + GAP
                    seat.id to rect
                }
            val all = core + rail
            val bounds = hull(all.values)
            if (bounds.width > available.width || bounds.height > available.height) continue
            val rectangles = all.values.toList()
            if (rectangles.indices.any { i ->
                    (i + 1 until rectangles.size).any { j ->
                        rectangles[i].inset(GAP / 2f - 0.01f).intersects(rectangles[j].inset(GAP / 2f - 0.01f))
                    }
                }
            ) {
                continue
            }
            val dx =
                (available.right - bounds.right + requestedDx)
                    .coerceIn(available.left - bounds.left, available.right - bounds.right)
            val dy =
                (available.bottom - bounds.bottom + requestedDy)
                    .coerceIn(available.top - bounds.top, available.bottom - bounds.bottom)
            val placed =
                all.mapValues { (_, rect) ->
                    val moved = PivotArcBounds(rect.left + dx, rect.top + dy, rect.right + dx, rect.bottom + dy)
                    if (leftHanded) {
                        PivotArcBounds(
                            safe.left + safe.right - moved.right,
                            moved.top,
                            safe.left + safe.right - moved.left,
                            moved.bottom,
                        )
                    } else {
                        moved
                    }
                }
            if (placed.values.any {
                    it.width < 48f || it.height < 48f || !it.inside(available) || it.intersects(viewport.inset(GAP))
                }
            ) {
                continue
            }
            return PivotArcResolution.Fits(
                PivotArcPlacement(
                    controls = placed,
                    visuals = profile.seats.associate { it.id to PivotArcSize(it.cap, it.cap) },
                    appliedDx = dx,
                    appliedDy = dy,
                    visualScale = scale,
                ),
            )
        }
        return PivotArcResolution.Unavailable(
            "The approved PivotArc constellation cannot fit this screen with independent 48dp targets. " +
                "Standard remains available.",
        )
    }

    private fun hull(rectangles: Collection<PivotArcBounds>): PivotArcBounds =
        PivotArcBounds(
            rectangles.minOf { it.left },
            rectangles.minOf { it.top },
            rectangles.maxOf { it.right },
            rectangles.maxOf { it.bottom },
        )
}
