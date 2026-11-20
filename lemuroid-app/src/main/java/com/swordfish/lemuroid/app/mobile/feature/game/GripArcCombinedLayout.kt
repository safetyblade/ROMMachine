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
    // Measured Standard constellations take priority over dock-first free-space packing.
    // Each entire hand group is translated together; console-specific identities stay fixed.
    private val closeFitOffsets = listOf(0f, 8f, 16f, 24f)

    internal fun closeFit(
        control: PivotArcBounds,
        anchor: PivotArcBounds,
        scale: Float,
        shiftX: Float,
        shiftY: Float,
    ): PivotArcBounds {
        val x = anchor.centerX + (control.centerX - anchor.centerX) * scale + shiftX
        val y = anchor.centerY + (control.centerY - anchor.centerY) * scale + shiftY
        val width = maxOf(48.1f, control.width * scale)
        val height = maxOf(48.1f, control.height * scale)
        return PivotArcBounds(x - width / 2f, y - height / 2f, x + width / 2f, y + height / 2f)
    }

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
        val baseline = initialNativeConstellation(native, safe, viewport) ?: return null
        return resolve(
            manifest, native, native.inputIds, baseline, safe, viewport, side, dx, dy,
            fallbackToNativeBaseline = false,
        )
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
        fallbackToNativeBaseline: Boolean = true,
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
            for (up in closeFitOffsets) for (inward in closeFitOffsets) {
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
                    controls[spec.key] = closeFit(r, anchor, 0.90f, shift, -up)
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
        if (!fallbackToNativeBaseline) return null
        val baseline = initialNativeConstellation(native, safe, viewport) ?: return null
        return resolve(
            manifest, native, observedInputIds, baseline, safe, viewport, side, dx, dy,
            fallbackToNativeBaseline = false,
        )
    }

    /**
     * First-frame constellation: positions derive from the exact native inventory,
     * using familiar shoulder / face / directional / analog / utility groups.
     * No row-by-row arbitrary packing; unsupported compact decks fail safely.
     * Live native measurements replace this provisional geometry when available.
     */
    internal fun initialNativeConstellation(
        native: NativeControlInventory,
        safe: PivotArcBounds,
        viewport: PivotArcBounds,
    ): Map<String, PivotArcBounds>? {
        val deckTop = maxOf(safe.top + 24f, viewport.bottom + 18f)
        val deckHeight = safe.bottom - deckTop
        if (deckHeight < 280f || safe.width < 290f) return null
        val width = safe.width
        val left = safe.left + width * 0.19f
        val right = safe.right - width * 0.19f
        val middle = safe.centerX
        val gameCube = native.controller == TouchControllerID.GAMECUBE

        fun box(x: Float, fraction: Float, diameter: Float): PivotArcBounds {
            val y = deckTop + deckHeight * fraction
            return PivotArcBounds(
                x - diameter / 2f, y - diameter / 2f,
                x + diameter / 2f, y + diameter / 2f,
            )
        }

        return native.controls.associate { spec ->
            val seat = spec.seat
            val rect =
                when {
                    seat == "dpad" -> box(left, if (gameCube) 0.76f else 0.49f, 112f)
                    seat.startsWith("face-") -> box(right, 0.49f, if (seat == "face-ab") 116f else 132f)
                    seat == "analog-left" -> box(left + 18f, if (gameCube) 0.49f else 0.76f, 70f)
                    seat == "analog-right" -> box(right - 18f, 0.76f, 70f)
                    seat.startsWith("shoulder-L") || seat == "aux-L" ->
                        box(safe.left + 30f + if (seat.endsWith("2")) 60f else 0f, 0.10f, 48f)
                    seat.startsWith("shoulder-R") || seat == "aux-Z" ->
                        box(safe.right - 30f - if (seat.endsWith("2")) 60f else 0f, 0.10f, 48f)
                    seat == "select" -> box(middle - 50f, 0.25f, 48f)
                    seat == "start" -> box(middle + 50f, 0.25f, 48f)
                    seat == "menu" -> box(middle, 0.89f, 48f)
                    else -> return null
                }
            seat to rect
        }
    }


}
