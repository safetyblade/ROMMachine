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
 * Conservative collision checks use the entire bounding rectangle, so a
 * control is never shown over a viewport or another known input target.
 */
object GripArcPositionSolver {
    private const val SAFE_GAP = 8f
    private const val MIN_TARGET = 48f
    private val scales = listOf(1.0f, 0.90f, 0.82f, 0.76f, 0.72f)

    fun resolve(
        manifest: GripArcManifest,
        safe: PivotArcBounds,
        viewport: PivotArcBounds,
        occupied: Collection<PivotArcBounds>,
        side: GripArcSide,
        dx: Float = 0f,
        dy: Float = 0f,
    ): GripArcFit {
        if (!dx.isFinite() || !dy.isFinite() || safe.width <= 0f || safe.height <= 0f) {
            return GripArcFit.Unavailable("GripArc has invalid screen measurements.")
        }
        for (scale in scales) {
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
                    viewport.bottom + SAFE_GAP,
                    maxY,
                    (viewport.bottom + maxY) / 2f,
                    minY,
                )
            for (candidateY in candidatesY) {
                val x = (edgeX + dx).coerceIn(minX, maxX)
                val y = (candidateY + dy).coerceIn(minY, maxY)
                val housing = PivotArcBounds(x, y, x + width, y + height)
                if (housing.intersects(viewport.inset(SAFE_GAP))) continue
                if (occupied.any { housing.intersects(it.inset(SAFE_GAP / 2f)) }) continue
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
                        slot.n to PivotArcSize(slot.visual.w * scale, slot.visual.h * scale)
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
