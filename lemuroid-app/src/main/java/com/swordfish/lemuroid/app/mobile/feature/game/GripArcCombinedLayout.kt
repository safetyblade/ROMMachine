package com.swordfish.lemuroid.app.mobile.feature.game

import com.swordfish.lemuroid.lib.library.SystemID

/** Resolved together, in root-local dp. No dependency on Standard group measurements. */
internal data class GripArcCombinedPlacement(
    val dock: GripArcPlacement,
    val controls: Map<String, PivotArcBounds>,
)

internal object GripArcCombinedLayout {
    fun controlsFor(system: SystemID): List<String>? =
        when (system) {
            SystemID.GB, SystemID.GBC, SystemID.NGC -> listOf("D", "A", "B", "Select", "Start", "Menu")
            SystemID.GBA -> listOf("D", "A", "B", "L", "R", "Select", "Start", "Menu")
            SystemID.SNES -> listOf("D", "A", "B", "X", "Y", "L", "R", "Select", "Start", "Menu")
            SystemID.PSP -> listOf("D", "A", "B", "X", "Y", "L", "R", "Select", "Start", "Menu", "AnalogL")
            SystemID.PSX, SystemID.PLAYSTATION_2 ->
                listOf("D", "A", "B", "X", "Y", "L", "R", "L2", "R2", "Select", "Start", "Menu", "AnalogL", "AnalogR")
            SystemID.GAMECUBE ->
                listOf("D", "A", "B", "X", "Y", "L", "R", "R2", "Start", "Menu", "AnalogL", "AnalogR")
            SystemID.DREAMCAST ->
                listOf("D", "A", "B", "X", "Y", "L", "R", "Start", "Menu", "AnalogL")
            else -> null
        }

    fun resolve(
        manifest: GripArcManifest,
        system: SystemID,
        safe: PivotArcBounds,
        viewport: PivotArcBounds,
        side: GripArcSide,
        dx: Float,
        dy: Float,
        analogCount: Int? = null,
    ): GripArcCombinedPlacement? {
        if (!safe.isFiniteArea() || !viewport.isFiniteArea()) return null
        val active =
            controlsFor(system)?.filter {
                when (it) {
                    "AnalogL" -> analogCount == null || analogCount >= 1
                    "AnalogR" -> analogCount == null || analogCount >= 2
                    else -> true
                }
            } ?: return null
        // Preserve the player's side. The UI can explicitly request the opposite side for this session.
        for ((hand, x, y) in listOf(Triple(side, dx, dy), Triple(side, 0f, 0f)).distinct()) {
            for (dockScale in listOf(0.84f, 0.80f, 0.76f, 0.72f)) {
                val dock =
                    (
                        GripArcPositionSolver.resolve(
                            manifest, safe, viewport, emptyList(), hand, x, y, dockScale,
                        ) as? GripArcFit.Fits
                    )?.placement
                        ?: continue
                val reserved =
                    GripArcPositionSolver.housingFootprint(dock.housing, dock.scale, hand) + dock.targets.values
                val top = maxOf(safe.top, viewport.bottom + 8f)
                val rail = active.filter { it in setOf("L", "R", "L2", "R2", "Select", "Start", "Menu") }
                // A second rail row is allowed on narrow devices, with 48dp targets throughout.
                val columns = ((safe.width + 4f) / 52f).toInt().coerceAtLeast(1)
                val rows = (rail.size + columns - 1) / columns
                val arm = GripArcPositionSolver.housingFootprint(dock.housing, dock.scale, hand).first()
                val armLeft = if (hand == GripArcSide.RIGHT) arm.left else safe.left + safe.right - arm.right
                // Prefer the open inside corner. Narrow devices can use the upper area above the arm.
                for (faceX in listOf(armLeft - 80f, safe.right - 78f).distinct()) {
                    for (mainSize in listOf(104f, 96f)) {
                        val targets = linkedMapOf<String, PivotArcBounds>()

                        fun put(
                            key: String,
                            cx: Float,
                            cy: Float,
                            w: Float,
                            h: Float = w,
                        ) {
                            val mirroredX = if (hand == GripArcSide.LEFT) safe.left + safe.right - cx else cx
                            targets[key] = PivotArcBounds(mirroredX - w / 2, cy - h / 2, mirroredX + w / 2, cy + h / 2)
                        }
                        rail.forEachIndexed { i, key ->
                            val count = minOf(columns, rail.size - (i / columns) * columns)
                            put(
                                key,
                                safe.centerX + (i % columns - (count - 1) / 2f) * 52f,
                                top + 24f + (i / columns) * 52f,
                                48f,
                            )
                        }
                        val fieldTop = top + rows * 52f + 8f
                        val faceY = fieldTop + 74f
                        put("D", safe.left + mainSize / 2, fieldTop + mainSize / 2, mainSize)
                        // Preserve each console's established face compass, regardless of dock side.
                        val faces =
                            when (system) {
                                SystemID.GAMECUBE -> listOf("X", "A", "B", "Y")
                                // Native Dreamcast compass: B east, A south, X west, Y north.
                                SystemID.DREAMCAST -> listOf("B", "A", "X", "Y")
                                else -> listOf("A", "B", "Y", "X")
                            }
                        val offsets = listOf(52f to 0f, 0f to 52f, -52f to 0f, 0f to -52f)
                        if ("X" in active) {
                            faces.zip(offsets).forEach { (key, offset) ->
                                put(
                                    key,
                                    faceX + (if (hand == GripArcSide.LEFT) -offset.first else offset.first),
                                    faceY + offset.second,
                                    48f,
                                )
                            }
                        } else {
                            put("A", faceX + (if (hand == GripArcSide.LEFT) -26f else 26f), fieldTop + 36f, 48f)
                            put("B", faceX + (if (hand == GripArcSide.LEFT) 26f else -26f), fieldTop + 66f, 48f)
                        }
                        val analogY = fieldTop + 148f + 12f + 40f
                        if ("AnalogL" in active) put("AnalogL", safe.left + 44f, analogY, 80f)
                        if ("AnalogR" in active) put("AnalogR", safe.left + 136f, analogY, 80f)
                        val boxes = targets.values.toList()
                        if (boxes.any {
                                !it.inside(safe) || it.intersects(viewport.inset(4f)) ||
                                    reserved.any {
                                            r ->
                                        it.intersects(r.inset(4f))
                                    }
                            }
                        ) {
                            continue
                        }
                        if (boxes.indices.any {
                                    i ->
                                (i + 1 until boxes.size).any { j -> boxes[i].intersects(boxes[j].inset(2f)) }
                            }
                        ) {
                            continue
                        }
                        return GripArcCombinedPlacement(dock, targets)
                    }
                }
            }
        }
        return null
    }
}
