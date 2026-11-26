package com.swordfish.lemuroid.app.mobile.feature.game

import com.swordfish.touchinput.radial.settings.NativeControlInventory
import com.swordfish.touchinput.radial.settings.TouchControllerID

/**
 * The first full-chassis GripArc controller layout. Native GBA keys are
 * placed deliberately inside the common physical housing. The old Standard
 * measurements are not recycled: they belong to a different controller.
 *
 * This changes geometry only. PadKit owns input, hold, key identities and
 * per-cartridge programming. Joint/PivotArc are separate immutable modules.
 */
internal object GbaGripArcLayout {
    fun resolve(
        native: NativeControlInventory,
        observedInputIds: Set<String>,
        safe: PivotArcBounds,
        viewport: PivotArcBounds,
        side: GripArcSide,
    ): GripArcCombinedPlacement? {
        if (native.controller != TouchControllerID.GBA ||
            !native.matchesNativeInputs(observedInputIds) ||
            !safe.isFiniteArea() || !viewport.isFiniteArea()
        ) {
            return null
        }
        val top = maxOf(safe.top + 16f, viewport.bottom + 16f)
        val panel = PivotArcBounds(safe.left + 12f, top, safe.right - 12f, safe.bottom - 16f)
        if (panel.width < 300f || panel.height < 390f) return null

        fun target(
            x: Float,
            y: Float,
            // Maintain >48dp after IEEE-754 coordinate subtraction.
            width: Float = 48.5f,
            height: Float = 48.5f,
        ): PivotArcBounds {
            val actualX = if (side == GripArcSide.LEFT) 1f - x else x
            val cx = panel.left + panel.width * actualX
            val cy = panel.top + panel.height * y
            return PivotArcBounds(cx - width / 2f, cy - height / 2f, cx + width / 2f, cy + height / 2f)
        }

        val design =
            mapOf(
                "L" to target(0.12f, 0.10f),
                "R" to target(0.88f, 0.10f),
                "Select" to target(0.39f, 0.22f),
                "Start" to target(0.58f, 0.22f),
                "D" to target(0.24f, 0.47f, 96f, 96f),
                "B" to target(0.62f, 0.51f),
                "A" to target(0.80f, 0.41f),
                "Menu" to target(0.27f, 0.82f),
            )
        val requestedKeys = native.controls.map { it.key }.toSet()
        if (design.keys != requestedKeys) return null

        val controls = design
        val visuals =
            native.controls.associate { spec ->
                val hit = controls.getValue(spec.key)
                val factor =
                    when (spec.key) {
                        "D" -> 0.96f
                        "A", "B" -> 0.88f
                        else -> 0.78f
                    }
                spec.key to PivotArcSize(hit.width * factor, hit.height * factor)
            }
        val seats =
            native.controls.groupBy { it.seat }.mapValues { (_, specs) ->
                val rects = specs.map { controls.getValue(it.key) }
                PivotArcBounds(
                    rects.minOf { it.left },
                    rects.minOf { it.top },
                    rects.maxOf { it.right },
                    rects.maxOf { it.bottom },
                )
            }

        // G1–G4 form a real two-column control bank instead of four labels
        // scattered around the old corner tower.
        val columns = listOf(0.64f, 0.83f)
        val programmable =
            mapOf(
                1 to target(columns[0], 0.72f),
                2 to target(columns[1], 0.72f),
                3 to target(columns[0], 0.86f),
                4 to target(columns[1], 0.86f),
            )
        val all = controls.values.toList() + programmable.values
        if (all.any { !it.inside(safe) || it.intersects(viewport.inset(4f)) } ||
            all.indices.any { a ->
                (a + 1 until all.size).any { b -> all[a].inset(1f).intersects(all[b].inset(1f)) }
            }
        ) {
            return null
        }
        val minX = programmable.values.minOf { it.left }
        val minY = programmable.values.minOf { it.top }
        val maxX = programmable.values.maxOf { it.right }
        val maxY = programmable.values.maxOf { it.bottom }
        val dock =
            GripArcPlacement(
                housing = PivotArcBounds(minX, minY, maxX, maxY),
                targets = programmable,
                visuals = (1..4).associateWith { PivotArcSize(36f, 19f) },
                scale = 1f,
                side = side,
            )
        return GripArcCombinedPlacement(dock, controls, visuals, seats, native, observedInputIds)
            .takeIf { it.complete }
    }
}
