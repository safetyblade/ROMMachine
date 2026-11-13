package com.swordfish.lemuroid.app.mobile.feature.game

import com.swordfish.lemuroid.lib.library.SystemID

internal enum class CompositeDeckFailure {
    UNSUPPORTED_CONTROLLER,
    PORTRAIT_REQUIRED,
    INVALID_GEOMETRY,
    INSUFFICIENT_SAFE_SPACE,
}

internal sealed interface CompositeDeckResolution {
    data class Fits(val placement: CompositeControlDeckPlacement) : CompositeDeckResolution

    data class Unavailable(val failure: CompositeDeckFailure, val reason: String) : CompositeDeckResolution
}

/** Two rectangles describe one notched island; the renderer traces their shared contour. */
internal data class PivotIslandShape(val upper: PivotArcBounds, val lower: PivotArcBounds) {
    val regions get() = listOf(upper, lower)

    companion object {
        const val OUTLINE_WIDTH_DP = 2f
    }
}

internal data class CompositeControlDeckPlacement(
    val viewport: PivotArcBounds,
    val safeDeck: PivotArcBounds,
    val dock: GripArcPlacement,
    val dockFootprint: List<PivotArcBounds>,
    val pivot: PivotArcPlacement,
    val futureFaces: Map<String, PivotArcBounds>,
    val island: PivotIslandShape,
    val wordmark: PivotArcBounds? = null,
)

/** Combined-only, dock-first resolution. Solo layouts and saved profiles are never modified. */
internal object CompositeControlDeckSolver {
    private const val GAP = 8f
    private const val EDGE = 6f

    // The housing renderer uses a centered 2dp outline. Reserve its painted edge too.
    private const val OUTLINE = PivotIslandShape.OUTLINE_WIDTH_DP / 2f
    private val utilities = setOf("Select", "Start", "Menu")

    fun resolve(
        manifest: GripArcManifest,
        baseline: PivotArcBaseline,
        system: SystemID,
        safe: PivotArcBounds,
        viewport: PivotArcBounds,
        side: GripArcSide,
        gripDx: Float = 0f,
        gripDy: Float = 0f,
        pivotDx: Float = 0f,
        pivotDy: Float = 0f,
        landscape: Boolean = false,
    ): CompositeDeckResolution {
        val active =
            PivotArcLayoutProfile.activeFor(system)
                ?: return unavailable(
                    CompositeDeckFailure.UNSUPPORTED_CONTROLLER,
                    "PivotArc + GripArc is not mapped for this controller.",
                )
        if (landscape) {
            return unavailable(
                CompositeDeckFailure.PORTRAIT_REQUIRED,
                "PivotArc + GripArc requires portrait orientation.",
            )
        }
        if (!safe.isFiniteArea() || !viewport.isFiniteArea() ||
            (active + listOf("X", "Y")).any { it !in baseline.buttonCenters }
        ) {
            return unavailable(
                CompositeDeckFailure.INVALID_GEOMETRY,
                "PivotArc + GripArc geometry is unavailable or invalid.",
            )
        }
        val safeDeck = PivotArcBounds(safe.left, maxOf(safe.top, viewport.bottom + GAP), safe.right, safe.bottom)
        val attempts = listOf(Triple(side, gripDx, gripDy), Triple(side, 0f, 0f)).distinct()
        for ((hand, dx, dy) in attempts) for (dockScale in listOf(1f)) {
            val dockFit =
                GripArcPositionSolver.resolve(
                    manifest,
                    safe,
                    viewport,
                    emptyList(),
                    hand,
                    dx,
                    dy,
                    dockScale,
                )
            val dock = (dockFit as? GripArcFit.Fits)?.placement ?: continue
            val arms = GripArcPositionSolver.housingFootprint(dock.housing, dock.scale, hand)
            val reserved = arms + dock.targets.values
            for (scale in listOf(1.08f, 1f, 0.9f, 0.8f, 0.7f)) {
                val source =
                    (active.filterNot { it in utilities } + listOf("X", "Y")).distinct().associateWith { key ->
                        val spec = baseline.buttonCenters.getValue(key)
                        val w = maxOf(48f, spec.touch.w * scale)
                        val h = maxOf(48f, spec.touch.h * scale)
                        PivotArcBounds(
                            spec.center.x * scale - w / 2,
                            spec.center.y * scale - h / 2,
                            spec.center.x * scale + w / 2,
                            spec.center.y * scale + h / 2,
                        )
                    }.toMutableMap()
                val a = source.getValue("A")
                val b = source.getValue("B")
                source["HoldA"] = PivotArcBounds(a.centerX - 24f, a.bottom + GAP, a.centerX + 24f, a.bottom + GAP + 48f)
                source["HoldB"] = PivotArcBounds(b.left - GAP - 48f, b.centerY - 24f, b.left - GAP, b.centerY + 24f)
                val hull = hull(source.values)
                val railWidth =
                    active.filter {
                        it in utilities
                    }.sumOf { baseline.buttonCenters.getValue(it).touch.w.toDouble() }.toFloat() + 2 * GAP
                var cursor = hull.centerX - railWidth / 2
                for (key in active.filter { it in utilities }) {
                    val w = maxOf(48f, baseline.buttonCenters.getValue(key).touch.w)
                    source[key] = PivotArcBounds(cursor, hull.bottom + GAP, cursor + w, hull.bottom + GAP + 48f)
                    cursor += w + GAP
                }
                val whole = hull(source.values)
                // Candidate edges derive from safe space and individual reserved material/targets.
                // Translation applies to the complete approved constellation, never individual slots.
                val xs = listOf(safe.left + EDGE + OUTLINE - whole.left, safe.right - EDGE - OUTLINE - whole.right)
                val ys =
                    listOf(safeDeck.top + EDGE + OUTLINE - whole.top, arms[1].top - GAP - EDGE - OUTLINE - whole.bottom)
                for (x in xs) for (y in ys) for (requested in listOf(true, false)) {
                    val shiftX = x + if (requested && pivotDx.isFinite()) pivotDx else 0f
                    val shiftY = y + if (requested && pivotDy.isFinite()) pivotDy else 0f
                    val positions =
                        source.mapValues { (_, r) ->
                            val moved =
                                PivotArcBounds(r.left + shiftX, r.top + shiftY, r.right + shiftX, r.bottom + shiftY)
                            if (hand == GripArcSide.RIGHT) {
                                moved
                            } else {
                                PivotArcBounds(
                                    safe.left + safe.right - moved.right,
                                    moved.top,
                                    safe.left + safe.right - moved.left,
                                    moved.bottom,
                                )
                            }
                        }
                    val boxes = positions.values.toList()
                    if (boxes.any {
                            !it.inside(safeDeck) ||
                                reserved.any {
                                        r ->
                                    it.inset(GAP / 2).intersects(r)
                                }
                        }
                    ) {
                        continue
                    }
                    if (boxes.indices.any {
                                i ->
                            (i + 1 until boxes.size).any {
                                    j ->
                                boxes[i].inset(GAP / 2).intersects(boxes[j].inset(GAP / 2))
                            }
                        }
                    ) {
                        continue
                    }
                    val coreBounds = hull(boxes).inset(EDGE)
                    val bounds =
                        if (hand == GripArcSide.RIGHT) {
                            coreBounds.copy(right = maxOf(coreBounds.right, arms[0].left + 16f))
                        } else {
                            coreBounds.copy(left = minOf(coreBounds.left, arms[0].right - 16f))
                        }
                    val notchY = (arms[0].top - GAP).coerceIn(bounds.top, bounds.bottom)
                    val innerLeft =
                        if (hand == GripArcSide.LEFT) {
                            maxOf(
                                bounds.left,
                                arms[0].right + EDGE,
                            )
                        } else {
                            bounds.left
                        }
                    val innerRight =
                        if (hand == GripArcSide.RIGHT) {
                            minOf(
                                bounds.right,
                                arms[0].left - EDGE,
                            )
                        } else {
                            bounds.right
                        }
                    val island =
                        PivotIslandShape(
                            PivotArcBounds(bounds.left, bounds.top, bounds.right, notchY),
                            PivotArcBounds(innerLeft, notchY, innerRight, bounds.bottom),
                        )
                    if (island.regions.any {
                            it.width <= 0f || it.height <= 0f || !it.inset(OUTLINE).inside(safeDeck) ||
                                reserved.any {
                                        r ->
                                    it.inset(OUTLINE).intersects(r)
                                }
                        }
                    ) {
                        continue
                    }
                    // Reject a notch that cuts through any actual or future touch region.
                    if (boxes.map { it.inset(EDGE) }.any { r ->
                            island.regions.none { r.inside(it) } &&
                                !(r.left >= innerLeft && r.right <= innerRight && r.inside(bounds))
                        }
                    ) {
                        continue
                    }
                    val future = positions.filterKeys { it !in active && it in setOf("X", "Y") }
                    val controls = positions - future.keys
                    val visuals =
                        controls.mapValues { (key, _) ->
                            if (key.startsWith("Hold")) {
                                PivotArcSize(36f, 36f)
                            } else {
                                baseline.buttonCenters.getValue(key).visual.let {
                                    val factor = if (key in utilities) 1f else scale
                                    PivotArcSize(it.w * factor, it.h * factor)
                                }
                            }
                        }
                    val brand =
                        PivotArcBounds(
                            island.upper.right - 96f,
                            island.upper.top + 10f,
                            island.upper.right - 8f,
                            island.upper.top + 34f,
                        ).takeIf { candidate ->
                            candidate.inside(island.upper) &&
                                (boxes + reserved).none { candidate.inset(4f).intersects(it) }
                        }
                    return CompositeDeckResolution.Fits(
                        CompositeControlDeckPlacement(
                            viewport,
                            safeDeck,
                            dock,
                            arms,
                            PivotArcPlacement(controls, visuals, shiftX, shiftY, scale),
                            future,
                            island,
                            brand,
                        ),
                    )
                }
            }
        }
        return unavailable(
            CompositeDeckFailure.INSUFFICIENT_SAFE_SPACE,
            "The joint PivotArc + GripArc deck cannot fit on your selected side, including its default position.",
        )
    }

    private fun hull(rects: Collection<PivotArcBounds>) =
        PivotArcBounds(
            rects.minOf {
                it.left
            },
            rects.minOf { it.top },
            rects.maxOf { it.right },
            rects.maxOf { it.bottom },
        )

    private fun unavailable(
        failure: CompositeDeckFailure,
        reason: String,
    ) = CompositeDeckResolution.Unavailable(
        failure,
        "$reason Standard is available for this session; your saved GripArc settings are preserved.",
    )
}
