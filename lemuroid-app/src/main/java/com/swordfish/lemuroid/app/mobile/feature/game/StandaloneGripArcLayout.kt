package com.swordfish.lemuroid.app.mobile.feature.game

import android.view.KeyEvent
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.touchinput.radial.settings.NativeControlIcon
import com.swordfish.touchinput.radial.settings.NativeControlInventory
import com.swordfish.touchinput.radial.settings.NativeControlKind
import com.swordfish.touchinput.radial.settings.NativeControlSpec
import com.swordfish.touchinput.radial.settings.TouchControllerID

internal object StandaloneGripArcLayout {
    private data class Seat(val x: Int, val y: Int, val size: Float = 48.5f)

    private val portraitNative =
        mapOf(
            "L" to Seat(55, 60),
            "R" to Seat(305, 60),
            "D" to Seat(86, 155, 80f),
            "B" to Seat(210, 185),
            "A" to Seat(278, 129),
            "Select" to Seat(80, 330),
            "Menu" to Seat(180, 330),
            "Start" to Seat(280, 330),
        )
    private val portraitProgram =
        mapOf(1 to Seat(180, 60), 2 to Seat(318, 195), 3 to Seat(236, 252), 4 to Seat(147, 260))
    private val landscapeNative =
        mapOf(
            "L" to Seat(84, 65),
            "D" to Seat(82, 159, 80f),
            "Select" to Seat(82, 322),
            "R" to Seat(650, 65),
            "A" to Seat(681, 143),
            "B" to Seat(612, 202),
            "Start" to Seat(674, 322),
            "Menu" to Seat(365, 365),
        )
    private val landscapeProgram =
        mapOf(1 to Seat(44, 253), 2 to Seat(684, 220), 3 to Seat(613, 278), 4 to Seat(114, 253))

    private val digitalTwoPortrait = portraitNative - setOf("L", "R")
    private val digitalTwoLandscape = landscapeNative - setOf("L", "R")
    private val segaPortrait =
        digitalTwoPortrait - "Select" + mapOf("Menu" to Seat(115, 330), "Start" to Seat(245, 330))
    private val segaLandscape = digitalTwoLandscape - "Select"
    private val digitalTwoProgramPortrait = portraitProgram + (1 to Seat(210, 60))
    private val digitalTwoProgramLandscape = landscapeProgram + (1 to Seat(650, 65))

    private val snesPortrait =
        mapOf(
            "L" to Seat(55, 60), "R" to Seat(305, 60), "D" to Seat(77, 163, 80f),
            "X" to Seat(250, 110), "A" to Seat(309, 166), "B" to Seat(250, 222), "Y" to Seat(191, 166),
            "Select" to Seat(80, 350), "Menu" to Seat(180, 350), "Start" to Seat(280, 350),
        )
    private val snesLandscape =
        mapOf(
            "L" to Seat(82, 47), "D" to Seat(82, 145, 80f), "Select" to Seat(114, 318),
            "R" to Seat(648, 47), "X" to Seat(648, 108), "A" to Seat(687, 169), "Y" to Seat(609, 169),
            "B" to Seat(648, 230), "Start" to Seat(648, 347), "Menu" to Seat(365, 365),
        )
    private val snesProgramPortrait =
        mapOf(1 to Seat(180, 60), 2 to Seat(77, 268), 3 to Seat(250, 293), 4 to Seat(148, 268))
    private val snesProgramLandscape =
        mapOf(1 to Seat(44, 290), 2 to Seat(44, 228), 3 to Seat(648, 289), 4 to Seat(114, 228))

    private val megaDriveThreePortrait =
        mapOf(
            "D" to Seat(80, 155, 80f),
            "A" to Seat(192, 170),
            "B" to Seat(252, 150),
            "C" to Seat(312, 130),
            "Menu" to Seat(115, 330),
            "Start" to Seat(245, 330),
        )
    private val megaDriveThreeLandscape =
        mapOf(
            "D" to Seat(82, 145, 80f),
            "A" to Seat(612, 210),
            "B" to Seat(650, 145),
            "C" to Seat(687, 80),
            "Start" to Seat(650, 343),
            "Menu" to Seat(365, 365),
        )
    private val megaDriveSixPortrait =
        mapOf(
            "D" to Seat(80, 155, 80f),
            "X" to Seat(200, 113), "Y" to Seat(258, 113), "Z" to Seat(316, 113),
            "A" to Seat(200, 180), "B" to Seat(258, 180), "C" to Seat(316, 180),
            "Select" to Seat(80, 330), "Menu" to Seat(180, 330), "Start" to Seat(280, 330),
        )
    private val megaDriveSixLandscape =
        mapOf(
            "D" to Seat(82, 145, 80f), "Select" to Seat(82, 49),
            "X" to Seat(615, 72), "Y" to Seat(615, 140), "Z" to Seat(615, 208),
            "A" to Seat(682, 72), "B" to Seat(682, 140), "C" to Seat(682, 208),
            "Start" to Seat(650, 343), "Menu" to Seat(365, 365),
        )
    private val megaDriveProgramPortrait =
        mapOf(1 to Seat(80, 252), 2 to Seat(220, 252), 3 to Seat(290, 252), 4 to Seat(150, 252))
    private val megaDriveProgramLandscape =
        mapOf(1 to Seat(44, 239), 2 to Seat(615, 285), 3 to Seat(682, 285), 4 to Seat(114, 239))

    private val atari2600Portrait =
        mapOf(
            "D" to Seat(80, 145, 84f),
            "B" to Seat(274, 145, 60f),
            "DifficultyA" to Seat(58, 45),
            "DifficultyB" to Seat(130, 45),
            "Select" to Seat(80, 305),
            "Menu" to Seat(180, 305),
            "Start" to Seat(280, 305),
        )
    private val atari2600Landscape =
        mapOf(
            "D" to Seat(82, 150, 84f),
            "B" to Seat(650, 145, 60f),
            "DifficultyA" to Seat(46, 43),
            "DifficultyB" to Seat(116, 43),
            "Select" to Seat(82, 322),
            "Menu" to Seat(365, 365),
            "Start" to Seat(650, 343),
        )
    private val atari2600ProgramPortrait = mapOf(1 to Seat(210, 75), 2 to Seat(274, 235))
    private val atari2600ProgramLandscape = mapOf(1 to Seat(650, 65), 2 to Seat(650, 240))
    private val lynxPortrait =
        portraitNative - "Select" + mapOf("Menu" to Seat(115, 350), "Start" to Seat(245, 350))
    private val lynxLandscape =
        landscapeNative - "Select" + mapOf("L" to Seat(82, 49), "R" to Seat(650, 49))

    private val dreamcastPortrait =
        mapOf(
            "L" to Seat(55, 60), "R" to Seat(305, 60),
            "AnalogL" to Seat(79, 135, 74f), "D" to Seat(79, 238, 76f),
            "Y" to Seat(253, 111), "B" to Seat(309, 169), "A" to Seat(253, 227), "X" to Seat(197, 169),
            "Menu" to Seat(115, 370), "Start" to Seat(245, 370),
        )
    private val dreamcastLandscape =
        mapOf(
            "L" to Seat(46, 43), "R" to Seat(684, 43),
            "AnalogL" to Seat(82, 119, 74f), "D" to Seat(82, 222, 76f),
            "Y" to Seat(648, 106), "B" to Seat(687, 169), "A" to Seat(648, 231), "X" to Seat(609, 169),
            "Start" to Seat(648, 348), "Menu" to Seat(365, 365),
        )
    private val dreamcastProgramPortrait =
        mapOf(
            1 to Seat(130, 60),
            2 to Seat(230, 60),
            3 to Seat(220, 305),
            4 to Seat(300, 305),
        )
    private val dreamcastProgramLandscape =
        mapOf(
            1 to Seat(116, 43),
            2 to Seat(614, 43),
            3 to Seat(615, 289),
            4 to Seat(682, 289),
        )

    private val gameCubePortrait =
        mapOf(
            "L" to Seat(48, 35), "HalfL" to Seat(124, 35), "HalfR" to Seat(236, 35), "R" to Seat(312, 35),
            "Z" to Seat(236, 91), "AnalogL" to Seat(78, 177, 78f), "D" to Seat(78, 292, 74f),
            "A" to Seat(255, 205), "B" to Seat(192, 242), "Y" to Seat(227, 140), "X" to Seat(313, 164),
            "AnalogR" to Seat(267, 310, 66f), "Menu" to Seat(115, 430), "Start" to Seat(245, 430),
        )
    private val gameCubeLandscape =
        mapOf(
            "L" to Seat(46, 32), "HalfL" to Seat(116, 32), "HalfR" to Seat(614, 32), "R" to Seat(684, 32),
            "Z" to Seat(614, 90), "AnalogL" to Seat(82, 170, 78f), "D" to Seat(82, 272, 70f),
            "A" to Seat(648, 209), "B" to Seat(610, 268), "Y" to Seat(614, 150), "X" to Seat(684, 150),
            "AnalogR" to Seat(650, 338, 60f), "Menu" to Seat(365, 365), "Start" to Seat(465, 365),
        )
    private val gameCubeProgramPortrait =
        mapOf(
            1 to Seat(48, 91),
            2 to Seat(312, 91),
            3 to Seat(310, 385),
            4 to Seat(150, 370),
        )
    private val gameCubeProgramLandscape =
        mapOf(
            1 to Seat(46, 90),
            2 to Seat(684, 90),
            3 to Seat(693, 268),
            4 to Seat(82, 342),
        )

    private val pspPortrait =
        dreamcastPortrait +
            mapOf(
                "D" to Seat(79, 135, 74f), "AnalogL" to Seat(79, 238, 74f),
                "X" to Seat(253, 111), "A" to Seat(309, 169), "B" to Seat(253, 227), "Y" to Seat(197, 169),
                "Select" to Seat(80, 370), "Menu" to Seat(180, 370), "Start" to Seat(280, 370),
            )
    private val pspLandscape =
        dreamcastLandscape +
            mapOf(
                "L" to Seat(49, 43), "R" to Seat(648, 49),
                "D" to Seat(82, 119, 74f), "AnalogL" to Seat(82, 222, 74f),
                "X" to Seat(648, 107), "A" to Seat(687, 169), "B" to Seat(648, 231), "Y" to Seat(609, 169),
                "Select" to Seat(82, 359), "Start" to Seat(648, 348),
            )
    private val pspProgramPortrait =
        mapOf(1 to Seat(180, 60), 2 to Seat(79, 322), 3 to Seat(253, 297), 4 to Seat(151, 302))
    private val pspProgramLandscape =
        mapOf(1 to Seat(121, 43), 2 to Seat(44, 300), 3 to Seat(648, 289), 4 to Seat(114, 300))

    private val playStationPortrait =
        mapOf(
            "L2" to Seat(55, 35), "L" to Seat(55, 88), "R2" to Seat(305, 35), "R" to Seat(305, 88),
            "D" to Seat(78, 174, 86f),
            "X" to Seat(253, 135), "A" to Seat(310, 188), "B" to Seat(253, 241), "Y" to Seat(196, 188),
            "Select" to Seat(80, 347), "Menu" to Seat(180, 347), "Start" to Seat(280, 347),
        )
    private val playStationLandscape =
        mapOf(
            "L2" to Seat(46, 32), "L" to Seat(116, 32), "R" to Seat(614, 32), "R2" to Seat(684, 32),
            "D" to Seat(82, 118, 86f),
            "X" to Seat(648, 92), "A" to Seat(689, 150), "B" to Seat(648, 208), "Y" to Seat(607, 150),
            "Select" to Seat(265, 365), "Menu" to Seat(365, 365), "Start" to Seat(465, 365),
        )
    private val dualShockPortrait =
        playStationPortrait +
            mapOf(
                "AnalogL" to Seat(78, 293, 76f), "AnalogR" to Seat(266, 310, 70f),
                "Select" to Seat(80, 430), "Menu" to Seat(180, 430), "Start" to Seat(280, 430),
            )
    private val dualShockLandscape =
        playStationLandscape + mapOf("AnalogL" to Seat(82, 242, 74f), "AnalogR" to Seat(648, 275, 62f))

    private fun playStationProgram(
        landscape: Boolean,
        dualShock: Boolean,
    ): Map<Int, Seat> =
        if (landscape) {
            val y = if (dualShock) 349 else 270
            mapOf(1 to Seat(45, y), 2 to Seat(115, y), 3 to Seat(615, y), 4 to Seat(684, y))
        } else {
            val y = if (dualShock) 380 else 294
            mapOf(1 to Seat(45, y), 2 to Seat(135, y), 3 to Seat(225, y), 4 to Seat(315, y))
        }

    private val threeDsPortrait =
        mapOf(
            "L" to Seat(65, 28), "R" to Seat(295, 28),
            "AnalogL" to Seat(77, 92, 78f), "D" to Seat(77, 190, 78f),
            "X" to Seat(267, 79), "A" to Seat(316, 129), "B" to Seat(267, 179), "Y" to Seat(218, 129),
            "Select" to Seat(64, 285), "Menu" to Seat(180, 285), "Start" to Seat(296, 285),
        )
    private val threeDsLandscape =
        mapOf(
            "L" to Seat(82, 40), "R" to Seat(648, 49),
            "AnalogL" to Seat(82, 119, 78f), "D" to Seat(82, 222, 78f),
            "X" to Seat(648, 109), "A" to Seat(689, 167), "B" to Seat(648, 225), "Y" to Seat(607, 167),
            "Select" to Seat(82, 360), "Menu" to Seat(365, 365), "Start" to Seat(648, 335),
        )
    private val threeDsProgramPortrait = mapOf(1 to Seat(189, 234), 2 to Seat(270, 234))
    private val threeDsProgramLandscape = mapOf(1 to Seat(44, 300), 2 to Seat(114, 300))

    private val controllers =
        setOf(
            TouchControllerID.GBA,
            TouchControllerID.GB,
            TouchControllerID.NES,
            TouchControllerID.SMS,
            TouchControllerID.GG,
            TouchControllerID.NGP,
            TouchControllerID.PCE,
            TouchControllerID.SNES,
            TouchControllerID.GENESIS_3,
            TouchControllerID.GENESIS_6,
            TouchControllerID.ATARI2600,
            TouchControllerID.ATARI7800,
            TouchControllerID.LYNX,
            TouchControllerID.DREAMCAST,
            TouchControllerID.PSP,
            TouchControllerID.GAMECUBE,
            TouchControllerID.PSX,
            TouchControllerID.PSX_DUALSHOCK,
            TouchControllerID.NINTENDO_3DS,
        )

    fun supports(controller: TouchControllerID?): Boolean = controller in controllers

    fun inventoryFor(controller: TouchControllerID): NativeControlInventory? =
        when (controller) {
            TouchControllerID.NES ->
                NativeControlInventory.forController(
                    TouchControllerID.GB,
                )?.copy(controller = controller)
            TouchControllerID.SMS, TouchControllerID.PCE -> {
                val base = if (controller == TouchControllerID.PCE) TouchControllerID.GB else TouchControllerID.GG
                NativeControlInventory.forController(base)?.let {
                    it.copy(
                        controller = controller,
                        controls =
                            it.controls.map { seat ->
                                when {
                                    seat.key == "Start" ->
                                        seat.copy(
                                            label = if (controller == TouchControllerID.PCE) "Run" else "Pause",
                                            icon = null,
                                        )
                                    controller == TouchControllerID.PCE && seat.key == "A" -> seat.copy(label = "II")
                                    controller == TouchControllerID.PCE && seat.key == "B" -> seat.copy(label = "I")
                                    else -> seat
                                }
                            },
                    )
                }
            }
            TouchControllerID.NGP ->
                NativeControlInventory.forController(controller)?.let {
                    it.copy(
                        controls =
                            it.controls.map { seat ->
                                if (seat.key == "Start") seat.copy(label = "Option", icon = null) else seat
                            },
                    )
                }
            TouchControllerID.GENESIS_3, TouchControllerID.GENESIS_6 -> megaDriveInventory(controller)
            TouchControllerID.ATARI2600, TouchControllerID.ATARI7800 -> atariInventory(controller)
            TouchControllerID.DREAMCAST -> analogueInventory(SystemID.DREAMCAST, controller)
            TouchControllerID.GAMECUBE -> analogueInventory(SystemID.GAMECUBE, controller)
            TouchControllerID.PSP, TouchControllerID.PSX_DUALSHOCK, TouchControllerID.NINTENDO_3DS -> {
                NativeControlInventory.forHoldTray(controller)?.let { inventory ->
                    inventory.copy(
                        controls =
                            inventory.controls.map { seat ->
                                if (seat.kind == NativeControlKind.ANALOG) seat.copy(pressCode = null) else seat
                            },
                    )
                }
            }
            else -> NativeControlInventory.forController(controller)
        }

    fun actionLabels(native: NativeControlInventory): Map<Int, String> =
        native.controls.filterNot { it.key == "Menu" }.mapNotNull { seat ->
            val label =
                when (seat.icon) {
                    NativeControlIcon.CIRCLE -> "Circle"
                    NativeControlIcon.CROSS -> "Cross"
                    NativeControlIcon.SQUARE -> "Square"
                    NativeControlIcon.TRIANGLE -> "Triangle"
                    else -> seat.label
                }
            seat.keyCode?.let { it to label }
        }.toMap()

    private fun megaDriveInventory(controller: TouchControllerID): NativeControlInventory {
        val six = controller == TouchControllerID.GENESIS_6
        val common = checkNotNull(NativeControlInventory.forController(TouchControllerID.GB)).controls
        val utility =
            common.filter { it.key in setOf("D", "Menu", "Start") } +
                if (six) listOf(common.first { it.key == "Select" }.copy(label = "Mode", icon = null)) else emptyList()
        val faceKeys =
            mapOf(
                "A" to KeyEvent.KEYCODE_BUTTON_Y,
                "B" to KeyEvent.KEYCODE_BUTTON_B,
                "C" to KeyEvent.KEYCODE_BUTTON_A,
            )
        // Genesis Plus GX only reads these four additional inputs in DEVICE_PAD6B.
        val extraKeys =
            if (six) {
                mapOf(
                    "X" to KeyEvent.KEYCODE_BUTTON_L1,
                    "Y" to KeyEvent.KEYCODE_BUTTON_X,
                    "Z" to KeyEvent.KEYCODE_BUTTON_R1,
                )
            } else {
                emptyMap()
            }
        val faces =
            (faceKeys + extraKeys).map { (key, value) ->
                NativeControlSpec(key, "face-${key.lowercase()}", NativeControlKind.FACE, keyCode = value)
            }
        return NativeControlInventory(controller, utility + faces)
    }

    private fun atariInventory(controller: TouchControllerID): NativeControlInventory {
        val single = controller == TouchControllerID.ATARI2600
        val digital = checkNotNull(NativeControlInventory.forController(TouchControllerID.GB)).controls
        val controls = if (single) digital.filterNot { it.key == "A" } else digital
        val labelled =
            controls.map {
                when (it.key) {
                    "A" -> it.copy(label = "2")
                    "B" -> it.copy(label = if (single) "Fire" else "1")
                    "Start" -> if (single) it.copy(label = "Reset", icon = null) else it
                    else -> it
                }
            }
        val switches =
            if (single) {
                listOf(
                    NativeControlSpec(
                        "DifficultyA",
                        "difficulty-a",
                        NativeControlKind.SHOULDER,
                        keyCode = KeyEvent.KEYCODE_BUTTON_L1,
                        label = "Diff A",
                    ),
                    NativeControlSpec(
                        "DifficultyB",
                        "difficulty-b",
                        NativeControlKind.SHOULDER,
                        keyCode = KeyEvent.KEYCODE_BUTTON_L2,
                        label = "Diff B",
                    ),
                )
            } else {
                emptyList()
            }
        return NativeControlInventory(controller, labelled + switches)
    }

    private fun analogueInventory(
        system: SystemID,
        controller: TouchControllerID,
    ): NativeControlInventory {
        val profile = checkNotNull(PivotArcSoloProfiles.forMode(system, controller))
        val controls =
            profile.seats.filter { it.holdOf == null }.map { seat ->
                val kind =
                    when {
                        seat.analog -> NativeControlKind.ANALOG
                        seat.direction != null -> NativeControlKind.DPAD
                        seat.utility -> NativeControlKind.UTILITY
                        seat.id in setOf("L", "R", "HalfL", "HalfR") -> NativeControlKind.SHOULDER
                        else -> NativeControlKind.FACE
                    }
                val label = if (system == SystemID.GAMECUBE && seat.id == "AnalogR") "C-Stick" else seat.label
                NativeControlSpec(
                    seat.id,
                    seat.id,
                    kind,
                    keyCode = seat.keyCode,
                    motionSource = seat.direction,
                    label = label,
                )
            }
        return NativeControlInventory(controller, controls)
    }

    /** Each inherited digital family uses its approved native controls and separately authored orientations. */
    fun resolveStandalone(
        native: NativeControlInventory,
        safe: PivotArcBounds,
        viewport: PivotArcBounds,
        landscape: Boolean,
        pspAnalogPrimary: Boolean = false,
    ): GripArcCombinedPlacement? {
        if (!supports(native.controller) || !safe.isFiniteArea() || !viewport.isFiniteArea()) return null
        val panel =
            if (landscape) {
                safe
            } else {
                PivotArcBounds(
                    safe.left + 4f,
                    maxOf(safe.top, viewport.bottom + 16f),
                    safe.right - 4f,
                    safe.bottom - 12f,
                )
            }
        if (!panel.isFiniteArea()) return null
        val width = if (landscape) 730f else 360f
        val snes = native.controller == TouchControllerID.SNES
        val height =
            if (landscape) {
                390f
            } else if (native.controller in setOf(TouchControllerID.GAMECUBE, TouchControllerID.PSX_DUALSHOCK)) {
                460f
            } else if (native.controller in setOf(TouchControllerID.DREAMCAST, TouchControllerID.PSP)) {
                420f
            } else if (snes || native.controller == TouchControllerID.LYNX) {
                400f
            } else if (native.controller == TouchControllerID.NINTENDO_3DS) {
                320f
            } else if (native.controller == TouchControllerID.ATARI2600) {
                350f
            } else {
                380f
            }

        fun bounds(
            seat: Seat,
            menu: Boolean = false,
        ): PivotArcBounds {
            val radius = seat.size / 2f
            val x = panel.left + panel.width * seat.x / width
            val referenceY = panel.top + panel.height * seat.y / height
            val y =
                if (menu && landscape) {
                    maxOf(minOf(referenceY, panel.bottom - radius), viewport.bottom + radius + 2f)
                } else {
                    referenceY
                }
            return PivotArcBounds(x - radius, y - radius, x + radius, y + radius)
        }
        val gba = native.controller == TouchControllerID.GBA
        val noSelect = native.controller in setOf(TouchControllerID.SMS, TouchControllerID.GG, TouchControllerID.NGP)
        val authoredNativeSeats =
            when {
                gba -> if (landscape) landscapeNative else portraitNative
                snes -> if (landscape) snesLandscape else snesPortrait
                native.controller == TouchControllerID.NINTENDO_3DS ->
                    if (landscape) threeDsLandscape else threeDsPortrait
                native.controller == TouchControllerID.PSX ->
                    if (landscape) playStationLandscape else playStationPortrait
                native.controller == TouchControllerID.PSX_DUALSHOCK ->
                    if (landscape) dualShockLandscape else dualShockPortrait
                native.controller == TouchControllerID.PSP ->
                    if (landscape) pspLandscape else pspPortrait
                native.controller == TouchControllerID.DREAMCAST ->
                    if (landscape) dreamcastLandscape else dreamcastPortrait
                native.controller == TouchControllerID.GAMECUBE ->
                    if (landscape) gameCubeLandscape else gameCubePortrait
                native.controller == TouchControllerID.ATARI2600 ->
                    if (landscape) atari2600Landscape else atari2600Portrait
                native.controller == TouchControllerID.ATARI7800 ->
                    if (landscape) digitalTwoLandscape else digitalTwoPortrait + ("D" to Seat(80, 150, 84f))
                native.controller == TouchControllerID.LYNX ->
                    if (landscape) lynxLandscape else lynxPortrait
                native.controller == TouchControllerID.GENESIS_3 ->
                    if (landscape) megaDriveThreeLandscape else megaDriveThreePortrait
                native.controller == TouchControllerID.GENESIS_6 ->
                    if (landscape) megaDriveSixLandscape else megaDriveSixPortrait
                noSelect -> if (landscape) segaLandscape else segaPortrait
                else -> if (landscape) digitalTwoLandscape else digitalTwoPortrait
            }
        val nativeSeats =
            if (native.controller == TouchControllerID.PSP && pspAnalogPrimary) {
                authoredNativeSeats +
                    mapOf(
                        "D" to authoredNativeSeats.getValue("AnalogL"),
                        "AnalogL" to authoredNativeSeats.getValue("D"),
                    )
            } else {
                authoredNativeSeats
            }
        val programSeats =
            when {
                gba -> if (landscape) landscapeProgram else portraitProgram
                native.controller == TouchControllerID.NINTENDO_3DS ->
                    if (landscape) threeDsProgramLandscape else threeDsProgramPortrait
                native.controller in setOf(TouchControllerID.PSX, TouchControllerID.PSX_DUALSHOCK) ->
                    playStationProgram(landscape, native.controller == TouchControllerID.PSX_DUALSHOCK)
                native.controller == TouchControllerID.PSP -> if (landscape) pspProgramLandscape else pspProgramPortrait
                native.controller == TouchControllerID.DREAMCAST ->
                    if (landscape) dreamcastProgramLandscape else dreamcastProgramPortrait
                native.controller == TouchControllerID.GAMECUBE ->
                    if (landscape) gameCubeProgramLandscape else gameCubeProgramPortrait
                native.controller == TouchControllerID.ATARI2600 ->
                    if (landscape) atari2600ProgramLandscape else atari2600ProgramPortrait
                native.controller == TouchControllerID.LYNX -> if (landscape) landscapeProgram else portraitProgram
                native.controller in setOf(TouchControllerID.GENESIS_3, TouchControllerID.GENESIS_6) ->
                    if (landscape) megaDriveProgramLandscape else megaDriveProgramPortrait
                snes -> if (landscape) snesProgramLandscape else snesProgramPortrait
                else -> if (landscape) digitalTwoProgramLandscape else digitalTwoProgramPortrait
            }
        val nativeTargets =
            nativeSeats.mapValues { (key, seat) ->
                val centralUtility =
                    key == "Menu" ||
                        (
                            native.controller in setOf(TouchControllerID.PSX, TouchControllerID.PSX_DUALSHOCK) &&
                                key in setOf("Select", "Start")
                        ) ||
                        (native.controller == TouchControllerID.GAMECUBE && key == "Start")
                bounds(seat, menu = centralUtility)
            }
        if (nativeTargets.keys != native.controls.map { it.key }.toSet()) return null
        val programTargets = programSeats.mapKeys { (slot, _) -> "program:$slot" }.mapValues { bounds(it.value) }
        val fitted = growTargets(nativeTargets + programTargets, panel, viewport.inset(1.5f)) ?: return null
        val controls = nativeTargets.keys.associateWith(fitted::getValue)
        val programmable = programSeats.keys.associateWith { fitted.getValue("program:$it") }
        val visuals =
            native.controls.associate { control ->
                val target = controls.getValue(control.key)
                val size = target.width - 5f
                val height =
                    when (control.kind) {
                        NativeControlKind.SHOULDER, NativeControlKind.UTILITY -> size * 0.72f
                        else -> size
                    }
                control.key to PivotArcSize(size, height)
            }
        val seats =
            native.controls.groupBy { it.seat }.mapValues { (_, controlsInSeat) ->
                val rectangles = controlsInSeat.map { controls.getValue(it.key) }
                PivotArcBounds(
                    rectangles.minOf { it.left },
                    rectangles.minOf { it.top },
                    rectangles.maxOf { it.right },
                    rectangles.maxOf { it.bottom },
                )
            }
        val dock =
            GripArcPlacement(
                housing = panel,
                targets = programmable,
                visuals =
                    programmable.mapValues {
                            (_, target) ->
                        PivotArcSize(target.width - 5f, target.height * 0.82f)
                    },
                scale = 1f,
                side = GripArcSide.RIGHT,
            )
        return GripArcCombinedPlacement(dock, controls, visuals, seats, native, native.inputIds)
    }

    /** Expand every seat together before using the clearance left around an individual seat. */
    private fun growTargets(
        minimum: Map<String, PivotArcBounds>,
        panel: PivotArcBounds,
        display: PivotArcBounds,
    ): Map<String, PivotArcBounds>? {
        val clearance = 0.7f

        fun allowed(bounds: PivotArcBounds): Boolean = bounds.inside(panel) && !bounds.intersects(display)

        fun independent(targets: List<PivotArcBounds>): Boolean =
            targets.all(::allowed) &&
                targets.indices.all { index ->
                    targets.drop(index + 1).none { targets[index].inset(clearance).intersects(it.inset(clearance)) }
                }

        if (!independent(minimum.values.toList())) return null
        val limit = minOf(panel.width, panel.height) / 2f

        fun largestGrowth(fits: (Float) -> Boolean): Float {
            var low = 0f
            var high = limit
            repeat(12) {
                val mid = (low + high) / 2f
                if (fits(mid)) low = mid else high = mid
            }
            return low
        }

        val shared = largestGrowth { growth -> independent(minimum.values.map { it.inset(growth) }) }
        val fitted = minimum.mapValues { (_, target) -> target.inset(shared) }.toMutableMap()
        for ((key, target) in fitted.toMap().entries.sortedBy { it.value.width }) {
            val neighbours = fitted.filterKeys { it != key }.values.map { it.inset(clearance) }
            val growth =
                largestGrowth { growth ->
                    val candidate = target.inset(growth)
                    allowed(candidate) && neighbours.none { candidate.inset(clearance).intersects(it) }
                }
            fitted[key] = target.inset(growth)
        }
        return fitted
    }
}
