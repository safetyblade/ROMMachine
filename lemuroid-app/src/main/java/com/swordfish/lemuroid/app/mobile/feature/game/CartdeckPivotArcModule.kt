package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swordfish.lemuroid.app.mobile.feature.shelf.colorContrast
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartdeckEditionTokens
import kotlin.math.roundToInt

/** One approved PivotArc identity across the installed shell and Layout settings. */
@Composable
internal fun PivotArcWordmark(
    modifier: Modifier = Modifier,
    ink: Color,
    accent: Color,
    compact: Boolean = false,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "PivotArc",
            color = ink,
            fontSize = if (compact) 16.sp else 19.sp,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        if (!compact) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "ONE HAND MODE",
                    color = accent,
                    fontSize = 8.sp,
                    letterSpacing = 1.3.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/**
 * Geometry for an attached hardware control plate. NEVER assume five physical
 * controls: every active gameplay key (including four future extra buttons)
 * participates in the measured hull. The utility rail and game screen are
 * independent exclusions. Art is pointer-transparent and never changes PadKit.
 */
internal data class PivotArcModulePlacement(
    val plate: Rect,
    val wordmark: Rect?,
)

internal object PivotArcModuleGeometry {
    private val utilityKeys = setOf("start", "select", "menu")

    fun resolve(
        controls: Map<String, Rect>,
        video: Rect?,
        root: Rect,
        edge: Float,
        labelHeight: Float,
    ): PivotArcModulePlacement? {
        if (root.width <= 0f || root.height <= 0f || edge <= 0f || labelHeight <= 0f) return null
        val gameplay = controls.filterKeys { it !in utilityKeys }.values
        if (gameplay.isEmpty()) return null

        val coreLeft = gameplay.minOf { it.left }
        val coreRight = gameplay.maxOf { it.right }
        val coreTop = gameplay.minOf { it.top }
        val coreBottom = gameplay.maxOf { it.bottom }
        // V1 utility group can be below the main cluster. Only utilities
        // genuinely ABOVE the gameplay hull constrain the plate top.
        val utilitiesAboveCore =
            controls.filterKeys { it in utilityKeys }.values
                .filter { it.bottom <= coreTop }
                .maxOfOrNull { it.bottom } ?: 0f
        val excludedTop = maxOf(video?.bottom ?: 0f, utilitiesAboveCore) + edge / 2f

        val left = maxOf(edge / 2f, coreLeft - edge)
        val top = maxOf(excludedTop, coreTop - edge - labelHeight)
        val right = minOf(root.width - edge / 2f, coreRight + edge)
        val bottom = minOf(root.height - edge / 2f, coreBottom + edge)
        if (top >= coreTop || left >= coreLeft || right <= coreRight || bottom <= coreBottom) {
            return null
        }
        val plate = Rect(left, top, right, bottom)
        val label =
            if (coreTop - top >= labelHeight + edge / 2f) {
                Rect(left + edge, top + 2f, right - edge, top + labelHeight)
                    .takeIf { it.width >= 105f && it.bottom < coreTop }
            } else {
                null
            }
        return PivotArcModulePlacement(plate, label)
    }
}

/** Draw beneath PadKit and its artwork; never install pointer handlers. */
@Composable
internal fun CartdeckPivotArcModule(
    controls: Map<String, Rect>,
    video: Rect?,
    root: Rect,
    tokens: CartdeckEditionTokens,
) {
    val density = LocalDensity.current
    val edge = with(density) { 11.dp.toPx() }
    val labelHeight = with(density) { 31.dp.toPx() }
    val module = PivotArcModuleGeometry.resolve(controls, video, root, edge, labelHeight) ?: return
    val ink =
        listOf(tokens.onChassis, tokens.onButton, Color.White, Color.Black)
            .maxBy { colorContrast(it, tokens.secondary) }

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val plate = module.plate
            val bevel = 15.dp.toPx()
            val face =
                Path().apply {
                    moveTo(plate.left + bevel, plate.top)
                    lineTo(plate.right - bevel, plate.top)
                    lineTo(plate.right, plate.top + bevel)
                    lineTo(plate.right, plate.bottom - bevel)
                    lineTo(plate.right - bevel, plate.bottom)
                    lineTo(plate.left + bevel, plate.bottom)
                    lineTo(plate.left, plate.bottom - bevel)
                    lineTo(plate.left, plate.top + bevel)
                    close()
                }
            // V2: multiple material levels are painted UNDER PadKit. This
            // layer never owns input, changes control coordinates, or extends
            // into the game viewport (the plate comes from measured geometry).
            val depth = 3.dp.toPx()
            val insetEdge = 7.dp.toPx()
            fun chamferedPlate(bounds: Rect, corner: Float) =
                Path().apply {
                    moveTo(bounds.left + corner, bounds.top)
                    lineTo(bounds.right - corner, bounds.top)
                    lineTo(bounds.right, bounds.top + corner)
                    lineTo(bounds.right, bounds.bottom - corner)
                    lineTo(bounds.right - corner, bounds.bottom)
                    lineTo(bounds.left + corner, bounds.bottom)
                    lineTo(bounds.left, bounds.bottom - corner)
                    lineTo(bounds.left, bounds.top + corner)
                    close()
                }
            val contactShadow =
                Rect(plate.left, plate.top + depth, plate.right, plate.bottom)
            drawPath(chamferedPlate(contactShadow, bevel), tokens.bezel.copy(alpha = 0.68f))
            drawPath(face, tokens.secondary)
            drawPath(
                face,
                tokens.onChassis.copy(alpha = 0.23f),
                style = Stroke(width = 2.dp.toPx()),
            )
            drawPath(face, tokens.bezel, style = Stroke(width = 3.dp.toPx()))
            val innerPlate =
                Rect(
                    plate.left + insetEdge,
                    plate.top + insetEdge,
                    plate.right - insetEdge,
                    plate.bottom - insetEdge,
                )
            if (innerPlate.width > 2 * bevel && innerPlate.height > 2 * bevel) {
                val inner = chamferedPlate(innerPlate, bevel * 0.60f)
                drawPath(inner, tokens.chassis.copy(alpha = 0.46f))
                drawPath(
                    inner,
                    tokens.onChassis.copy(alpha = 0.13f),
                    style = Stroke(width = 1.1.dp.toPx()),
                )
                drawLine(
                    tokens.bezel.copy(alpha = 0.64f),
                    Offset(innerPlate.left + bevel, innerPlate.bottom - 2.dp.toPx()),
                    Offset(innerPlate.right - bevel, innerPlate.bottom - 2.dp.toPx()),
                    strokeWidth = 2.2.dp.toPx(),
                )
            }
            // The face, D-pad well and shoulders get individual machined
            // seats. The existing caps/pressed feedback remain authoritative.
            val gameplaySeats = controls.filterKeys {
                it.lowercase() !in setOf("start", "select", "menu")
            }
            for ((key, bounds) in gameplaySeats) {
                val center = bounds.center
                val radius = minOf(bounds.width, bounds.height) / 2f
                when (key.uppercase()) {
                    "A", "B", "X", "Y" -> {
                        val collar = (radius - 2.dp.toPx()).coerceAtLeast(0f)
                        drawCircle(tokens.bezel.copy(alpha = 0.89f), radius = collar, center = center)
                        drawCircle(
                            tokens.onChassis.copy(alpha = 0.35f),
                            radius = (collar - 3.dp.toPx()).coerceAtLeast(0f),
                            center = center,
                            style = Stroke(width = 1.4.dp.toPx()),
                        )
                        drawArc(
                            tokens.accent.copy(alpha = 0.28f),
                            startAngle = 205f,
                            sweepAngle = 110f,
                            useCenter = false,
                            topLeft = Offset(center.x - collar, center.y - collar),
                            size = Size(collar * 2, collar * 2),
                            style = Stroke(width = 1.2.dp.toPx()),
                        )
                    }
                    "D" -> {
                        val margin = 3.dp.toPx()
                        val wellTop = Offset(bounds.left + margin, bounds.top + margin)
                        val wellSize = Size(
                            (bounds.width - margin * 2).coerceAtLeast(0f),
                            (bounds.height - margin * 2).coerceAtLeast(0f),
                        )
                        drawRoundRect(
                            tokens.bezel.copy(alpha = 0.90f),
                            topLeft = wellTop,
                            size = wellSize,
                            cornerRadius = CornerRadius(22.dp.toPx()),
                        )
                        drawRoundRect(
                            tokens.onChassis.copy(alpha = 0.26f),
                            topLeft = wellTop,
                            size = wellSize,
                            cornerRadius = CornerRadius(22.dp.toPx()),
                            style = Stroke(width = 1.7.dp.toPx()),
                        )
                    }
                    "L", "R", "L2", "R2" -> {
                        val margin = 2.dp.toPx()
                        drawRoundRect(
                            tokens.bezel.copy(alpha = 0.86f),
                            topLeft = Offset(bounds.left + margin, bounds.top + margin),
                            size = Size(
                                (bounds.width - 2 * margin).coerceAtLeast(0f),
                                (bounds.height - 2 * margin).coerceAtLeast(0f),
                            ),
                            cornerRadius = CornerRadius(12.dp.toPx()),
                        )
                        drawLine(
                            tokens.onChassis.copy(alpha = 0.34f),
                            Offset(bounds.left + 9.dp.toPx(), bounds.top + 6.dp.toPx()),
                            Offset(bounds.right - 9.dp.toPx(), bounds.top + 6.dp.toPx()),
                            strokeWidth = 1.2.dp.toPx(),
                        )
                    }
                    "HOLDA", "HOLDB" -> {
                        drawCircle(
                            tokens.bezel.copy(alpha = 0.85f),
                            radius = (radius - 2.dp.toPx()).coerceAtLeast(0f),
                            center = center,
                        )
                        drawCircle(
                            tokens.onChassis.copy(alpha = 0.25f),
                            radius = (radius - 5.dp.toPx()).coerceAtLeast(0f),
                            center = center,
                            style = Stroke(width = 1.dp.toPx()),
                        )
                    }
                }
            }
            // Small hardware utility seats, but never alter Standard's rail.
            for ((key, bounds) in controls) {
                if (key.lowercase() !in setOf("start", "select", "menu")) continue
                drawRoundRect(
                    tokens.bezel.copy(alpha = 0.82f),
                    topLeft = Offset(bounds.left + 2.dp.toPx(), bounds.top + 2.dp.toPx()),
                    size = Size(
                        (bounds.width - 4.dp.toPx()).coerceAtLeast(0f),
                        (bounds.height - 4.dp.toPx()).coerceAtLeast(0f),
                    ),
                    cornerRadius = CornerRadius(9.dp.toPx()),
                )
            }
            val foot = 11.dp.toPx()
            val inset = 4.dp.toPx()
            drawLine(
                tokens.accent.copy(alpha = 0.72f),
                Offset(plate.left + bevel, plate.top + inset),
                Offset(minOf(plate.left + bevel + 27.dp.toPx(), plate.right - bevel), plate.top + inset),
                strokeWidth = 2.dp.toPx(),
            )
            drawLine(
                tokens.accent.copy(alpha = 0.72f),
                Offset(maxOf(plate.right - bevel - 27.dp.toPx(), plate.left + bevel), plate.top + inset),
                Offset(plate.right - bevel, plate.top + inset),
                strokeWidth = 2.dp.toPx(),
            )
            drawLine(
                tokens.accent.copy(alpha = 0.42f),
                Offset(plate.left + bevel, plate.bottom - inset),
                Offset(minOf(plate.left + bevel + 22.dp.toPx(), plate.right - bevel), plate.bottom - inset),
                strokeWidth = 1.8.dp.toPx(),
            )
            // Machined bolt heads remain away from active control surfaces.
            val bolts =
                listOf(
                    Offset(plate.left + foot, plate.top + foot),
                    Offset(plate.right - foot, plate.top + foot),
                    Offset(plate.left + foot, plate.bottom - foot),
                    Offset(plate.right - foot, plate.bottom - foot),
                )
            val gameplay = controls.filterKeys { it !in setOf("start", "select", "menu") }.values
            for (center in bolts) {
                val protected =
                    Rect(
                        center.x - 7.dp.toPx(),
                        center.y - 7.dp.toPx(),
                        center.x + 7.dp.toPx(),
                        center.y + 7.dp.toPx(),
                    )
                if (gameplay.none { CartdeckHardwareGeometry.intersects(it, protected) }) {
                    drawCircle(tokens.bezel, radius = 3.dp.toPx(), center = center)
                    drawCircle(
                        tokens.accent.copy(alpha = 0.34f),
                        radius = 2.dp.toPx(),
                        center = center,
                        style = Stroke(0.8.dp.toPx()),
                    )
                    drawLine(
                        tokens.onChassis.copy(alpha = 0.40f),
                        center - Offset(1.5.dp.toPx(), 0f),
                        center + Offset(1.5.dp.toPx(), 0f),
                        strokeWidth = 0.8.dp.toPx(),
                    )
                }
            }
        }

        module.wordmark?.let { region ->
            val width = with(density) { region.width.toDp() }
            val height = with(density) { region.height.toDp() }
            Box(
                modifier =
                    Modifier
                        .offset { IntOffset(region.left.roundToInt(), region.top.roundToInt()) }
                        .width(width)
                        .height(height),
                contentAlignment = Alignment.Center,
            ) {
                PivotArcWordmark(
                    ink = ink,
                    accent = tokens.accent,
                    compact = true,
                )
            }
        }
    }
}
