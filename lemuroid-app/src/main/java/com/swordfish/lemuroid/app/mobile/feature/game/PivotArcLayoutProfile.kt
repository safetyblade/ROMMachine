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
    private const val UTILITY_OFFSET_FROM_VIEWPORT = 60f
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

        val core = gameplay.associateWith(::rectangleFor)
        val coreHull =
            PivotArcBounds(
                core.values.minOf { it.left },
                core.values.minOf { it.top },
                core.values.maxOf { it.right },
                core.values.maxOf { it.bottom },
            )

        // Utility buttons remain deliberately outside the frequent-thumb area.
        // Their separate rail leaves room for speaker/indicator hardware between
        // the video and Start/Select/Menu without touching Standard mode.
        val railY = viewport.bottom + UTILITY_OFFSET_FROM_VIEWPORT
        val utilityCentersX =
            mapOf(
                "Select" to safe.left + 34f,
                "Start" to safe.centerX,
                "Menu" to safe.right - 34f,
            )
        val rail =
            active.filter { it in utilities }.associateWith { key ->
                val target = baseline.buttonCenters.getValue(key).touch
                val x = utilityCentersX.getValue(key)
                PivotArcBounds(
                    x - target.w / 2f,
                    railY - target.h / 2f,
                    x + target.w / 2f,
                    railY + target.h / 2f,
                )
            }

        val minTop = max(viewport.bottom + GAP, (rail.values.maxOfOrNull { it.bottom } ?: 0f) + GAP)
        val minDx = safe.left - coreHull.left
        val maxDx = safe.right - coreHull.right
        val minDy = max(safe.top, minTop) - coreHull.top
        val maxDy = safe.bottom - coreHull.bottom
        if (minDx > maxDx || minDy > maxDy) {
            return PivotArcResolution.Unavailable(
                "Insufficient space for full-sized PivotArc controls outside the game viewport.",
            )
        }

        val dx = requestedDx.coerceIn(minDx, maxDx)
        val dy = requestedDy.coerceIn(minDy, maxDy)
        val moved =
            core.mapValues { (_, rect) ->
                PivotArcBounds(
                    rect.left + dx,
                    rect.top + dy,
                    rect.right + dx,
                    rect.bottom + dy,
                )
            }
        val rightHanded = moved + rail
        val positions =
            if (leftHanded) {
                rightHanded.mapValues { (_, rect) ->
                    PivotArcBounds(
                        safe.left + safe.right - rect.right,
                        rect.top,
                        safe.left + safe.right - rect.left,
                        rect.bottom,
                    )
                }
            } else {
                rightHanded
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
            }
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
