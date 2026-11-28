package com.swordfish.lemuroid.app.mobile.feature.game

import android.view.KeyEvent
import com.swordfish.lemuroid.app.shared.game.viewmodel.WonderSwanPivotInput
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.touchinput.radial.layouts.pceSixButtonLabels
import com.swordfish.touchinput.radial.settings.NativeControlIcon
import com.swordfish.touchinput.radial.settings.NativeControlInventory
import com.swordfish.touchinput.radial.settings.NativeControlKind
import com.swordfish.touchinput.radial.settings.NativeControlSpec
import com.swordfish.touchinput.radial.settings.TouchControllerID

internal object StandaloneGripArcLayout {
    const val GAMECUBE_LANDSCAPE_FIT = "authored GameCube landscape panels + utility rail"
    const val AUTHORED_LANDSCAPE_FIT = "authored landscape panels + utility rail"

    private data class Seat(
        val x: Int,
        val y: Int,
        val size: Float = 48.5f,
    )

    /**
     * C05-B: explicit authored analogue thumb-cap proportions per
     * console — the cap diameter as a fraction of the fitted stick
     * target. These replace the generic derived cap formula for
     * analogue controls, so each console's thumb caps are an authored
     * property of that console (the GameCube C-stick cap is smaller
     * than its main stick cap; the 3DS circle pad is nearly full).
     * Consoles without an entry use [DEFAULT_ANALOGUE_CAP_FRACTION].
     */
    private val analogueCapFractions =
        mapOf(
            TouchControllerID.GAMECUBE to mapOf("AnalogL" to 0.68f, "AnalogR" to 0.60f),
            TouchControllerID.PSX_DUALSHOCK to mapOf("AnalogL" to 0.66f, "AnalogR" to 0.66f),
            TouchControllerID.PSP to mapOf("AnalogL" to 0.62f),
            TouchControllerID.N64 to mapOf("AnalogL" to 0.60f),
            TouchControllerID.XBOX to mapOf("AnalogL" to 0.66f, "AnalogR" to 0.66f),
            TouchControllerID.XBOX360 to mapOf("AnalogL" to 0.66f, "AnalogR" to 0.66f),
            TouchControllerID.WII_CLASSIC to mapOf("AnalogL" to 0.64f, "AnalogR" to 0.64f),
            TouchControllerID.NINTENDO_3DS to mapOf("AnalogL" to 0.70f),
            TouchControllerID.DREAMCAST to mapOf("AnalogL" to 0.64f),
        )

    private const val DEFAULT_ANALOGUE_CAP_FRACTION = 0.65f

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

    private val wonderSwanPortrait =
        mapOf(
            "D" to Seat(80, 155, 96f),
            "Cursor2" to Seat(263, 96, 82f),
            "B" to Seat(292, 201),
            "A" to Seat(220, 231),
            "Menu" to Seat(115, 350),
            "Start" to Seat(245, 350),
        )
    private val wonderSwanLandscape =
        mapOf(
            "D" to Seat(82, 130, 96f),
            "Cursor2" to Seat(650, 84, 82f),
            "B" to Seat(680, 194),
            "A" to Seat(612, 223),
            "Start" to Seat(650, 335),
            "Menu" to Seat(365, 365),
        )
    private val wonderSwanProgramPortrait =
        mapOf(1 to Seat(80, 257), 2 to Seat(150, 294), 3 to Seat(294, 277), 4 to Seat(223, 294))
    private val wonderSwanProgramLandscape =
        mapOf(1 to Seat(44, 232), 2 to Seat(114, 232), 3 to Seat(680, 270), 4 to Seat(82, 298))

    private val digitalTwoPortrait = portraitNative - setOf("L", "R")
    private val digitalTwoLandscape = landscapeNative - setOf("L", "R")
    private val segaPortrait =
        digitalTwoPortrait - "Select" + mapOf("Menu" to Seat(115, 330), "Start" to Seat(245, 330))
    private val segaLandscape = digitalTwoLandscape - "Select"
    private val digitalTwoProgramPortrait = portraitProgram + (1 to Seat(210, 60))
    private val digitalTwoProgramLandscape = landscapeProgram + (1 to Seat(650, 65))

    private val snesPortrait =
        mapOf(
            "L" to Seat(55, 60),
            "R" to Seat(305, 60),
            "D" to Seat(77, 163, 80f),
            "X" to Seat(250, 110),
            "A" to Seat(309, 166),
            "B" to Seat(250, 222),
            "Y" to Seat(191, 166),
            "Select" to Seat(80, 350),
            "Menu" to Seat(180, 350),
            "Start" to Seat(280, 350),
        )
    private val snesLandscape =
        mapOf(
            "L" to Seat(82, 47),
            "D" to Seat(82, 145, 80f),
            "Select" to Seat(114, 318),
            "R" to Seat(648, 47),
            "X" to Seat(648, 108),
            "A" to Seat(687, 169),
            "Y" to Seat(609, 169),
            "B" to Seat(648, 230),
            "Start" to Seat(648, 347),
            "Menu" to Seat(365, 365),
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
            "X" to Seat(200, 113),
            "Y" to Seat(258, 113),
            "Z" to Seat(316, 113),
            "A" to Seat(200, 180),
            "B" to Seat(258, 180),
            "C" to Seat(316, 180),
            "Select" to Seat(80, 330),
            "Menu" to Seat(180, 330),
            "Start" to Seat(280, 330),
        )
    private val megaDriveSixLandscape =
        mapOf(
            "D" to Seat(82, 145, 80f),
            "Select" to Seat(82, 49),
            "X" to Seat(615, 72),
            "Y" to Seat(615, 140),
            "Z" to Seat(615, 208),
            "A" to Seat(682, 72),
            "B" to Seat(682, 140),
            "C" to Seat(682, 208),
            "Start" to Seat(650, 343),
            "Menu" to Seat(365, 365),
        )
    private val megaDriveProgramPortrait =
        mapOf(1 to Seat(80, 252), 2 to Seat(220, 252), 3 to Seat(290, 252), 4 to Seat(150, 252))
    private val megaDriveProgramLandscape =
        mapOf(1 to Seat(44, 239), 2 to Seat(615, 285), 3 to Seat(682, 285), 4 to Seat(114, 239))

    private val pceSixPortrait =
        mapOf(
            "D" to Seat(80, 155, 86f),
            "IV" to Seat(200, 113),
            "V" to Seat(258, 113),
            "VI" to Seat(316, 113),
            "I" to Seat(200, 180),
            "II" to Seat(258, 180),
            "III" to Seat(316, 180),
            "Select" to Seat(80, 330),
            "Menu" to Seat(180, 330),
            "Start" to Seat(280, 330),
        )
    private val pceSixLandscape =
        mapOf(
            "D" to Seat(82, 159, 86f),
            "IV" to Seat(615, 72),
            "V" to Seat(682, 72),
            "VI" to Seat(615, 140),
            "I" to Seat(682, 140),
            "II" to Seat(615, 208),
            "III" to Seat(682, 208),
            "Select" to Seat(82, 322),
            "Menu" to Seat(365, 365),
            "Start" to Seat(650, 335),
        )
    private val pceSixProgramPortrait =
        mapOf(1 to Seat(80, 252), 4 to Seat(150, 252), 3 to Seat(220, 252), 2 to Seat(290, 252))
    private val pceSixProgramLandscape =
        mapOf(1 to Seat(44, 253), 4 to Seat(114, 253), 3 to Seat(615, 276), 2 to Seat(682, 276))

    private val arcadeFourPortrait =
        mapOf(
            "D" to Seat(80, 150, 90f),
            "X" to Seat(220, 108),
            "A" to Seat(294, 96),
            "Y" to Seat(220, 182),
            "B" to Seat(294, 170),
            "Select" to Seat(80, 330),
            "Menu" to Seat(180, 330),
            "Start" to Seat(280, 330),
        )
    private val arcadeFourLandscape =
        mapOf(
            "D" to Seat(82, 145, 88f),
            "X" to Seat(615, 95),
            "A" to Seat(682, 95),
            "Y" to Seat(615, 175),
            "B" to Seat(682, 175),
            "Select" to Seat(82, 322),
            "Menu" to Seat(365, 365),
            "Start" to Seat(650, 335),
        )
    private val arcadeSixPortrait =
        megaDriveSixPortrait + ("D" to Seat(80, 150, 90f))
    private val arcadeSixLandscape =
        megaDriveSixLandscape +
            mapOf(
                "D" to Seat(82, 145, 88f),
                "Select" to Seat(82, 322),
                "Start" to Seat(650, 335),
            )
    private val arcadeProgramPortrait =
        mapOf(1 to Seat(80, 252), 2 to Seat(150, 252), 3 to Seat(220, 252), 4 to Seat(290, 252))
    private val arcadeProgramLandscape =
        mapOf(1 to Seat(44, 239), 2 to Seat(114, 239), 3 to Seat(615, 276), 4 to Seat(682, 276))

    // C02-B: two/three-button cabinets keep the four-button arrangement's
    // authored positions for the seats they actually have (seat 1 = Y,
    // 2 = B, 3 = X in the standalone inventory).
    private val arcadeTwoPortrait =
        arcadeFourPortrait.filterKeys { it in setOf("D", "Y", "B", "Select", "Menu", "Start") }
    private val arcadeTwoLandscape =
        arcadeFourLandscape.filterKeys { it in setOf("D", "Y", "B", "Select", "Menu", "Start") }
    private val arcadeThreePortrait =
        arcadeFourPortrait.filterKeys { it in setOf("D", "X", "Y", "B", "Select", "Menu", "Start") }
    private val arcadeThreeLandscape =
        arcadeFourLandscape.filterKeys { it in setOf("D", "X", "Y", "B", "Select", "Menu", "Start") }

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
            "L" to Seat(55, 60),
            "R" to Seat(305, 60),
            "AnalogL" to Seat(79, 135, 74f),
            "D" to Seat(79, 238, 76f),
            "Y" to Seat(253, 111),
            "B" to Seat(309, 169),
            "A" to Seat(253, 227),
            "X" to Seat(197, 169),
            "Menu" to Seat(115, 370),
            "Start" to Seat(245, 370),
        )
    private val dreamcastLandscape =
        mapOf(
            "L" to Seat(46, 43),
            "R" to Seat(684, 43),
            "AnalogL" to Seat(82, 119, 74f),
            "D" to Seat(82, 222, 76f),
            "Y" to Seat(648, 106),
            "B" to Seat(687, 169),
            "A" to Seat(648, 231),
            "X" to Seat(609, 169),
            "Start" to Seat(648, 348),
            "Menu" to Seat(365, 365),
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

    private val n64Portrait =
        mapOf(
            "L" to Seat(55, 42),
            "R" to Seat(305, 42),
            "D" to Seat(78, 116, 78f),
            "AnalogL" to Seat(78, 222, 86f),
            "Z" to Seat(164, 206),
            "CDirection" to Seat(265, 120, 88f),
            "B" to Seat(226, 226),
            "A" to Seat(300, 254),
            "Menu" to Seat(115, 370),
            "Start" to Seat(245, 370),
        )
    private val n64Landscape =
        mapOf(
            "L" to Seat(49, 43),
            "R" to Seat(648, 43),
            "D" to Seat(82, 114, 72f),
            "AnalogL" to Seat(63, 208, 82f),
            "Z" to Seat(136, 248),
            "CDirection" to Seat(650, 120, 84f),
            "B" to Seat(612, 222),
            "A" to Seat(683, 250),
            "Menu" to Seat(365, 365),
            "Start" to Seat(675, 335),
        )
    private val n64ProgramPortrait =
        mapOf(1 to Seat(180, 42), 2 to Seat(80, 320), 3 to Seat(224, 320), 4 to Seat(155, 270))
    private val n64ProgramLandscape =
        mapOf(1 to Seat(121, 43), 2 to Seat(42, 320), 3 to Seat(615, 299), 4 to Seat(114, 320))

    private val gameCubePortrait =
        mapOf(
            "L" to Seat(48, 35),
            "HalfL" to Seat(124, 35),
            "HalfR" to Seat(236, 35),
            "R" to Seat(312, 35),
            "Z" to Seat(236, 91),
            "AnalogL" to Seat(78, 177, 78f),
            "D" to Seat(78, 292, 74f),
            "A" to Seat(255, 205),
            "B" to Seat(192, 242),
            "Y" to Seat(227, 140),
            "X" to Seat(313, 164),
            "AnalogR" to Seat(267, 310, 66f),
            "Menu" to Seat(115, 430),
            "Start" to Seat(245, 430),
        )
    private val gameCubeLandscape =
        mapOf(
            "L" to Seat(46, 32),
            "HalfL" to Seat(116, 32),
            "HalfR" to Seat(614, 32),
            "R" to Seat(684, 32),
            "Z" to Seat(614, 90),
            "AnalogL" to Seat(82, 170, 78f),
            "D" to Seat(82, 272, 70f),
            "A" to Seat(648, 209),
            "B" to Seat(610, 268),
            "Y" to Seat(614, 150),
            "X" to Seat(684, 150),
            "AnalogR" to Seat(650, 338, 60f),
            "Menu" to Seat(365, 365),
            "Start" to Seat(465, 365),
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
                "D" to Seat(79, 135, 74f),
                "AnalogL" to Seat(79, 238, 74f),
                "X" to Seat(253, 111),
                "A" to Seat(309, 169),
                "B" to Seat(253, 227),
                "Y" to Seat(197, 169),
                "Select" to Seat(80, 370),
                "Menu" to Seat(180, 370),
                "Start" to Seat(280, 370),
            )
    private val pspLandscape =
        dreamcastLandscape +
            mapOf(
                "L" to Seat(49, 43),
                "R" to Seat(648, 49),
                "D" to Seat(82, 119, 74f),
                "AnalogL" to Seat(82, 222, 74f),
                "X" to Seat(648, 107),
                "A" to Seat(687, 169),
                "B" to Seat(648, 231),
                "Y" to Seat(609, 169),
                "Select" to Seat(82, 359),
                "Start" to Seat(648, 348),
            )
    private val pspProgramPortrait =
        mapOf(1 to Seat(180, 60), 2 to Seat(79, 322), 3 to Seat(253, 297), 4 to Seat(151, 302))
    private val pspProgramLandscape =
        mapOf(1 to Seat(121, 43), 2 to Seat(44, 300), 3 to Seat(648, 289), 4 to Seat(114, 300))

    private val playStationPortrait =
        mapOf(
            "L2" to Seat(55, 35),
            "L" to Seat(55, 88),
            "R2" to Seat(305, 35),
            "R" to Seat(305, 88),
            "D" to Seat(78, 174, 86f),
            "X" to Seat(253, 135),
            "A" to Seat(310, 188),
            "B" to Seat(253, 241),
            "Y" to Seat(196, 188),
            "Select" to Seat(80, 347),
            "Menu" to Seat(180, 347),
            "Start" to Seat(280, 347),
        )
    private val playStationLandscape =
        mapOf(
            "L2" to Seat(46, 32),
            "L" to Seat(116, 32),
            "R" to Seat(614, 32),
            "R2" to Seat(684, 32),
            "D" to Seat(82, 118, 86f),
            "X" to Seat(648, 92),
            "A" to Seat(689, 150),
            "B" to Seat(648, 208),
            "Y" to Seat(607, 150),
            "Select" to Seat(265, 365),
            "Menu" to Seat(365, 365),
            "Start" to Seat(465, 365),
        )
    private val dualShockPortrait =
        playStationPortrait +
            mapOf(
                "AnalogL" to Seat(78, 293, 76f),
                "AnalogR" to Seat(266, 310, 70f),
                "Select" to Seat(80, 430),
                "Menu" to Seat(180, 430),
                "Start" to Seat(280, 430),
            )
    private val dualShockLandscape =
        playStationLandscape + mapOf("AnalogL" to Seat(82, 242, 74f), "AnalogR" to Seat(648, 275, 62f))

    private val sonyHostPortrait =
        dualShockPortrait + mapOf("LS" to Seat(148, 285), "RS" to Seat(205, 310))
    private val sonyHostLandscape =
        dualShockLandscape + mapOf("LS" to Seat(180, 365), "RS" to Seat(550, 365))

    private val xboxPortrait =
        dualShockPortrait +
            mapOf(
                "AnalogL" to Seat(78, 174, 84f),
                "D" to Seat(78, 293, 78f),
                "X" to Seat(196, 188),
                "A" to Seat(253, 241),
                "B" to Seat(310, 188),
                "Y" to Seat(253, 135),
                "LS" to Seat(145, 236),
                "RS" to Seat(194, 310),
            )
    private val xboxLandscape =
        dualShockLandscape +
            mapOf(
                "AnalogL" to Seat(82, 107, 80f),
                "D" to Seat(82, 257, 74f),
                "Y" to Seat(648, 89),
                "B" to Seat(687, 147),
                "A" to Seat(648, 205),
                "X" to Seat(609, 147),
                "AnalogR" to Seat(648, 274, 66f),
                "LS" to Seat(82, 184),
                "RS" to Seat(650, 349),
            )

    private val dosPortrait =
        dualShockPortrait +
            mapOf(
                "X" to Seat(196, 188),
                "A" to Seat(253, 241),
                "B" to Seat(310, 188),
                "Y" to Seat(253, 135),
                "Select" to Seat(49, 430),
                "Keyboard" to Seat(135, 430),
                "Menu" to Seat(225, 430),
                "Start" to Seat(311, 430),
            )
    private val dosLandscape =
        dualShockLandscape +
            mapOf(
                "X" to Seat(609, 147),
                "A" to Seat(648, 205),
                "B" to Seat(687, 147),
                "Y" to Seat(648, 89),
                "AnalogL" to Seat(82, 242, 74f),
                "AnalogR" to Seat(648, 274, 64f),
                "Select" to Seat(215, 365),
                "Keyboard" to Seat(315, 365),
                "Menu" to Seat(415, 365),
                "Start" to Seat(515, 365),
            )

    private val wiiSidewaysPortrait =
        mapOf(
            "D" to Seat(80, 155, 86f),
            "Shake" to Seat(145, 70),
            "X" to Seat(220, 95),
            "Y" to Seat(295, 95),
            "B" to Seat(220, 170),
            "A" to Seat(295, 170),
            "Select" to Seat(49, 330),
            "Home" to Seat(135, 330),
            "Menu" to Seat(225, 330),
            "Start" to Seat(311, 330),
        )
    private val wiiSidewaysLandscape =
        mapOf(
            "D" to Seat(82, 145, 86f),
            "Shake" to Seat(82, 49),
            "X" to Seat(615, 95),
            "Y" to Seat(682, 95),
            "B" to Seat(615, 175),
            "A" to Seat(682, 175),
            "Select" to Seat(215, 365),
            "Home" to Seat(315, 365),
            "Menu" to Seat(415, 365),
            "Start" to Seat(515, 365),
        )
    private val wiiSidewaysProgramPortrait =
        mapOf(1 to Seat(80, 252), 2 to Seat(150, 252), 3 to Seat(220, 252), 4 to Seat(290, 252))
    private val wiiSidewaysProgramLandscape =
        mapOf(1 to Seat(44, 239), 2 to Seat(114, 239), 3 to Seat(615, 255), 4 to Seat(682, 255))
    private val wiiClassicPortrait =
        dosPortrait - "Keyboard" + ("Home" to Seat(135, 430))
    private val wiiClassicLandscape =
        dosLandscape - "Keyboard" + ("Home" to Seat(315, 365))

    private val wiiuGamePadPortrait =
        mapOf(
            "L2" to Seat(55, 35),
            "L" to Seat(55, 88),
            "R2" to Seat(305, 35),
            "R" to Seat(305, 88),
            "AnalogL" to Seat(78, 163, 78f),
            "AnalogR" to Seat(266, 163, 78f),
            "LS" to Seat(145, 163),
            "RS" to Seat(196, 163),
            "D" to Seat(78, 280, 84f),
            "X" to Seat(253, 230),
            "A" to Seat(310, 283),
            "B" to Seat(253, 336),
            "Y" to Seat(196, 283),
            "Select" to Seat(49, 435),
            "Menu" to Seat(135, 435),
            "Screen" to Seat(225, 435),
            "Start" to Seat(311, 435),
        )
    private val wiiuGamePadLandscape =
        mapOf(
            "L2" to Seat(46, 32),
            "L" to Seat(116, 32),
            "R2" to Seat(684, 32),
            "R" to Seat(614, 32),
            "AnalogL" to Seat(82, 107, 76f),
            "AnalogR" to Seat(648, 106, 66f),
            "LS" to Seat(82, 181),
            "RS" to Seat(570, 365),
            "D" to Seat(82, 254, 76f),
            "X" to Seat(648, 237),
            "A" to Seat(687, 295),
            "B" to Seat(648, 353),
            "Y" to Seat(609, 295),
            "Select" to Seat(215, 365),
            "Menu" to Seat(315, 365),
            "Screen" to Seat(415, 365),
            "Start" to Seat(515, 365),
        )
    private val wiiuProgramPortrait =
        mapOf(1 to Seat(45, 377), 2 to Seat(135, 377), 3 to Seat(224, 385), 4 to Seat(315, 385))
    private val wiiuProgramLandscape =
        mapOf(1 to Seat(45, 338), 2 to Seat(115, 338), 3 to Seat(615, 179), 4 to Seat(684, 179))

    const val WIIU_SCREEN_ACTION = KeyEvent.KEYCODE_TV_INPUT

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

    private val dsPortrait =
        mapOf(
            "L" to Seat(65, 28),
            "Mic" to Seat(140, 28),
            "Lid" to Seat(220, 28),
            "R" to Seat(295, 28),
            "D" to Seat(76, 119, 82f),
            // C05-B: the DS portrait face diamond is authored at an
            // explicit 44dp — at the generic 48.5dp the diamond's
            // diagonal neighbours cannot stay independent in the
            // 360x280 reference space, which forced the legacy
            // fallback for this configuration.
            "X" to Seat(266, 75, 44f),
            "A" to Seat(315, 125, 44f),
            "B" to Seat(266, 175, 44f),
            "Y" to Seat(217, 125, 44f),
            "Select" to Seat(64, 253),
            "Menu" to Seat(180, 253),
            "Start" to Seat(296, 253),
        )
    private val dsLandscape =
        mapOf(
            "L" to Seat(82, 49),
            "Mic" to Seat(146, 49),
            "Lid" to Seat(594, 49),
            "R" to Seat(648, 49),
            "D" to Seat(82, 145, 82f),
            "X" to Seat(648, 112),
            "A" to Seat(687, 170),
            "B" to Seat(648, 228),
            "Y" to Seat(609, 170),
            "Select" to Seat(82, 322),
            "Menu" to Seat(365, 365),
            "Start" to Seat(648, 335),
        )
    private val dsProgramPortrait = mapOf(1 to Seat(76, 205), 2 to Seat(182, 205))
    private val dsProgramLandscape = mapOf(1 to Seat(44, 242), 2 to Seat(114, 242))

    private val threeDsPortrait =
        mapOf(
            "L" to Seat(65, 28),
            "R" to Seat(295, 28),
            "AnalogL" to Seat(77, 92, 78f),
            "D" to Seat(77, 190, 78f),
            "X" to Seat(267, 79),
            "A" to Seat(316, 129),
            "B" to Seat(267, 179),
            "Y" to Seat(218, 129),
            "Select" to Seat(64, 285),
            "Menu" to Seat(180, 285),
            "Start" to Seat(296, 285),
        )
    private val threeDsLandscape =
        mapOf(
            "L" to Seat(82, 40),
            "R" to Seat(648, 49),
            "AnalogL" to Seat(82, 119, 78f),
            "D" to Seat(82, 222, 78f),
            "X" to Seat(648, 109),
            "A" to Seat(689, 167),
            "B" to Seat(648, 225),
            "Y" to Seat(607, 167),
            "Select" to Seat(82, 360),
            "Menu" to Seat(365, 365),
            "Start" to Seat(648, 335),
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
            TouchControllerID.PCE_SIX,
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
            TouchControllerID.N64,
            TouchControllerID.MELONDS,
            TouchControllerID.DESMUME,
            TouchControllerID.WS_PORTRAIT,
            TouchControllerID.WS_LANDSCAPE,
            TouchControllerID.ARCADE_4,
            TouchControllerID.ARCADE_6,
            TouchControllerID.ARCADE_2,
            TouchControllerID.ARCADE_3,
            TouchControllerID.XBOX,
            TouchControllerID.XBOX360,
            TouchControllerID.DOS,
            TouchControllerID.WII_CLASSIC,
            TouchControllerID.WII_SIDEWAYS,
        )

    fun densePortrait(controller: TouchControllerID?): Boolean =
        controller in
            setOf(
                TouchControllerID.PSX_DUALSHOCK,
                TouchControllerID.GAMECUBE,
                TouchControllerID.XBOX,
                TouchControllerID.XBOX360,
                TouchControllerID.WII_CLASSIC,
                TouchControllerID.DOS,
            )

    fun supports(controller: TouchControllerID?): Boolean = controller in controllers

    fun inventoryFor(
        controller: TouchControllerID,
        system: SystemID? = null,
        wiiuPro: Boolean = false,
    ): NativeControlInventory? =
        when (controller) {
            TouchControllerID.WII_CLASSIC ->
                if (system == SystemID.WII_U) {
                    wiiuInventory(controller, wiiuPro)
                } else {
                    labelledFaces(analogueInventory(SystemID.WII, controller))
                }
            TouchControllerID.WII_SIDEWAYS -> analogueInventory(SystemID.WII, controller)
            TouchControllerID.WS_PORTRAIT, TouchControllerID.WS_LANDSCAPE -> wonderSwanInventory(controller)
            TouchControllerID.ARCADE_4, TouchControllerID.ARCADE_6,
            TouchControllerID.ARCADE_2, TouchControllerID.ARCADE_3,
            ->
                analogueInventory(SystemID.FBNEO, controller)
            TouchControllerID.NES ->
                NativeControlInventory
                    .forController(
                        TouchControllerID.GB,
                    )?.copy(controller = controller)
            TouchControllerID.PCE_SIX -> {
                val inventory = checkNotNull(inventoryFor(TouchControllerID.PCE))
                val faces =
                    pceSixButtonLabels.map { (code, roman) ->
                        NativeControlSpec(roman, roman, NativeControlKind.FACE, keyCode = code)
                    }
                inventory.copy(
                    controller = controller,
                    controls = inventory.controls.filterNot { it.key in setOf("A", "B") } + faces,
                )
            }
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
                                    controller == TouchControllerID.PCE && seat.key == "A" ->
                                        seat.copy(label = "II", keyCode = KeyEvent.KEYCODE_BUTTON_B)
                                    controller == TouchControllerID.PCE && seat.key == "B" ->
                                        seat.copy(label = "I", keyCode = KeyEvent.KEYCODE_BUTTON_A)
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
            TouchControllerID.N64 -> analogueInventory(SystemID.N64, controller)
            TouchControllerID.DOS -> labelledFaces(analogueInventory(SystemID.DOS, controller))
            TouchControllerID.XBOX, TouchControllerID.XBOX360 -> withStickClicks(controller, "LS", "RS")
            TouchControllerID.MELONDS, TouchControllerID.DESMUME -> NativeControlInventory.forHoldTray(controller)
            TouchControllerID.PSP, TouchControllerID.PSX_DUALSHOCK, TouchControllerID.NINTENDO_3DS -> {
                if (controller == TouchControllerID.PSX_DUALSHOCK &&
                    system in setOf(SystemID.PLAYSTATION_2, SystemID.PLAYSTATION_3)
                ) {
                    withStickClicks(controller, "L3", "R3")
                } else {
                    NativeControlInventory.forHoldTray(controller)?.let { inventory ->
                        inventory.copy(
                            controls =
                                inventory.controls.map { seat ->
                                    if (seat.kind == NativeControlKind.ANALOG) seat.copy(pressCode = null) else seat
                                },
                        )
                    }
                }
            }
            else -> NativeControlInventory.forController(controller)
        }

    private fun labelledFaces(inventory: NativeControlInventory): NativeControlInventory =
        inventory.copy(
            controls =
                inventory.controls.map { seat ->
                    if (seat.kind == NativeControlKind.FACE && seat.label in setOf("A", "B", "X", "Y")) {
                        seat.copy(key = seat.label, seat = seat.label)
                    } else {
                        seat
                    }
                },
        )

    private fun wiiuInventory(
        controller: TouchControllerID,
        pro: Boolean,
    ): NativeControlInventory {
        val inventory = labelledFaces(analogueInventory(SystemID.WII_U, controller))
        val screen =
            NativeControlSpec(
                "Screen",
                "Screen",
                NativeControlKind.UTILITY,
                keyCode = WIIU_SCREEN_ACTION,
                label = "Screen",
            )
        return if (pro) inventory else inventory.copy(controls = inventory.controls + screen)
    }

    private fun withStickClicks(
        controller: TouchControllerID,
        leftLabel: String,
        rightLabel: String,
    ): NativeControlInventory {
        val native = checkNotNull(NativeControlInventory.forController(controller))
        val clicks =
            listOf(
                NativeControlSpec(
                    "LS",
                    "LS",
                    NativeControlKind.FACE,
                    keyCode = KeyEvent.KEYCODE_BUTTON_THUMBL,
                    label = leftLabel,
                ),
                NativeControlSpec(
                    "RS",
                    "RS",
                    NativeControlKind.FACE,
                    keyCode = KeyEvent.KEYCODE_BUTTON_THUMBR,
                    label = rightLabel,
                ),
            )
        return native.copy(
            controls =
                native.controls.map {
                    if (it.kind == NativeControlKind.ANALOG) it.copy(pressCode = null) else it
                } + clicks,
        )
    }

    private fun wonderSwanInventory(controller: TouchControllerID): NativeControlInventory {
        val input = checkNotNull(WonderSwanPivotInput.forController(controller))
        val base = checkNotNull(NativeControlInventory.forController(TouchControllerID.GB))
        val controls =
            base.controls.filterNot { it.key == "Select" }.map {
                when (it.key) {
                    "D" -> it.copy(label = input.primaryLabel)
                    "A" -> it.copy(keyCode = input.a)
                    "B" -> it.copy(keyCode = input.b)
                    else -> it
                }
            }
        val cursor =
            NativeControlSpec(
                "Cursor2",
                "cursor2",
                NativeControlKind.DPAD,
                motionSource = input.source,
                label = input.secondaryLabel,
            )
        return NativeControlInventory(controller, controls + cursor)
    }

    // Preserve programmable identities when guest rotation or the arcade seat order changes.
    private fun programmableCode(
        native: NativeControlInventory,
        seat: NativeControlSpec,
    ): Int? =
        when (native.controller) {
            TouchControllerID.WS_PORTRAIT, TouchControllerID.WS_LANDSCAPE ->
                when (seat.key) {
                    "A" -> KeyEvent.KEYCODE_BUTTON_A
                    "B" -> KeyEvent.KEYCODE_BUTTON_B
                    else -> seat.keyCode
                }
            TouchControllerID.ARCADE_4, TouchControllerID.ARCADE_6,
            TouchControllerID.ARCADE_2, TouchControllerID.ARCADE_3,
            ->
                if (seat.kind == NativeControlKind.FACE) {
                    KeyEvent.KEYCODE_BUTTON_1 + seat.label.toInt() - 1
                } else {
                    seat.keyCode
                }
            else -> seat.keyCode
        }

    fun guestAction(
        profile: GripArcProfile,
        slot: Int,
        native: NativeControlInventory,
    ): GripArcAction? {
        val codes =
            native.controls
                .filterNot { it.key in setOf("Menu", "Keyboard", "Screen", "Mic", "Lid") }
                .mapNotNull { seat ->
                    programmableCode(native, seat)?.let { it to checkNotNull(seat.keyCode) }
                }.toMap()
        return when (val action = profile.supportedActionFor(slot, codes.keys)) {
            is GripArcAction.Native -> action.copy(keys = action.keys.mapTo(linkedSetOf(), codes::getValue))
            is GripArcAction.Hold -> action.copy(key = codes.getValue(action.key))
            is GripArcAction.Turbo -> action.copy(key = codes.getValue(action.key))
            GripArcAction.OpenMenu, null -> action
        }
    }

    fun actionLabels(native: NativeControlInventory): Map<Int, String> =
        native.controls
            .filterNot { it.key in setOf("Menu", "Keyboard", "Screen", "Mic", "Lid") }
            .mapNotNull { seat ->
                val label =
                    when (seat.icon) {
                        NativeControlIcon.CIRCLE -> "Circle"
                        NativeControlIcon.CROSS -> "Cross"
                        NativeControlIcon.SQUARE -> "Square"
                        NativeControlIcon.TRIANGLE -> "Triangle"
                        else -> seat.label
                    }
                programmableCode(native, seat)?.let { it to label }
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
        val profile =
            checkNotNull(
                PivotArcSoloProfiles.forMode(system, controller)
                    ?: PivotArcSoloProfiles.forNativeHost(system, controller),
            )
        val controls =
            profile.seats.filter { it.holdOf == null }.map { seat ->
                val kind =
                    when {
                        seat.analog -> NativeControlKind.ANALOG
                        seat.direction != null -> NativeControlKind.DPAD
                        seat.utility -> NativeControlKind.UTILITY
                        seat.id in setOf("L", "R", "L2", "R2", "HalfL", "HalfR") -> NativeControlKind.SHOULDER
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
        // C01: GameCube landscape never falls through to the legacy seat
        // projection or the compact directional fallback below. It either
        // fits its authored panels and utility rail, or it does not fit.
        if (landscape && native.controller == TouchControllerID.GAMECUBE) {
            return resolveGameCubeLandscape(native, safe, viewport)
        }
        val width = if (landscape) 730f else 360f
        val snes = native.controller == TouchControllerID.SNES
        val wonderSwan = WonderSwanPivotInput.forController(native.controller) != null
        val arcade =
            native.controller in
                setOf(
                    TouchControllerID.ARCADE_4,
                    TouchControllerID.ARCADE_6,
                    TouchControllerID.ARCADE_2,
                    TouchControllerID.ARCADE_3,
                )
        val xbox = native.controller in setOf(TouchControllerID.XBOX, TouchControllerID.XBOX360)
        val dos = native.controller == TouchControllerID.DOS
        val ds = native.controller in setOf(TouchControllerID.MELONDS, TouchControllerID.DESMUME)
        val wiiClassic = native.controller == TouchControllerID.WII_CLASSIC && native.controls.any { it.key == "Home" }
        val wiiSideways = native.controller == TouchControllerID.WII_SIDEWAYS
        val wiiu = native.controller == TouchControllerID.WII_CLASSIC && !wiiClassic
        val wiiuPro = wiiu && native.controls.none { it.key == "Screen" }
        val sonyHost = native.controller == TouchControllerID.PSX_DUALSHOCK && native.controls.any { it.key == "LS" }
        val height =
            if (landscape) {
                390f
            } else if (
                xbox ||
                dos ||
                wiiu ||
                wiiClassic ||
                native.controller in setOf(TouchControllerID.GAMECUBE, TouchControllerID.PSX_DUALSHOCK)
            ) {
                460f
            } else if (native.controller in
                setOf(
                    TouchControllerID.DREAMCAST,
                    TouchControllerID.PSP,
                    TouchControllerID.N64,
                )
            ) {
                420f
            } else if (snes || wonderSwan || native.controller == TouchControllerID.LYNX) {
                400f
            } else if (ds) {
                280f
            } else if (native.controller == TouchControllerID.NINTENDO_3DS) {
                320f
            } else if (native.controller == TouchControllerID.ATARI2600) {
                350f
            } else {
                380f
            }

        val scale = minOf(panel.width / width, panel.height / height, 1.22f)
        val deckLeft = panel.centerX - width * scale / 2f
        val deckTop = panel.centerY - height * scale / 2f

        fun authoredBounds(
            seat: Seat,
            rail: Boolean,
            diameter: Float = maxOf(seat.size, seat.size * scale, 49.2f),
        ): PivotArcBounds {
            val radius = diameter / 2f
            val x = deckLeft + seat.x * scale
            val y =
                if (rail && landscape) {
                    panel.bottom - maxOf(radius, (height - seat.y) * scale)
                } else {
                    deckTop + seat.y * scale
                }
            return PivotArcBounds(x - radius, y - radius, x + radius, y + radius)
        }

        fun bounds(
            seat: Seat,
            menu: Boolean = false,
        ): PivotArcBounds {
            val radius = seat.size / 2f
            val x =
                (panel.left + panel.width * seat.x / width)
                    .coerceIn(panel.left + radius, maxOf(panel.left + radius, panel.right - radius))
            val referenceY = panel.top + panel.height * seat.y / height
            val top = if (menu && landscape) maxOf(panel.top, viewport.bottom + 2f) else panel.top
            val y = referenceY.coerceIn(top + radius, maxOf(top + radius, panel.bottom - radius))
            return PivotArcBounds(x - radius, y - radius, x + radius, y + radius)
        }
        val gba = native.controller == TouchControllerID.GBA
        val noSelect = native.controller in setOf(TouchControllerID.SMS, TouchControllerID.GG, TouchControllerID.NGP)
        val authoredNativeSeats =
            when {
                gba -> if (landscape) landscapeNative else portraitNative
                ds -> if (landscape) dsLandscape else dsPortrait
                native.controller == TouchControllerID.PCE_SIX -> if (landscape) pceSixLandscape else pceSixPortrait
                xbox -> if (landscape) xboxLandscape else xboxPortrait
                dos -> if (landscape) dosLandscape else dosPortrait
                wiiClassic -> if (landscape) wiiClassicLandscape else wiiClassicPortrait
                wiiSideways -> if (landscape) wiiSidewaysLandscape else wiiSidewaysPortrait
                wiiu -> {
                    val seats = if (landscape) wiiuGamePadLandscape else wiiuGamePadPortrait
                    if (!wiiuPro) {
                        seats
                    } else if (landscape) {
                        seats - "Screen" +
                            mapOf(
                                "Select" to Seat(265, 365),
                                "Menu" to Seat(365, 365),
                                "Start" to Seat(465, 365),
                            )
                    } else {
                        seats - "Screen" +
                            mapOf(
                                "Select" to Seat(80, 435),
                                "Menu" to Seat(180, 435),
                                "Start" to Seat(280, 435),
                            )
                    }
                }
                sonyHost -> if (landscape) sonyHostLandscape else sonyHostPortrait
                wonderSwan -> if (landscape) wonderSwanLandscape else wonderSwanPortrait
                native.controller == TouchControllerID.ARCADE_2 ->
                    if (landscape) arcadeTwoLandscape else arcadeTwoPortrait
                native.controller == TouchControllerID.ARCADE_3 ->
                    if (landscape) arcadeThreeLandscape else arcadeThreePortrait
                native.controller == TouchControllerID.ARCADE_4 ->
                    if (landscape) arcadeFourLandscape else arcadeFourPortrait
                native.controller == TouchControllerID.ARCADE_6 ->
                    if (landscape) arcadeSixLandscape else arcadeSixPortrait
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
                native.controller == TouchControllerID.N64 -> if (landscape) n64Landscape else n64Portrait
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
                ds -> if (landscape) dsProgramLandscape else dsProgramPortrait
                native.controller == TouchControllerID.PCE_SIX ->
                    if (landscape) pceSixProgramLandscape else pceSixProgramPortrait
                xbox ->
                    if (landscape) {
                        mapOf(1 to Seat(45, 349), 2 to Seat(115, 349), 3 to Seat(588, 349), 4 to Seat(711, 349))
                    } else {
                        playStationProgram(landscape = false, dualShock = true)
                    }
                dos || wiiClassic -> playStationProgram(landscape, dualShock = true)
                wiiSideways -> if (landscape) wiiSidewaysProgramLandscape else wiiSidewaysProgramPortrait
                wiiu -> if (landscape) wiiuProgramLandscape else wiiuProgramPortrait
                wonderSwan -> if (landscape) wonderSwanProgramLandscape else wonderSwanProgramPortrait
                arcade -> if (landscape) arcadeProgramLandscape else arcadeProgramPortrait
                native.controller == TouchControllerID.NINTENDO_3DS ->
                    if (landscape) threeDsProgramLandscape else threeDsProgramPortrait
                native.controller in setOf(TouchControllerID.PSX, TouchControllerID.PSX_DUALSHOCK) ->
                    playStationProgram(landscape, native.controller == TouchControllerID.PSX_DUALSHOCK)
                native.controller == TouchControllerID.PSP -> if (landscape) pspProgramLandscape else pspProgramPortrait
                native.controller == TouchControllerID.DREAMCAST ->
                    if (landscape) dreamcastProgramLandscape else dreamcastProgramPortrait
                native.controller == TouchControllerID.N64 -> if (landscape) n64ProgramLandscape else n64ProgramPortrait
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

        fun nativeTargets(
            compact: Boolean = false,
            authored: Boolean = false,
        ): Map<String, PivotArcBounds> =
            nativeSeats.mapValues { (key, seat) ->
                val centralUtility =
                    key == "Menu" ||
                        (xbox && key in setOf("Select", "Start")) ||
                        (sonyHost && key in setOf("LS", "RS")) ||
                        (dos && key in setOf("Select", "Start", "Keyboard")) ||
                        ((wiiClassic || wiiSideways) && key in setOf("Select", "Start", "Home")) ||
                        (wiiu && key in setOf("Select", "Start", "Screen", "RS")) ||
                        (
                            native.controller in setOf(TouchControllerID.PSX, TouchControllerID.PSX_DUALSHOCK) &&
                                key in setOf("Select", "Start")
                        ) ||
                        (native.controller == TouchControllerID.GAMECUBE && key == "Start")
                val sized = if (compact) seat.copy(size = minOf(seat.size, 58f)) else seat
                if (authored) {
                    // C05-B explicit authored dimensions in the uniform
                    // transform (same rules as the landscape zone
                    // resolver): primary directional/analogue controls
                    // never below 56dp, everything else scales from its
                    // authored size with a 42dp floor. Programmable
                    // seats are an explicit 45dp (see authoredTargets).
                    val kind = native.controls.firstOrNull { it.key == key }?.kind
                    val diameter =
                        if (kind == NativeControlKind.DPAD || kind == NativeControlKind.ANALOG) {
                            maxOf(seat.size * scale, 56f)
                        } else {
                            maxOf(seat.size * scale, 42f)
                        }
                    authoredBounds(sized, centralUtility, diameter)
                } else {
                    bounds(sized, menu = centralUtility)
                }
            }
        if (nativeSeats.keys != native.controls.map { it.key }.toSet()) return null
        // C02-A: landscape resolves through authored panels and a utility
        // rail first (the C01 architecture, generalised). See
        // resolveAuthoredLandscape. The uniform authored transform below
        // is the only other landscape route: the legacy seat projection
        // and the compact directional fallback no longer run in landscape.
        if (landscape) {
            val zoned =
                resolveAuthoredLandscape(native, nativeSeats, programSeats, safe, viewport, width, height)
            if (zoned != null) return zoned
        }
        val program = programSeats.mapKeys { (slot, _) -> "program:$slot" }
        val programTargets = program.mapValues { bounds(it.value) }
        val display = viewport.inset(1.5f)
        val authoredTargets =
            nativeTargets(authored = true) + program.mapValues { authoredBounds(it.value, false, 45f) }
        val authored = authoredTargets.takeIf { independentTargets(it.values.toList(), panel, display) }
        // Landscape: authored routes only — no legacy projection, no
        // compact directional shrink. Portrait keeps its existing routes
        // until its authored migration lands.
        if (landscape) {
            val fitted = authored ?: return null
            return placementFromFitted(
                native,
                nativeSeats,
                programSeats,
                fitted,
                panel,
                safe,
                viewport,
                width,
                height,
                fit = "uniform chassis transform",
            )
        }
        val regular = authored ?: growTargets(nativeTargets() + programTargets, panel, display)
        val fitted =
            regular ?: growTargets(nativeTargets(compact = true) + programTargets, panel, display)
                ?: return null
        return placementFromFitted(
            native,
            nativeSeats,
            programSeats,
            fitted,
            panel,
            safe,
            viewport,
            width,
            height,
            fit =
                when {
                    authored != null -> "uniform chassis transform"
                    regular != null -> "legacy seat projection"
                    else -> "legacy compact directional targets"
                },
        )
    }

    /** Build the placement (caps, visuals, seats, dock, geometry) from resolved target bounds. */
    private fun placementFromFitted(
        native: NativeControlInventory,
        nativeSeats: Map<String, Seat>,
        programSeats: Map<Int, Seat>,
        fitted: Map<String, PivotArcBounds>,
        panel: PivotArcBounds,
        safe: PivotArcBounds,
        viewport: PivotArcBounds,
        width: Float,
        height: Float,
        fit: String,
    ): GripArcCombinedPlacement {
        val controls = nativeSeats.keys.associateWith(fitted::getValue)
        val programmable = programSeats.keys.associateWith { fitted.getValue("program:$it") }
        val capWidths =
            native.controls
                .groupBy { control -> control.kind to nativeSeats.getValue(control.key).size }
                .flatMap { (family, members) ->
                    val cap = minOf(family.second + 7f, members.minOf { controls.getValue(it.key).width } - 5f)
                    members.map { it.key to cap }
                }.toMap()
        val visuals =
            native.controls.associate { control ->
                val cap = capWidths.getValue(control.key)
                val gameCubeFace =
                    native.controller == TouchControllerID.GAMECUBE && control.kind == NativeControlKind.FACE
                // C05-B: analogue thumb caps come from the explicit
                // per-console authored table, not the derived formula.
                val size =
                    when {
                        control.kind == NativeControlKind.ANALOG -> {
                            val fraction =
                                analogueCapFractions[native.controller]?.get(control.key)
                                    ?: DEFAULT_ANALOGUE_CAP_FRACTION
                            controls.getValue(control.key).width * fraction
                        }
                        gameCubeFace && control.key in setOf("B", "X", "Y") -> cap - 5f
                        else -> cap
                    }
                val visualHeight =
                    when (control.kind) {
                        NativeControlKind.SHOULDER, NativeControlKind.UTILITY -> size * 0.72f
                        else -> size
                    }
                control.key to PivotArcSize(size, visualHeight)
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
        val programWidth = programmable.values.minOf { it.width }.coerceAtMost(60.5f)
        val dock =
            GripArcPlacement(
                housing = panel,
                targets = programmable,
                visuals = programmable.mapValues { PivotArcSize(programWidth - 5f, programWidth * 0.82f) },
                scale = 1f,
                side = GripArcSide.RIGHT,
            )
        val geometry =
            StandaloneGripArcGeometry(
                reference = PivotArcSize(width, height),
                authoredSeats =
                    (nativeSeats + programSeats.mapKeys { (slot, _) -> "program:$slot" }).mapValues { (_, seat) ->
                        val x = seat.x.toFloat()
                        val y = seat.y.toFloat()
                        PivotArcBounds(x, y, x, y).inset(seat.size / 2f)
                    },
                safe = safe,
                viewport = viewport,
                fit = fit,
            )
        return GripArcCombinedPlacement(dock, controls, visuals, seats, native, native.inputIds, geometry)
    }

    /**
     * C02-A — authored landscape panels + utility rail for every standalone
     * configuration (generalising the C01 GameCube architecture).
     *
     * The measured viewport divides the safe area into a left hardware
     * panel, a right hardware panel and a utility rail below the aperture.
     * Each console keeps its own authored seat map and authentic arrangement:
     * a seat's reference x decides its zone (left reference region → left
     * panel, right reference region → right panel, centre region → rail),
     * its position inside the zone is the authored proportion of that zone,
     * its y is the authored proportion of the panel height, and all target
     * sizes share one uniform scale — no independent x/y remap, no seat
     * clamping into a different arrangement, and no compact shrink of the
     * directional controls. Returns null when the arrangement does not fit
     * its zones on the measured bounds.
     */
    private fun resolveAuthoredLandscape(
        native: NativeControlInventory,
        nativeSeats: Map<String, Seat>,
        programSeats: Map<Int, Seat>,
        safe: PivotArcBounds,
        viewport: PivotArcBounds,
        width: Float,
        height: Float,
    ): GripArcCombinedPlacement? {
        val gap = 6f
        val leftPanel = PivotArcBounds(safe.left, safe.top, viewport.left - gap, safe.bottom)
        val rightPanel = PivotArcBounds(viewport.right + gap, safe.top, safe.right, safe.bottom)
        val rail = PivotArcBounds(viewport.left, viewport.bottom + gap, viewport.right, safe.bottom)
        if (!leftPanel.isFiniteArea() || !rightPanel.isFiniteArea() || !rail.isFiniteArea()) return null
        val zoneWidth = minOf(leftPanel.width, rightPanel.width)
        if (zoneWidth < 120f || rail.height < 40f) return null
        val scale = minOf(safe.height / height, zoneWidth / 200f, 1.22f)

        // C02-D explicit dimensions (not artefacts of a general floor):
        // primary directional and analogue controls never render below
        // 56dp, programmable GripARC seats are an explicit uniform 45dp,
        // and every other control keeps a 42dp floor under its authored
        // size. Verified against all 43 non-GameCube landscape fixtures.
        val primaryKinds = setOf(NativeControlKind.DPAD, NativeControlKind.ANALOG)
        val kindByKey = native.controls.associate { it.key to it.kind }

        fun place(
            seat: Seat,
            primary: Boolean,
            programmable: Boolean,
        ): PivotArcBounds {
            val diameter =
                when {
                    programmable -> 45f
                    primary -> maxOf(seat.size * scale, 56f)
                    else -> maxOf(seat.size * scale, 42f)
                }
            val radius = diameter / 2f
            val centerX: Float
            val centerY: Float
            when {
                seat.x > 200 && seat.x < 530 -> {
                    val fraction = ((seat.x - 200f) / 330f).coerceIn(0f, 1f)
                    centerX =
                        (rail.left + fraction * rail.width).coerceIn(rail.left + radius, rail.right - radius)
                    centerY = safe.bottom - maxOf(radius, (height - seat.y) * scale)
                }
                seat.x <= 200 -> {
                    val fraction = seat.x / 200f
                    centerX =
                        (leftPanel.left + fraction * leftPanel.width)
                            .coerceIn(leftPanel.left + radius, leftPanel.right - radius)
                    centerY =
                        (safe.top + seat.y * (safe.height / height))
                            .coerceIn(safe.top + radius, safe.bottom - radius)
                }
                else -> {
                    val fraction = (seat.x - 530f) / 200f
                    centerX =
                        (rightPanel.left + fraction * rightPanel.width)
                            .coerceIn(rightPanel.left + radius, rightPanel.right - radius)
                    centerY =
                        (safe.top + seat.y * (safe.height / height))
                            .coerceIn(safe.top + radius, safe.bottom - radius)
                }
            }
            return PivotArcBounds(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius,
            )
        }

        val fitted = linkedMapOf<String, PivotArcBounds>()
        for ((key, seat) in nativeSeats) {
            fitted[key] = place(seat, primary = kindByKey[key] in primaryKinds, programmable = false)
        }
        for ((slot, seat) in programSeats) {
            fitted["program:$slot"] = place(seat, primary = false, programmable = true)
        }
        // Zone containment: panel seats stay in their panel column, rail
        // seats stay in the rail, and nothing touches the aperture.
        for ((key, seat) in nativeSeats) {
            val target = fitted.getValue(key)
            val contained =
                when {
                    seat.x > 200 && seat.x < 530 -> target.inside(rail)
                    seat.x <= 200 ->
                        target.left >= leftPanel.left - 0.05f && target.right <= leftPanel.right + 0.05f
                    else ->
                        target.left >= rightPanel.left - 0.05f && target.right <= rightPanel.right + 0.05f
                }
            if (!contained) return null
        }
        val display = viewport.inset(1.5f)
        if (!independentTargets(fitted.values.toList(), safe, display)) return null
        return placementFromFitted(
            native,
            nativeSeats,
            programSeats,
            fitted,
            safe,
            safe,
            viewport,
            width,
            height,
            fit = AUTHORED_LANDSCAPE_FIT,
        )
    }

    /**
     * C01 — authored GameCube landscape.
     *
     * The gameplay aperture and the controller space are allocated together
     * from the measured safe area and viewport: the space left of the
     * aperture is the left hardware panel, the space right of it is the
     * right hardware panel, and the strip below the aperture is a utility
     * rail for Menu and Start. Every target is authored inside one of
     * those three zones, so no target can cross a panel outline, sit on
     * the aperture, or be shrunk by the legacy projection / compact
     * fallback. Directional and analogue controls are deliberately larger
     * than the face buttons; the GameCube face cluster keeps its console
     * arrangement (large central A, B low-left, X/Y kidneys above), and
     * the programmable GripARC seats are contained in the panels.
     * Returns null when the measured zones cannot hold this arrangement.
     */
    private fun resolveGameCubeLandscape(
        native: NativeControlInventory,
        safe: PivotArcBounds,
        viewport: PivotArcBounds,
    ): GripArcCombinedPlacement? {
        val gap = 6f
        val leftPanel = PivotArcBounds(safe.left, safe.top, viewport.left - gap, safe.bottom)
        val rightPanel = PivotArcBounds(viewport.right + gap, safe.top, safe.right, safe.bottom)
        val rail = PivotArcBounds(viewport.left, viewport.bottom + gap, viewport.right, safe.bottom)
        if (!leftPanel.isFiniteArea() || !rightPanel.isFiniteArea() || !rail.isFiniteArea()) return null
        if (leftPanel.width < 160f || rightPanel.width < 160f) return null
        if (rail.height < 48f || rail.width < 120f) return null

        fun target(
            centerX: Float,
            centerY: Float,
            size: Float,
        ): PivotArcBounds =
            PivotArcBounds(
                centerX - size / 2f,
                centerY - size / 2f,
                centerX + size / 2f,
                centerY + size / 2f,
            )

        fun panelX(
            panel: PivotArcBounds,
            fraction: Float,
        ): Float = panel.left + panel.width * fraction

        val controls = linkedMapOf<String, PivotArcBounds>()

        fun place(
            key: String,
            centerX: Float,
            centerY: Float,
            size: Float,
        ) {
            controls[key] = target(centerX, centerY, size)
        }

        // Left hardware panel: shoulders, large analogue stick, large D-pad.
        place("L", panelX(leftPanel, 0.181f), safe.top + 28f, 48f)
        place("HalfL", panelX(leftPanel, 0.819f), safe.top + 28f, 48f)
        place("AnalogL", panelX(leftPanel, 0.5f), safe.top + 110f, 92f)
        place("D", panelX(leftPanel, 0.5f), safe.top + 228f, 88f)
        // Right hardware panel: shoulders and Z, then the face cluster
        // (Y/X kidneys above the large central A, B low-left) and C-stick.
        place("HalfR", panelX(rightPanel, 0.181f), safe.top + 28f, 48f)
        place("Z", panelX(rightPanel, 0.5f), safe.top + 28f, 44f)
        place("R", panelX(rightPanel, 0.819f), safe.top + 28f, 48f)
        place("Y", panelX(rightPanel, 0.229f), safe.top + 96f, 52f)
        place("X", panelX(rightPanel, 0.771f), safe.top + 96f, 52f)
        place("A", panelX(rightPanel, 0.5f), safe.top + 168f, 72f)
        place("B", panelX(rightPanel, 0.211f), safe.top + 240f, 52f)
        place("AnalogR", panelX(rightPanel, 0.711f), safe.top + 240f, 66f)
        // Utility rail below the aperture: Menu and Start, contained.
        place("Menu", viewport.centerX - 40f, safe.bottom - 26f, 46f)
        place("Start", viewport.centerX + 40f, safe.bottom - 26f, 46f)
        if (controls.keys != native.controls.map { it.key }.toSet()) return null

        // Programmable GripARC seats stay inside the hardware panels.
        val programmable =
            linkedMapOf(
                1 to target(panelX(leftPanel, 0.181f), safe.bottom - 32f, 48f),
                2 to target(panelX(leftPanel, 0.819f), safe.bottom - 32f, 48f),
                3 to target(panelX(rightPanel, 0.181f), safe.bottom - 32f, 48f),
                4 to target(panelX(rightPanel, 0.819f), safe.bottom - 32f, 48f),
            )
        val leftKeys = setOf("L", "HalfL", "AnalogL", "D")
        val rightKeys = setOf("HalfR", "Z", "R", "Y", "X", "A", "B", "AnalogR")
        if (leftKeys.any { !controls.getValue(it).inside(leftPanel) }) return null
        if (rightKeys.any { !controls.getValue(it).inside(rightPanel) }) return null
        if (setOf("Menu", "Start").any { !controls.getValue(it).inside(rail) }) return null
        if (programmable.any { (slot, bounds) -> !bounds.inside(if (slot <= 2) leftPanel else rightPanel) }) {
            return null
        }
        val display = viewport.inset(1.5f)
        val allTargets = controls.values.toList() + programmable.values.toList()
        if (!independentTargets(allTargets, safe, display)) return null

        val capWidths =
            native.controls
                .groupBy { control -> control.kind to controls.getValue(control.key).width }
                .flatMap { (family, members) ->
                    val cap = minOf(family.second + 7f, members.minOf { controls.getValue(it.key).width } - 5f)
                    members.map { it.key to cap }
                }.toMap()
        val visuals =
            native.controls.associate { control ->
                val cap = capWidths.getValue(control.key)
                val size = if (control.key in setOf("B", "X", "Y")) cap - 5f else cap
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
        val programWidth = programmable.values.minOf { it.width }.coerceAtMost(60.5f)
        val dock =
            GripArcPlacement(
                housing = safe,
                targets = programmable,
                visuals = programmable.mapValues { PivotArcSize(programWidth - 5f, programWidth * 0.82f) },
                scale = 1f,
                side = GripArcSide.RIGHT,
            )
        val geometry =
            StandaloneGripArcGeometry(
                reference = PivotArcSize(safe.width, safe.height),
                authoredSeats = controls + programmable.mapKeys { (slot, _) -> "program:$slot" },
                safe = safe,
                viewport = viewport,
                fit = GAMECUBE_LANDSCAPE_FIT,
            )
        return GripArcCombinedPlacement(dock, controls, visuals, seats, native, native.inputIds, geometry)
    }

    /** Expand all hit regions together to preserve the proportions of the authored seats. */
    private fun growTargets(
        minimum: Map<String, PivotArcBounds>,
        panel: PivotArcBounds,
        display: PivotArcBounds,
    ): Map<String, PivotArcBounds>? {
        if (!independentTargets(minimum.values.toList(), panel, display)) return null
        val limit = 6f

        fun largestGrowth(fits: (Float) -> Boolean): Float {
            var low = 0f
            var high = limit
            repeat(12) {
                val mid = (low + high) / 2f
                if (fits(mid)) low = mid else high = mid
            }
            return low
        }

        val shared =
            largestGrowth { growth ->
                independentTargets(minimum.values.map { it.inset(growth) }, panel, display)
            }
        return minimum.mapValues { (_, target) -> target.inset(shared) }
    }

    private fun independentTargets(
        targets: List<PivotArcBounds>,
        panel: PivotArcBounds,
        display: PivotArcBounds,
    ): Boolean =
        targets.all { it.inside(panel) && !it.intersects(display) } &&
            targets.indices.all { index ->
                (index + 1 until targets.size).none { targets[index].intersects(targets[it]) }
            }
}
