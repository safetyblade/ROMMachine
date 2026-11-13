package com.swordfish.lemuroid.app.mobile.feature.shelf

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Original CARTDECK surface art. Pattern cells and stamps never claim a
 * viewport or a touch target; callers supply their measured exclusion zones.
 * Shared by handheld portrait/landscape, tablet and the library/navigation.
 */
object CartdeckSurfaceDesign {
    const val PATTERN_NONE = "none"
    const val PATTERN_TACTICAL_GRID = "tactical-grid"
    const val PATTERN_CAMO = "camo"
    const val PATTERN_DIAGONAL = "diagonal"
    const val PATTERN_PIXEL = "pixel"

    const val STAMP_NONE = "none"
    const val STAMP_CHEVRON = "c-chevron"
    const val STAMP_SMILEY = "smiley"
    const val STAMP_SIGNAL = "signal"

    val patterns =
        linkedMapOf(
            PATTERN_NONE to "No pattern",
            PATTERN_TACTICAL_GRID to "Tactical grid",
            PATTERN_CAMO to "Camo",
            PATTERN_DIAGONAL to "Diagonal hatching",
            PATTERN_PIXEL to "Pixel relief",
        )

    val stamps =
        linkedMapOf(
            STAMP_NONE to "No stamp",
            STAMP_CHEVRON to "CARTDECK chevron",
            STAMP_SMILEY to "Smiley",
            STAMP_SIGNAL to "Signal",
        )

    fun validPattern(value: String): Boolean = value in patterns

    fun validStamp(value: String): Boolean = value in stamps

    /**
     * Geometric safety invariant: every painted tile remains completely inside
     * the residual frame and outside the expanded video, control and brand rects.
     */
    fun safeTiles(
        frame: Rect,
        exclusions: List<Rect>,
        requestedStep: Float,
        clearance: Float,
    ): List<Rect> {
        if (frame.width <= 0f || frame.height <= 0f || requestedStep <= 0f) return emptyList()
        // At most ~420 bounded tile tests, including tablet and TV windows.
        val step = maxOf(requestedStep, sqrt(frame.width * frame.height / 420f))
        val columns = ceil(frame.width / step).toInt().coerceAtMost(80)
        val rows = ceil(frame.height / step).toInt().coerceAtMost(80)
        val blocked = exclusions.map { expanded(it, clearance) }
        val cells = ArrayList<Rect>(minOf(420, columns * rows))
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val left = frame.left + column * step
                val top = frame.top + row * step
                val cell =
                    Rect(
                        left,
                        top,
                        minOf(left + step, frame.right),
                        minOf(top + step, frame.bottom),
                    )
                if (cell.width < step * 0.55f || cell.height < step * 0.55f) continue
                if (blocked.none { intersects(it, cell) }) cells.add(cell)
                if (cells.size >= 420) return cells
            }
        }
        return cells
    }

    fun stampSeat(
        frame: Rect,
        exclusions: List<Rect>,
        requestedSize: Float,
        clearance: Float,
    ): Rect? {
        if (frame.width <= 0f || frame.height <= 0f) return null
        val side = minOf(requestedSize, frame.width * 0.18f, frame.height * 0.22f)
        if (side < 18f) return null
        val inset = maxOf(5f, clearance)
        val candidates =
            listOf(
                Offset(frame.right - side - inset, frame.bottom - side - inset),
                Offset(frame.left + inset, frame.bottom - side - inset),
                Offset(frame.right - side - inset, frame.top + inset),
                Offset(frame.left + inset, frame.top + inset),
                Offset(frame.left + inset, frame.center.y - side / 2f),
                Offset(frame.right - side - inset, frame.center.y - side / 2f),
            )
        val blocked = exclusions.map { expanded(it, clearance) }
        return candidates
            .map { Rect(it.x, it.y, it.x + side, it.y + side) }
            .firstOrNull { candidate ->
                candidate.left >= frame.left && candidate.right <= frame.right &&
                    candidate.top >= frame.top && candidate.bottom <= frame.bottom &&
                    blocked.none { intersects(it, candidate) }
            }
    }

    private fun expanded(
        rect: Rect,
        by: Float,
    ): Rect = Rect(rect.left - by, rect.top - by, rect.right + by, rect.bottom + by)

    private fun intersects(
        one: Rect,
        two: Rect,
    ): Boolean = one.left < two.right && one.right > two.left && one.top < two.bottom && one.bottom > two.top
}

/** Static, GPU-backed cosmetic drawing only; no gesture modifiers or emulator rendering. */
internal fun DrawScope.drawCartdeckSurfaceDesign(
    patternId: String,
    stampId: String,
    frame: Rect,
    exclusions: List<Rect>,
    lineColour: Color,
    stampColour: Color,
    cellSize: Float,
    clearance: Float,
) {
    if (patternId != CartdeckSurfaceDesign.PATTERN_NONE) {
        CartdeckSurfaceDesign.safeTiles(frame, exclusions, cellSize, clearance).forEach { tile ->
            val cx = tile.center.x
            val cy = tile.center.y
            val stroke = maxOf(1f, tile.width * 0.035f)
            when (patternId) {
                CartdeckSurfaceDesign.PATTERN_TACTICAL_GRID -> {
                    val colour = lineColour.copy(alpha = 0.19f)
                    drawLine(colour, Offset(tile.left, cy), Offset(tile.right, cy), stroke)
                    drawLine(colour, Offset(cx, tile.top), Offset(cx, tile.bottom), stroke)
                    drawCircle(colour, radius = stroke * 1.5f, center = tile.center)
                }
                CartdeckSurfaceDesign.PATTERN_DIAGONAL -> {
                    drawLine(
                        lineColour.copy(alpha = 0.22f),
                        Offset(tile.left + tile.width * 0.12f, tile.bottom - tile.height * 0.1f),
                        Offset(tile.right - tile.width * 0.12f, tile.top + tile.height * 0.1f),
                        strokeWidth = stroke * 1.7f,
                    )
                }
                CartdeckSurfaceDesign.PATTERN_CAMO -> {
                    val silhouette =
                        Path().apply {
                            moveTo(tile.left + tile.width * 0.12f, cy)
                            lineTo(cx - tile.width * 0.1f, tile.top + tile.height * 0.15f)
                            lineTo(tile.right - tile.width * 0.1f, tile.top + tile.height * 0.3f)
                            lineTo(tile.right - tile.width * 0.24f, tile.bottom - tile.height * 0.1f)
                            lineTo(tile.left + tile.width * 0.25f, tile.bottom - tile.height * 0.19f)
                            close()
                        }
                    drawPath(silhouette, lineColour.copy(alpha = 0.14f))
                }
                CartdeckSurfaceDesign.PATTERN_PIXEL -> {
                    drawRect(
                        lineColour.copy(alpha = 0.22f),
                        topLeft = Offset(tile.left + tile.width * 0.2f, tile.top + tile.height * 0.22f),
                        size = androidx.compose.ui.geometry.Size(tile.width * 0.33f, tile.height * 0.33f),
                    )
                }
            }
        }
    }

    if (stampId == CartdeckSurfaceDesign.STAMP_NONE) return
    val stamp =
        CartdeckSurfaceDesign.stampSeat(
            frame = frame,
            exclusions = exclusions,
            requestedSize = cellSize * 2.1f,
            clearance = clearance,
        )
            ?: return
    val center = stamp.center
    val radius = minOf(stamp.width, stamp.height) * 0.38f
    val ink = stampColour.copy(alpha = 0.72f)
    val stroke = maxOf(1f, radius * 0.12f)
    when (stampId) {
        CartdeckSurfaceDesign.STAMP_CHEVRON -> {
            val logo =
                Path().apply {
                    moveTo(center.x + radius * 0.6f, center.y - radius * 0.8f)
                    lineTo(center.x - radius * 0.5f, center.y - radius * 0.8f)
                    lineTo(center.x - radius, center.y)
                    lineTo(center.x - radius * 0.5f, center.y + radius * 0.8f)
                    lineTo(center.x + radius * 0.6f, center.y + radius * 0.8f)
                }
            drawPath(logo, ink, style = Stroke(width = stroke * 1.4f))
            drawLine(
                ink,
                Offset(center.x + radius * 0.2f, center.y),
                Offset(center.x + radius, center.y),
                strokeWidth = stroke,
            )
        }
        CartdeckSurfaceDesign.STAMP_SMILEY -> {
            drawCircle(ink, radius = radius, center = center, style = Stroke(width = stroke))
            drawCircle(ink, radius = stroke, center = Offset(center.x - radius * 0.35f, center.y - radius * 0.2f))
            drawCircle(ink, radius = stroke, center = Offset(center.x + radius * 0.35f, center.y - radius * 0.2f))
            drawLine(
                ink,
                Offset(center.x - radius * 0.35f, center.y + radius * 0.3f),
                Offset(center.x + radius * 0.35f, center.y + radius * 0.3f),
                strokeWidth = stroke,
            )
        }
        CartdeckSurfaceDesign.STAMP_SIGNAL -> {
            for (index in 0..2) {
                val x = center.x + (index - 1) * radius * 0.65f
                drawLine(
                    ink,
                    Offset(x, center.y + radius * 0.6f),
                    Offset(x, center.y + radius * (0.25f - index * 0.45f)),
                    strokeWidth = stroke * 1.8f,
                )
            }
        }
    }
}
