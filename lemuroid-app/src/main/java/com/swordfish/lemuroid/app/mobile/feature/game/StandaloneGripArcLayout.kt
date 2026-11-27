package com.swordfish.lemuroid.app.mobile.feature.game

import com.swordfish.touchinput.radial.settings.NativeControlInventory
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
            else -> NativeControlInventory.forController(controller)
        }

    /** Each inherited digital family uses its approved native controls and separately authored orientations. */
    fun resolveStandalone(
        native: NativeControlInventory,
        safe: PivotArcBounds,
        viewport: PivotArcBounds,
        landscape: Boolean,
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
            } else if (snes) {
                400f
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
        val nativeSeats =
            when {
                gba -> if (landscape) landscapeNative else portraitNative
                snes -> if (landscape) snesLandscape else snesPortrait
                noSelect -> if (landscape) segaLandscape else segaPortrait
                else -> if (landscape) digitalTwoLandscape else digitalTwoPortrait
            }
        val programSeats =
            when {
                gba -> if (landscape) landscapeProgram else portraitProgram
                snes -> if (landscape) snesProgramLandscape else snesProgramPortrait
                else -> if (landscape) digitalTwoProgramLandscape else digitalTwoProgramPortrait
            }
        val programmable = programSeats.mapValues { bounds(it.value) }
        val controls = nativeSeats.mapValues { (key, seat) -> bounds(seat, menu = key == "Menu") }
        if (controls.keys != native.controls.map { it.key }.toSet()) return null
        val all = controls.values + programmable.values
        if (all.any { !it.inside(panel) || it.intersects(viewport.inset(1.5f)) } ||
            all.indices.any { index ->
                all.drop(index + 1).any { all[index].inset(0.7f).intersects(it.inset(0.7f)) }
            }
        ) {
            return null
        }
        val visuals =
            nativeSeats.mapValues { (id, seat) ->
                val size =
                    if (id == "D") {
                        76f
                    } else if (id == "A" || id == "B") {
                        44f
                    } else {
                        39f
                    }
                PivotArcSize(minOf(size, seat.size), minOf(size, seat.size))
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
                visuals = programmable.mapValues { PivotArcSize(45f, 30f) },
                scale = 1f,
                side = GripArcSide.RIGHT,
            )
        return GripArcCombinedPlacement(dock, controls, visuals, seats, native, native.inputIds)
    }
}
