package com.swordfish.lemuroid.app.mobile.feature.game

import android.content.Context
import com.swordfish.lemuroid.lib.library.SystemID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.max

/**
 * Candidate C data lives in assets/pivotarc/candidate-c-baseline.json. Coordinates
 * are versioned design INPUT, not fixed Compose percentages or new emulator IDs.
 * The only native system mapping here is which pre-existing PadKit keys are active.
 */
@Serializable
data class PivotArcSize(val w: Float, val h: Float)

@Serializable
data class PivotArcPoint(val x: Float, val y: Float)

@Serializable
data class PivotArcButtonSpec(
    val center: PivotArcPoint,
    val visual: PivotArcSize,
    val touch: PivotArcSize,
    val kind: String,
)

@Serializable
data class PivotArcBaseline(
    val schema: String,
    val buttonCenters: Map<String, PivotArcButtonSpec>,
    val profiles: Map<String, List<String>>,
)

object PivotArcLayoutProfile {
    // Conditional extensions are not active unless a console mapping is
    // complete. The shared solver and cradle accept all four without
    // hardcoded key counts or relocating the approved primary cluster.
    val optionalFourButtonExpansion = setOf("X", "Y", "L2", "R2")

    private val decoder = Json { ignoreUnknownKeys = true }

    fun parse(json: String): PivotArcBaseline {
        val baseline = decoder.decodeFromString(PivotArcBaseline.serializer(), json)
        require(baseline.schema == "cartdeck.pivotarc.candidate-c/0.7.0") {
            "Unknown PivotArc geometry version"
        }
        return baseline
    }

    fun load(context: Context): PivotArcBaseline =
        context.assets.open("pivotarc/candidate-c-baseline.json").bufferedReader().use {
            parse(it.readText())
        }

    fun activeFor(system: SystemID): List<String>? =
        when (system) {
            SystemID.GB, SystemID.GBC -> listOf("D", "A", "B", "Select", "Start", "Menu")
            SystemID.GBA -> listOf("D", "A", "B", "L", "R", "Select", "Start", "Menu")
            SystemID.SNES -> listOf("D", "A", "B", "X", "Y", "L", "R", "Select", "Start", "Menu")
            else -> null
        }
}

/** Coordinates in measured ROOT-local dp, not in screenshot pixels. */
data class PivotArcBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width get() = right - left
    val height get() = bottom - top
    val centerX get() = (left + right) / 2f
    val centerY get() = (top + bottom) / 2f

    fun inset(amount: Float): PivotArcBounds =
        PivotArcBounds(left - amount, top - amount, right + amount, bottom + amount)

    fun inside(
        other: PivotArcBounds,
        tolerance: Float = 0.05f,
    ): Boolean =
        left >= other.left - tolerance && right <= other.right + tolerance &&
            top >= other.top - tolerance && bottom <= other.bottom + tolerance

    fun intersects(other: PivotArcBounds): Boolean =
        left < other.right && right > other.left && top < other.bottom && bottom > other.top
}

data class PivotArcPlacement(
    val controls: Map<String, PivotArcBounds>,
    val visuals: Map<String, PivotArcSize>,
    val appliedDx: Float,
    val appliedDy: Float,
    val visualScale: Float = 1f,
)

sealed interface PivotArcResolution {
    data class Fits(val placement: PivotArcPlacement) : PivotArcResolution

    data class Unavailable(val reason: String) : PivotArcResolution
}

/**
 * Scale the entire Candidate C *gameplay* constellation before placing it.
 * Layout topology, PadKit identities and the separated utility rail are never
 * changed. The owner feedback rejects fixed miniature 44dp action art:
 * spacious phones get a larger manufactured PivotArc control set.
 *
 * If a device cannot safely fit a particular scale, attempt a smaller still-
 * usable preset, or report Unavailable so Standard stays available.
 */
object PivotArcPositionSolver {
    private const val REFERENCE_WIDTH = 393f
    private const val REFERENCE_HEIGHT = 852f
    private const val GAP = 8f
    private const val HOLD_TOUCH_SIZE = 48f
    private const val HOLD_VISUAL_SIZE = 28f
    private const val UTILITY_GAP = 9f
    private val utilities = setOf("Start", "Select", "Menu")
    private val preferredScales = listOf(1.28f, 1.18f, 1.08f, 1.0f)

    fun resolve(
        baseline: PivotArcBaseline,
        active: List<String>,
        safe: PivotArcBounds,
        viewport: PivotArcBounds,
        leftHanded: Boolean,
        requestedDx: Float,
        requestedDy: Float,
        isLandscape: Boolean,
    ): PivotArcResolution {
        if (isLandscape) {
            return PivotArcResolution.Unavailable("PivotArc currently requires portrait orientation.")
        }
        if (safe.width <= 0f || safe.height <= 0f || !requestedDx.isFinite() || !requestedDy.isFinite()) {
            return PivotArcResolution.Unavailable("Invalid measured display bounds or saved position.")
        }
        if (active.isEmpty() || active.any { it !in baseline.buttonCenters }) {
            return PivotArcResolution.Unavailable("Required system controls are not defined.")
        }
        val gameplay = active.filterNot { it in utilities }
        if (gameplay.isEmpty()) {
            return PivotArcResolution.Unavailable("No usable gameplay controls.")
        }
        val undersized =
            active.any {
                val target = baseline.buttonCenters.getValue(it).touch
                target.w < 48f || target.h < 48f
            }
        if (undersized) {
            return PivotArcResolution.Unavailable("PivotArc has a control smaller than 48dp.")
        }

        var lastFailure = "Not enough safe space for this PivotArc configuration."
        for (scale in preferredScales) {
            when (
                val result =
                    attemptPlacement(
                        baseline = baseline,
                        active = active,
                        gameplay = gameplay,
                        safe = safe,
                        viewport = viewport,
                        leftHanded = leftHanded,
                        requestedDx = requestedDx,
                        requestedDy = requestedDy,
                        scale = scale,
                    )
            ) {
                is PivotArcResolution.Fits -> return result
                is PivotArcResolution.Unavailable -> lastFailure = result.reason
            }
        }
        return PivotArcResolution.Unavailable(lastFailure)
    }

    private fun attemptPlacement(
        baseline: PivotArcBaseline,
        active: List<String>,
        gameplay: List<String>,
        safe: PivotArcBounds,
        viewport: PivotArcBounds,
        leftHanded: Boolean,
        requestedDx: Float,
        requestedDy: Float,
        scale: Float,
    ): PivotArcResolution {
        val shiftX = safe.centerX - REFERENCE_WIDTH / 2f
        val shiftY = safe.bottom - REFERENCE_HEIGHT
        val anchor = baseline.buttonCenters.getValue("D").center

        fun rectangleFor(key: String): PivotArcBounds {
            val button = baseline.buttonCenters.getValue(key)
            val centerX = anchor.x + (button.center.x - anchor.x) * scale + shiftX
            val centerY = anchor.y + (button.center.y - anchor.y) * scale + shiftY
            val halfWidth = button.touch.w * scale / 2f
            val halfHeight = button.touch.h * scale / 2f
            return PivotArcBounds(
                centerX - halfWidth,
                centerY - halfHeight,
                centerX + halfWidth,
                centerY + halfHeight,
            )
        }

        val normalControls = gameplay.associateWith(::rectangleFor)
        // Dedicated 48dp targets flank B and sit just below A. Neither
        // modifies the owner's original A/B/D/L/R anchors or PadKit IDs.
        val b = normalControls.getValue("B")
        val a = normalControls.getValue("A")
        val holds =
            mapOf(
                "HoldB" to
                    PivotArcBounds(
                        b.left - GAP - HOLD_TOUCH_SIZE,
                        b.centerY - HOLD_TOUCH_SIZE / 2f,
                        b.left - GAP,
                        b.centerY + HOLD_TOUCH_SIZE / 2f,
                    ),
                "HoldA" to
                    PivotArcBounds(
                        a.centerX - HOLD_TOUCH_SIZE / 2f,
                        a.bottom + GAP,
                        a.centerX + HOLD_TOUCH_SIZE / 2f,
                        a.bottom + GAP + HOLD_TOUCH_SIZE,
                    ),
            )
        val core = normalControls + holds
        val coreHull =
            PivotArcBounds(
                core.values.minOf { it.left },
                core.values.minOf { it.top },
                core.values.maxOf { it.right },
                core.values.maxOf { it.bottom },
            )

        // V1 trial utility bank: directly beneath the PivotArc gameplay
        // cluster, separate from frequent thumb actions and mirrored as a unit.
        // Standard mode does not use this solver or these positions.
        val utilitySpecs = active.filter { it in utilities }
        val utilityTotalWidth =
            utilitySpecs.sumOf { baseline.buttonCenters.getValue(it).touch.w.toDouble() }.toFloat() +
                (utilitySpecs.size - 1).coerceAtLeast(0) * UTILITY_GAP
        var cursor = coreHull.centerX - utilityTotalWidth / 2f
        val railY = coreHull.bottom + GAP
        val rail =
            utilitySpecs.associateWith { key ->
                val target = baseline.buttonCenters.getValue(key).touch
                PivotArcBounds(cursor, railY, cursor + target.w, railY + target.h)
                    .also { cursor += target.w + UTILITY_GAP }
            }
        val complete = core + rail
        val wholeHull =
            PivotArcBounds(
                complete.values.minOf { it.left },
                complete.values.minOf { it.top },
                complete.values.maxOf { it.right },
                complete.values.maxOf { it.bottom },
            )
        val minTop = max(safe.top, viewport.bottom + GAP)
        val minDx = safe.left - wholeHull.left
        val maxDx = safe.right - wholeHull.right
        val minDy = minTop - wholeHull.top
        val maxDy = safe.bottom - wholeHull.bottom
        if (minDx > maxDx || minDy > maxDy) {
            return PivotArcResolution.Unavailable(
                "Insufficient space for full-sized PivotArc controls outside the game viewport.",
            )
        }

        val dx = requestedDx.coerceIn(minDx, maxDx)
        val dy = requestedDy.coerceIn(minDy, maxDy)
        val moved =
            complete.mapValues { (_, rect) ->
                PivotArcBounds(
                    rect.left + dx,
                    rect.top + dy,
                    rect.right + dx,
                    rect.bottom + dy,
                )
            }
        val positions =
            if (leftHanded) {
                moved.mapValues { (_, rect) ->
                    PivotArcBounds(
                        safe.left + safe.right - rect.right,
                        rect.top,
                        safe.left + safe.right - rect.left,
                        rect.bottom,
                    )
                }
            } else {
                moved
            }
        for ((key, rect) in positions) {
            if (!rect.inside(safe)) {
                return PivotArcResolution.Unavailable("$key enters a device inset or gesture area.")
            }
            if (rect.intersects(viewport.inset(GAP))) {
                return PivotArcResolution.Unavailable("$key would obscure gameplay.")
            }
        }

        val entries = positions.entries.toList()
        for (i in entries.indices) {
            for (j in i + 1 until entries.size) {
                if (entries[i].value.inset(GAP / 2f).intersects(entries[j].value.inset(GAP / 2f))) {
                    return PivotArcResolution.Unavailable(
                        "${entries[i].key} and ${entries[j].key} lack the required clearance.",
                    )
                }
            }
        }

        val visuals =
            active.associateWith { key ->
                val source = baseline.buttonCenters.getValue(key).visual
                val factor = if (key in utilities) 1f else scale
                PivotArcSize(source.w * factor, source.h * factor)
            } + holds.keys.associateWith { PivotArcSize(HOLD_VISUAL_SIZE, HOLD_VISUAL_SIZE) }
        return PivotArcResolution.Fits(
            PivotArcPlacement(
                controls = positions,
                visuals = visuals,
                appliedDx = dx,
                appliedDy = dy,
                visualScale = scale,
            ),
        )
    }
}
