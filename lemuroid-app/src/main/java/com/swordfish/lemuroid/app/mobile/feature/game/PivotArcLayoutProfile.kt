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
)

sealed interface PivotArcResolution {
    data class Fits(val placement: PivotArcPlacement) : PivotArcResolution

    data class Unavailable(val reason: String) : PivotArcResolution
}

/**
 * Pure fit solver. Moves the whole gameplay constellation, never its individual
 * controls. Utilities occupy a separate rail, and are not allowed to collide
 * with the viewport, other keys or gesture inset. Incompatible modes are not
 * persisted as Standard; the game can temporarily use its untouched PadKit UI.
 */
object PivotArcPositionSolver {
    private const val REFERENCE_WIDTH = 393f
    private const val REFERENCE_HEIGHT = 852f
    private const val GAP = 8f
    private val utilities = setOf("Start", "Select", "Menu")

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
        if (isLandscape) return PivotArcResolution.Unavailable("PivotArc currently requires portrait orientation.")
        if (safe.width <= 0f || safe.height <= 0f || !requestedDx.isFinite() || !requestedDy.isFinite()) {
            return PivotArcResolution.Unavailable("Invalid measured display bounds or saved position.")
        }
        if (active.isEmpty() || active.any { it !in baseline.buttonCenters }) {
            return PivotArcResolution.Unavailable("Required system controls are not defined.")
        }
        val gameplay = active.filterNot { it in utilities }
        if (gameplay.isEmpty()) return PivotArcResolution.Unavailable("No usable gameplay controls.")
        val undersized =
            gameplay.any {
                val target = baseline.buttonCenters.getValue(it).touch
                target.w < 48f || target.h < 48f
            }
        if (undersized) {
            return PivotArcResolution.Unavailable("PivotArc targets are below the safe 48dp minimum.")
        }

        val shiftX = safe.centerX - REFERENCE_WIDTH / 2f
        val shiftY = safe.bottom - REFERENCE_HEIGHT

        fun rectFor(key: String): PivotArcBounds {
            val b = baseline.buttonCenters.getValue(key)
            val x = b.center.x + shiftX
            val y = b.center.y + shiftY
            return PivotArcBounds(
                x - b.touch.w / 2f,
                y - b.touch.h / 2f,
                x + b.touch.w / 2f,
                y + b.touch.h / 2f,
            )
        }

        val core = gameplay.associateWith(::rectFor)
        val coreHull =
            PivotArcBounds(
                core.values.minOf { it.left },
                core.values.minOf { it.top },
                core.values.maxOf { it.right },
                core.values.maxOf { it.bottom },
            )

        // Three low-frequency inputs sit in a protected rail directly below
        // the real viewport, rather than migrating Standard Start/Select.
        val utilityCenterY = viewport.bottom + GAP + 24f
        val utilityCentersX =
            mapOf(
                "Select" to safe.left + 34f,
                "Start" to safe.centerX,
                "Menu" to safe.right - 34f,
            )
        val rail =
            active.filter { it in utilities }.associateWith { key ->
                val spec = baseline.buttonCenters.getValue(key).touch
                val x = utilityCentersX.getValue(key)
                PivotArcBounds(
                    x - spec.w / 2f,
                    utilityCenterY - spec.h / 2f,
                    x + spec.w / 2f,
                    utilityCenterY + spec.h / 2f,
                )
            }

        // Move all active gameplay controls as ONE cluster; 8dp clearance is
        // enforced between the utility rail and the game cluster.
        val minimumTop = max(viewport.bottom + GAP, (rail.values.maxOfOrNull { it.bottom } ?: viewport.bottom) + GAP)
        val minDx = safe.left - coreHull.left
        val maxDx = safe.right - coreHull.right
        val minDy = max(safe.top, minimumTop) - coreHull.top
        val maxDy = safe.bottom - coreHull.bottom
        if (minDx > maxDx || minDy > maxDy) {
            return PivotArcResolution.Unavailable("Insufficient safe control space. Standard controls are available.")
        }

        // Baseline is bottom-anchored; adjust only when necessary to keep it
        // outside the measured game viewport and rail.
        val dx = requestedDx.coerceIn(minDx, maxDx)
        val dy = requestedDy.coerceIn(minDy, maxDy)
        val translated =
            core.mapValues { (_, rect) ->
                PivotArcBounds(rect.left + dx, rect.top + dy, rect.right + dx, rect.bottom + dy)
            }
        val rightHandPositions = translated + rail
        val positions =
            if (leftHanded) {
                rightHandPositions.mapValues { (_, r) ->
                    PivotArcBounds(
                        safe.left + safe.right - r.right,
                        r.top,
                        safe.left + safe.right - r.left,
                        r.bottom,
                    )
                }
            } else {
                rightHandPositions
            }

        for ((key, r) in positions) {
            if (!r.inside(safe)) return PivotArcResolution.Unavailable("$key enters a display inset.")
            if (r.intersects(viewport.inset(GAP))) {
                return PivotArcResolution.Unavailable("$key would obscure game pixels.")
            }
        }
        val entries = positions.entries.toList()
        for (i in entries.indices) {
            for (j in i + 1 until entries.size) {
                val a = entries[i].value.inset(GAP / 2f)
                val b = entries[j].value.inset(GAP / 2f)
                if (a.intersects(b)) {
                    return PivotArcResolution.Unavailable(
                        "${entries[i].key} and ${entries[j].key} lack safe separation.",
                    )
                }
            }
        }
        return PivotArcResolution.Fits(
            PivotArcPlacement(
                controls = positions,
                visuals = active.associateWith { baseline.buttonCenters.getValue(it).visual },
                appliedDx = dx,
                appliedDy = dy,
            ),
        )
    }
}
