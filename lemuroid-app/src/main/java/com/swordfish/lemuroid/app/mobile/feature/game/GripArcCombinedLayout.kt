package com.swordfish.lemuroid.app.mobile.feature.game

import com.swordfish.touchinput.radial.settings.NativeControlInventory
import com.swordfish.touchinput.radial.settings.NativeControlKind
import com.swordfish.touchinput.radial.settings.TouchControllerID

/** Native inventory, painted seats, visuals and input bounds resolve together. */
internal data class GripArcCombinedPlacement(
    val dock: GripArcPlacement,
    val controls: Map<String, PivotArcBounds>,
    val visuals: Map<String, PivotArcSize>,
    val seats: Map<String, PivotArcBounds>,
    val native: NativeControlInventory,
    val observedInputIds: Set<String>,
) {
    val complete: Boolean get() =
        controls.keys == native.controls.map { it.key }.toSet() && visuals.keys == controls.keys &&
            native.matchesNativeInputs(observedInputIds)
}

internal object GripArcCombinedLayout {
    fun hasMeasurements(
        native: NativeControlInventory,
        measured: Map<String, PivotArcBounds>,
    ): Boolean = native.controls.all { measured[it.seat]?.isFiniteArea() == true }

    /** Native inventory supplies a complete initial layout before live artwork measurements arrive. */
    fun resolveDockFirst(
        manifest: GripArcManifest,
        native: NativeControlInventory,
        safe: PivotArcBounds,
        viewport: PivotArcBounds,
        side: GripArcSide,
        dx: Float,
        dy: Float,
    ): GripArcCombinedPlacement? {
        if (!safe.isFiniteArea() || !viewport.isFiniteArea()) return null
        return reflow(manifest, native, native.inputIds, safe, viewport, side, dx, dy)
    }

    fun resolve(
        manifest: GripArcManifest,
        native: NativeControlInventory,
        observedInputIds: Set<String>,
        measured: Map<String, PivotArcBounds>,
        safe: PivotArcBounds,
        viewport: PivotArcBounds,
        side: GripArcSide,
        dx: Float,
        dy: Float,
    ): GripArcCombinedPlacement? {
        if (!safe.isFiniteArea() || !viewport.isFiniteArea() ||
            !native.matchesNativeInputs(observedInputIds) || !hasMeasurements(native, measured)
        ) {
            return null
        }
        val primaryLeft = if (native.controller == TouchControllerID.GAMECUBE) "analog-left" else "dpad"
        val leftAnchor = measured[primaryLeft] ?: return null
        val faceSeat = native.controls.firstOrNull { it.kind == NativeControlKind.FACE }?.seat ?: return null
        val rightAnchor = measured.getValue(faceSeat)
        val bases =
            native.controls.associate { spec ->
                val r = measured.getValue(spec.seat)
                val base =
                    if (spec.kind == NativeControlKind.FACE) {
                        val diameter = minOf(r.width, r.height)
                        val size = maxOf(48f, diameter * 0.28f)
                        val x = r.centerX + spec.faceX * maxOf(56f, diameter * 0.31f)
                        val y = r.centerY + spec.faceY * maxOf(56f, diameter * 0.31f)
                        PivotArcBounds(x - size / 2, y - size / 2, x + size / 2, y + size / 2)
                    } else {
                        r
                    }
                spec.key to base
            }
        for ((dockX, dockY) in listOf(dx to dy, 0f to 0f).distinct()) {
            val fit = GripArcPositionSolver.resolve(manifest, safe, viewport, emptyList(), side, dockX, dockY)
            val dock = (fit as? GripArcFit.Fits)?.placement ?: continue
            val reserved = GripArcPositionSolver.housingFootprint(dock.housing, dock.scale, side) + dock.targets.values
            // Preserve the selected native constellation; no replacement shoulder/face grid.
            for (up in listOf(0f, 16f, 32f, 48f, 64f, 80f, 96f)) for (inward in listOf(10f, 16f, 24f)) {
                val controls = linkedMapOf<String, PivotArcBounds>()
                val visuals = linkedMapOf<String, PivotArcSize>()
                native.controls.forEach { spec ->
                    val r = bases.getValue(spec.key)
                    val anchor = if (spec.leftHand) leftAnchor else rightAnchor
                    val shift =
                        if (spec.leftHand == (side == GripArcSide.LEFT)) {
                            if (side == GripArcSide.LEFT) inward else -inward
                        } else {
                            0f
                        }
                    val x = anchor.centerX + (r.centerX - anchor.centerX) * 0.90f + shift
                    val y = anchor.centerY + (r.centerY - anchor.centerY) * 0.90f - up
                    val w = maxOf(48.1f, r.width * 0.90f)
                    val h = maxOf(48.1f, r.height * 0.90f)
                    controls[spec.key] = PivotArcBounds(x - w / 2, y - h / 2, x + w / 2, y + h / 2)
                    visuals[spec.key] = PivotArcSize(r.width * 0.90f, r.height * 0.90f)
                }
                // Keep each native hand constellation intact when its painted edge needs a small safe-inset correction.
                var edgeFits = true
                for (left in listOf(true, false)) {
                    val keys = native.controls.filter { it.leftHand == left }.map { it.key }
                    val group = keys.map { controls.getValue(it) }
                    val min = group.minOf { it.left }
                    val max = group.maxOf { it.right }
                    val correction =
                        when {
                            min - 3f < safe.left -> safe.left + 3f - min
                            max + 3f > safe.right -> safe.right - 3f - max
                            else -> 0f
                        }
                    val dockShift = if (left == (side == GripArcSide.LEFT)) inward else 0f
                    if (kotlin.math.abs(correction) + dockShift > 24f) {
                        edgeFits = false
                        break
                    }
                    keys.forEach {
                            key ->
                        controls[key] =
                            controls.getValue(key).let { r ->
                                r.copy(left = r.left + correction, right = r.right + correction)
                            }
                    }
                }
                if (!edgeFits) continue
                if (controls.keys != native.controls.map { it.key }.toSet()) return null
                val boxes = controls.values.toList()
                if (boxes.any {
                        !it.inside(safe) || it.intersects(viewport.inset(4f)) ||
                            reserved.any {
                                    r ->
                                it.inset(4f).intersects(r)
                            }
                    }
                ) {
                    continue
                }
                if (boxes.indices.any {
                            i ->
                        (i + 1 until boxes.size).any {
                                j ->
                            boxes[i].inset(1f).intersects(boxes[j].inset(1f))
                        }
                    }
                ) {
                    continue
                }
                val seats =
                    native.controls.groupBy { it.seat }.mapValues { (_, specs) ->
                        val parts =
                            specs.map { spec ->
                                val hit = controls.getValue(spec.key)
                                val art = visuals.getValue(spec.key)
                                PivotArcBounds(
                                    hit.centerX - art.w / 2,
                                    hit.centerY - art.h / 2,
                                    hit.centerX + art.w / 2,
                                    hit.centerY + art.h / 2,
                                )
                            }
                        PivotArcBounds(
                            parts.minOf { it.left },
                            parts.minOf { it.top },
                            parts.maxOf { it.right },
                            parts.maxOf { it.bottom },
                        )
                    }
                // Reserve the actual native paired/quad well artwork as well as individual hit regions.
                if (seats.values.any {
                        !it.inset(3f).inside(safe) || it.inset(3f).intersects(viewport) ||
                            reserved.any {
                                    r ->
                                it.inset(3f).intersects(r)
                            }
                    }
                ) {
                    continue
                }
                // Use the same margins as the native hardware-seat painter, so it cannot silently drop a well.
                if (seats.any { (key, r) ->
                        seats.any { (other, bounds) ->
                            other != key && r.inset(3f).intersects(bounds.inset(4f))
                        }
                    }
                ) {
                    continue
                }
                return GripArcCombinedPlacement(
                    dock,
                    controls,
                    visuals,
                    seats,
                    native,
                    observedInputIds.toSet(),
                ).takeIf { it.complete }
            }
        }
        return reflow(manifest, native, observedInputIds, safe, viewport, side, dx, dy)
    }

    /** Reserve the dock before sizing native seats. Live Standard artwork is only a preference. */
    private fun reflow(
        manifest: GripArcManifest,
        native: NativeControlInventory,
        observedInputIds: Set<String>,
        safe: PivotArcBounds,
        viewport: PivotArcBounds,
        side: GripArcSide,
        dx: Float,
        dy: Float,
    ): GripArcCombinedPlacement? {
        val dock =
            listOf(dx to dy, 0f to 0f).distinct().firstNotNullOfOrNull { (x, y) ->
                (
                    GripArcPositionSolver.resolve(
                        manifest, safe, viewport, emptyList(), side, x, y,
                    ) as? GripArcFit.Fits
                )?.placement
            } ?: return null
        val reserved = GripArcPositionSolver.housingFootprint(dock.housing, dock.scale, side) + dock.targets.values
        val top = maxOf(safe.top, viewport.bottom + 8f)
        val groups =
            native.controls.groupBy { it.seat }.values.sortedBy { specs ->
                when (specs.first().kind) {
                    NativeControlKind.FACE -> 0
                    NativeControlKind.DPAD -> 1
                    NativeControlKind.ANALOG -> 2
                    NativeControlKind.SHOULDER -> 3
                    NativeControlKind.UTILITY -> 4
                }
            }
        for (artScale in listOf(1f, 0.85f, 0.7f)) {
            val controls = linkedMapOf<String, PivotArcBounds>()
            val visuals = linkedMapOf<String, PivotArcSize>()
            val seats = linkedMapOf<String, PivotArcBounds>()
            for (specs in groups) {
                val first = specs.first()
                val parts =
                    specs.associate { spec ->
                        val size =
                            when (spec.kind) {
                                NativeControlKind.DPAD -> maxOf(64f, 96f * artScale)
                                NativeControlKind.ANALOG -> maxOf(48f, 64f * artScale)
                                else -> 48f
                            }
                        val x = if (spec.kind == NativeControlKind.FACE) spec.faceX * 52f else 0f
                        val y = if (spec.kind == NativeControlKind.FACE) spec.faceY * 52f else 0f
                        spec.key to PivotArcBounds(x - size / 2, y - size / 2, x + size / 2, y + size / 2)
                    }
                val hull =
                    PivotArcBounds(
                        parts.values.minOf { it.left },
                        parts.values.minOf { it.top },
                        parts.values.maxOf { it.right },
                        parts.values.maxOf { it.bottom },
                    )
                val candidates = mutableListOf<Pair<Float, Float>>()
                var y = top + 4f
                while (y + hull.height + 4f <= safe.bottom) {
                    var x = safe.left + 4f
                    while (x + hull.width + 4f <= safe.right) {
                        candidates += x to y
                        x += 8f
                    }
                    y += 8f
                }
                val preferredX = if (first.leftHand) safe.left else safe.right - hull.width
                val preferredY =
                    top +
                        when (first.kind) {
                            NativeControlKind.SHOULDER -> 0f
                            NativeControlKind.DPAD, NativeControlKind.FACE -> 64f
                            NativeControlKind.ANALOG -> 192f
                            NativeControlKind.UTILITY -> 272f
                        }
                val origin =
                    candidates.sortedBy { (x, y) ->
                        kotlin.math.abs(x - preferredX) + kotlin.math.abs(y - preferredY)
                    }.firstOrNull { (x, y) ->
                        val seat = PivotArcBounds(x, y, x + hull.width, y + hull.height)
                        val correctHand =
                            first.kind == NativeControlKind.UTILITY ||
                                (if (first.leftHand) seat.centerX <= safe.centerX else seat.centerX >= safe.centerX)
                        correctHand && (reserved + seats.values).none { seat.inset(4f).intersects(it.inset(3f)) }
                    } ?: break
                specs.forEach { spec ->
                    val r = parts.getValue(spec.key)
                    controls[spec.key] =
                        PivotArcBounds(
                            r.left - hull.left + origin.first, r.top - hull.top + origin.second,
                            r.right - hull.left + origin.first, r.bottom - hull.top + origin.second,
                        )
                    visuals[spec.key] = PivotArcSize(r.width * artScale, r.height * artScale)
                }
                seats[first.seat] =
                    PivotArcBounds(
                        origin.first, origin.second, origin.first + hull.width, origin.second + hull.height,
                    )
            }
            if (controls.size == native.controls.size) {
                return GripArcCombinedPlacement(dock, controls, visuals, seats, native, observedInputIds.toSet())
                    .takeIf { it.complete }
            }
        }
        return null
    }
}
