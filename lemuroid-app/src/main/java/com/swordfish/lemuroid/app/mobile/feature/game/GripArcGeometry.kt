package com.swordfish.lemuroid.app.mobile.feature.game

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/** Runtime geometry sourced directly from the approved PR #126 manifest. */
@Serializable
data class GripArcManifestPoint(val x: Float, val y: Float)

@Serializable
data class GripArcManifestSize(val width: Float, val height: Float)

@Serializable
data class GripArcManifestButtonSize(val w: Float, val h: Float)

@Serializable
data class GripArcManifestSlot(
    val id: String,
    val n: Int,
    val centerRight: GripArcManifestPoint,
    val centerLeft: GripArcManifestPoint,
    val visual: GripArcManifestButtonSize,
    val hitTarget: GripArcManifestButtonSize,
)

@Serializable
data class GripArcManifest(
    val schema: String,
    val artboard: GripArcManifestSize,
    val slots: List<GripArcManifestSlot>,
) {
    companion object {
        private val decoder = Json { ignoreUnknownKeys = true }

        fun parse(text: String): GripArcManifest =
            decoder.decodeFromString<GripArcManifest>(text).also { manifest ->
                require(manifest.schema == "cartdeck.griparc.assets/1.0.0")
                require(manifest.artboard.width == 272f && manifest.artboard.height == 308f)
                require(manifest.slots.map { it.id } == listOf("G1", "G2", "G3", "G4"))
                require(manifest.slots.map { it.n } == listOf(1, 2, 3, 4))
                require(manifest.slots.all { it.hitTarget.w >= 80f && it.hitTarget.h >= 52f })
            }

        fun load(context: Context): GripArcManifest =
            context.assets.open("griparc/griparc-v1-manifest.json")
                .bufferedReader().use { parse(it.readText()) }
    }
}

/** Reject incomplete/invalid measurements instead of accepting NaN comparisons as clear space. */
internal fun PivotArcBounds.isFiniteArea(): Boolean =
    listOf(left, top, right, bottom, width, height).all { it.isFinite() } && width > 0f && height > 0f

data class GripArcPlacement(
    val housing: PivotArcBounds,
    val targets: Map<Int, PivotArcBounds>,
    val visuals: Map<Int, PivotArcSize>,
    val scale: Float,
    val side: GripArcSide,
)

sealed interface GripArcFit {
    data class Fits(val placement: GripArcPlacement) : GripArcFit

    data class Unavailable(val reason: String) : GripArcFit
}

/**
 * This solver translates/scales one permanent 272×308 L assembly.
 * G1–G4 always keep the approved manifest centres. No button reflow.
 * Only the painted vertical and horizontal arms of the permanent L count as
 * physical housing. The open interior is available for ordinary controls.
 * Active touch slots are checked separately; the full four-slot footprint
 * remains reserved regardless of current count.
 */
object GripArcPositionSolver {
    private const val SAFE_GAP = 8f
    private const val MIN_TARGET = 48f
    private val scales = listOf(0.84f, 0.80f, 0.76f, 0.72f)

    /**
     * Actual permanent L material, in approved 272 x 308 source coordinates.
     * Right vertical arm: x 157..265, entire height. Bottom: y 219..298.
     * Left mirrors x around the 272-unit centre. Do NOT use the enclosing
     * rectangle: the unused top-inside area is intentionally open.
     */
    internal fun housingFootprint(
        housing: PivotArcBounds,
        scale: Float,
        side: GripArcSide,
    ): List<PivotArcBounds> {
        val armLeft = if (side == GripArcSide.RIGHT) 157f else 7f
        val armRight = if (side == GripArcSide.RIGHT) 265f else 115f
        return listOf(
            PivotArcBounds(
                housing.left + armLeft * scale,
                housing.top + 12f * scale,
                housing.left + armRight * scale,
                housing.top + 298f * scale,
            ),
            PivotArcBounds(
                housing.left + 7f * scale,
                housing.top + 219f * scale,
                housing.left + 265f * scale,
                housing.top + 298f * scale,
            ),
        )
    }

    fun resolve(
        manifest: GripArcManifest,
        safe: PivotArcBounds,
        viewport: PivotArcBounds,
        occupied: Collection<PivotArcBounds>,
        side: GripArcSide,
        dx: Float = 0f,
        dy: Float = 0f,
        maxScale: Float = 0.84f,
    ): GripArcFit {
        if (!dx.isFinite() || !dy.isFinite() || !safe.isFiniteArea() || !viewport.isFiniteArea()) {
            return GripArcFit.Unavailable("GripArc has invalid screen measurements.")
        }
        for (scale in scales.filter { it <= maxScale }) {
            val width = manifest.artboard.width * scale
            val height = manifest.artboard.height * scale
            if (width > safe.width || height > safe.height) continue
            val minX = safe.left
            val maxX = safe.right - width
            val minY = safe.top
            val maxY = safe.bottom - height
            val edgeX = if (side == GripArcSide.RIGHT) maxX else minX
            val candidatesY =
                listOf(
                    // Anchor the permanent bottom arm to the lower console edge first.
                    maxY,
                    (viewport.bottom + maxY) / 2f,
                    viewport.bottom + SAFE_GAP,
                    minY,
                )
            for (candidateY in candidatesY) {
                val x = (edgeX + dx).coerceIn(minX, maxX)
                val y = (candidateY + dy).coerceIn(minY, maxY)
                val housing = PivotArcBounds(x, y, x + width, y + height)
                val footprint = housingFootprint(housing, scale, side)
                if (footprint.any { it.intersects(viewport.inset(SAFE_GAP)) }) continue
                if (footprint.any { arm -> occupied.any { arm.intersects(it.inset(SAFE_GAP / 2f)) } }) continue
                val targets =
                    manifest.slots.associate { slot ->
                        val center = if (side == GripArcSide.RIGHT) slot.centerRight else slot.centerLeft
                        val touchWidth = maxOf(MIN_TARGET, slot.hitTarget.w * scale)
                        val touchHeight = maxOf(MIN_TARGET, slot.hitTarget.h * scale)
                        slot.n to
                            PivotArcBounds(
                                x + center.x * scale - touchWidth / 2f,
                                y + center.y * scale - touchHeight / 2f,
                                x + center.x * scale + touchWidth / 2f,
                                y + center.y * scale + touchHeight / 2f,
                            )
                    }
                val targetRects = targets.values.toList()
                if (targetRects.any { !it.inside(safe) || it.intersects(viewport.inset(SAFE_GAP)) }) continue
                if (targetRects.any { box -> occupied.any { box.intersects(it.inset(SAFE_GAP / 2f)) } }) continue
                if (targetRects.indices.any { i ->
                        (i + 1 until targetRects.size).any { j -> targetRects[i].intersects(targetRects[j]) }
                    }
                ) {
                    continue
                }
                val visuals =
                    manifest.slots.associate { slot ->
                        slot.n to PivotArcSize(slot.visual.w * scale * 0.9f, slot.visual.h * scale * 0.9f)
                    }
                return GripArcFit.Fits(GripArcPlacement(housing, targets, visuals, scale, side))
            }
        }
        return GripArcFit.Unavailable(
            "The complete GripArc attachment does not fit outside gameplay and existing controls. " +
                "Move controls or select a compatible layout; your cartridge preference is preserved.",
        )
    }
}
