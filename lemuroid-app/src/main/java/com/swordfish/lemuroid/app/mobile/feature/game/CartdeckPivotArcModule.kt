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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
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

internal const val PIVOT_ARC_DISPLAY_CLEARANCE_DP = 20f

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
            lineHeight = if (compact) 16.sp else 21.sp,
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
        displayClearance: Float = edge * 1.8f,
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
        val excludedTop =
            maxOf(
                (video?.bottom ?: 0f) + displayClearance,
                utilitiesAboveCore + edge / 2f,
            )

        val left = maxOf(edge / 2f, coreLeft - edge)
        val top = maxOf(excludedTop, coreTop - edge - labelHeight)
        val right = minOf(root.width - edge / 2f, coreRight + edge)
        val bottom = minOf(root.height - edge / 2f, coreBottom + edge)
        if (top >= coreTop || left >= coreLeft || right <= coreRight || bottom <= coreBottom) {
            return null
        }
        val plate = Rect(left, top, right, bottom)
        val labelTop = top + edge / 3f
        val labelWidth = minOf(plate.width - edge * 2f, edge * 10f)
        val label =
            if (labelWidth >= edge * 8f) {
                val center = (left + right - labelWidth) / 2f
                listOf(center, right - edge - labelWidth, left + edge)
                    .map { x -> Rect(x, labelTop, x + labelWidth, labelTop + labelHeight) }
                    .firstOrNull { candidate ->
                        candidate.bottom <= plate.bottom - edge &&
                            controls.values.none { CartdeckHardwareGeometry.intersects(it, candidate) }
                    }
            } else {
                null
            }
        return PivotArcModulePlacement(plate, label)
    }
}

/** Material and trim dimensions for the measured PivotArc housing. */
internal object PivotArcPaintPolicy {
    /** Hardware-seat names, not raw gamepad keys, arrive from MobileGameScreen. */
    fun materialKey(key: String): String =
        when {
            key.startsWith("face-") -> key.removePrefix("face-").uppercase()
            key == "dpad" || key == "c-direction" -> "D"
            key.startsWith("analog-") -> "ANALOG"
            key.startsWith("shoulder-") -> key.removePrefix("shoulder-").uppercase()
            key == "hold-a" -> "HOLDA"
            key == "hold-b" -> "HOLDB"
            else -> key.uppercase()
        }

    /** Compact local control pods; NEVER paint the full clearance hull. */
    fun pods(
        controls: Map<String, Rect>,
        padding: Float,
        density: Float = 1f,
    ): List<Rect> {
        if (padding <= 0f) return emptyList()
        val groups =
            listOf(
                controls.filterKeys { materialKey(it) == "D" }.values,
                controls.filterKeys { materialKey(it) == "ANALOG" }.values,
                controls.filterKeys { materialKey(it) in setOf("A", "B", "X", "Y") }.values,
            )
        return groups.filter { it.isNotEmpty() }.mapNotNull { seats ->
            val bounds =
                Rect(
                    seats.minOf { it.left } - padding,
                    seats.minOf { it.top } - padding,
                    seats.maxOf { it.right } + padding,
                    seats.maxOf { it.bottom } + padding,
                )
            // A scattered layout must not recreate the oversized black slab.
            bounds.takeIf { it.width <= 175f * density && it.height <= 175f * density }
        }
    }

    fun rails(
        plate: Rect,
        width: Float,
    ): List<Rect> {
        if (width <= 0f || plate.width <= width * 4f || plate.height <= width * 4f) return emptyList()
        return listOf(
            Rect(plate.left + width, plate.top, plate.right - width, plate.top + width),
            Rect(plate.left + width, plate.bottom - width, plate.right - width, plate.bottom),
        )
    }

    fun railAreaFraction(
        plate: Rect,
        width: Float,
    ): Float =
        if (plate.width <= 0f || plate.height <= 0f) {
            0f
        } else {
            rails(plate, width).sumOf { (it.width * it.height).toDouble() }.toFloat() /
                (plate.width * plate.height)
        }
}

internal fun pivotArcHardwareSeatKey(key: String): String =
    when (key) {
        "D" -> "dpad"
        "Analog", "AnalogL" -> "analog-left"
        "AnalogR" -> "analog-right"
        "A", "B", "X", "Y" -> "face-${key.lowercase()}"
        "HoldA" -> "hold-a"
        "HoldB" -> "hold-b"
        "L", "R", "L2", "R2" -> "shoulder-$key"
        else -> key.lowercase()
    }

/** Bevel and recessed material shared by the controller islands and dock. */
internal fun DrawScope.drawControllerInset(
    rim: Path,
    inset: Path,
    bounds: Rect,
    tokens: CartdeckEditionTokens,
    edge: Float,
    housing: Color = tokens.secondary,
) {
    drawPath(rim, tokens.bezel)
    drawPath(
        inset,
        Brush.verticalGradient(
            listOf(
                lerp(housing, tokens.bezel, 0.55f),
                housing,
                lerp(tokens.chassis, housing, 0.65f),
            ),
            startY = bounds.top,
            endY = bounds.bottom,
        ),
    )
    drawPath(rim, tokens.secondary, style = Stroke(edge))
    drawPath(inset, tokens.onChassis.copy(alpha = 0.24f), style = Stroke(edge / 2f))
}

internal fun chamferedIsland(
    bounds: Rect,
    corner: Float,
): Path =
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

/** Trace a single recessed face with an optional cutout for the mounted GripArc. */
private fun cradleContour(
    bounds: Rect,
    notch: Rect?,
    corner: Float,
): Path {
    val upper = chamferedIsland(bounds, minOf(corner, bounds.height / 4f, bounds.width / 4f))
    return if (notch == null) {
        upper
    } else {
        val lower = chamferedIsland(notch, minOf(corner, notch.height / 4f, notch.width / 4f))
        Path.combine(PathOperation.Union, upper, lower)
    }
}

/** Draw beneath PadKit and its artwork; never install pointer handlers. */
@Composable
internal fun CartdeckPivotArcModule(
    controls: Map<String, Rect>,
    video: Rect?,
    root: Rect,
    tokens: CartdeckEditionTokens,
    joinedIsland: PivotIslandShape? = null,
    showWordmark: Boolean = true,
) {
    val density = LocalDensity.current
    val edge = with(density) { 11.dp.toPx() }
    val labelHeight = with(density) { 20.dp.toPx() }
    val displayClearance = with(density) { PIVOT_ARC_DISPLAY_CLEARANCE_DP.dp.toPx() }
    val module = PivotArcModuleGeometry.resolve(controls, video, root, edge, labelHeight, displayClearance) ?: return
    val ink =
        listOf(tokens.onChassis, tokens.onButton, Color.White, Color.Black)
            .maxBy { colorContrast(it, tokens.secondary) }

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val plate = module.plate
            val bevel = 15.dp.toPx()

            fun PivotArcBounds.pixels(): Rect = Rect(left.dp.toPx(), top.dp.toPx(), right.dp.toPx(), bottom.dp.toPx())

            val upper =
                joinedIsland?.upper?.pixels()?.let {
                    it.copy(top = maxOf(it.top, (video?.bottom ?: 0f) + displayClearance))
                } ?: plate
            val lower = joinedIsland?.lower?.pixels()
            val lip = 5.dp.toPx()
            if (upper.width > lip * 2f && upper.height > lip * 2f) {
                val inset = upper.deflate(lip)
                drawControllerInset(
                    rim = cradleContour(upper, lower?.let { it.copy(top = it.top - lip * 2f) }, bevel),
                    inset =
                        cradleContour(
                            inset,
                            lower?.deflate(lip)?.let { it.copy(top = it.top - lip * 4f) },
                            bevel / 2f,
                        ),
                    bounds = upper.copy(bottom = lower?.bottom ?: upper.bottom),
                    tokens = tokens,
                    edge = 2.dp.toPx(),
                    housing = lerp(tokens.chassis, tokens.secondary, 0.24f),
                )
            }
            if (joinedIsland == null) {
                for (groove in PivotArcPaintPolicy.rails(plate.deflate(lip), 1.dp.toPx())) {
                    drawRoundRect(
                        color = tokens.bezel.copy(alpha = 0.68f),
                        topLeft = groove.topLeft,
                        size = groove.size,
                        cornerRadius = CornerRadius(groove.height / 2f),
                    )
                }
            }
            val utility = controls.filterKeys { it in setOf("start", "select", "menu") }
            val rail =
                utility.values.takeIf { it.isNotEmpty() }?.let { seats ->
                    Rect(
                        seats.minOf { it.left },
                        seats.minOf { it.top },
                        seats.maxOf { it.right },
                        seats.maxOf { it.bottom },
                    ).takeIf { band ->
                        controls.filterKeys { it !in utility }.values.none {
                            CartdeckHardwareGeometry.intersects(it, band)
                        } && band.height <= 56.dp.toPx()
                    }
                }
            for (pod in listOfNotNull(rail)) {
                if (pod.left < 0f || pod.top < (video?.bottom ?: 0f) ||
                    pod.right > size.width || pod.bottom > size.height
                ) {
                    continue
                }
                val corner = minOf(13.dp.toPx(), minOf(pod.width, pod.height) / 4f)
                drawControllerInset(
                    chamferedIsland(pod, corner),
                    chamferedIsland(pod.inflate(-3.dp.toPx()), corner / 2f),
                    pod,
                    tokens,
                    1.5.dp.toPx(),
                )
            }
            // Local machined wells below are preserved; no opaque full hull.
            // The face, D-pad well and shoulders get individual machined
            // seats. The existing caps/pressed feedback remain authoritative.
            val gameplaySeats =
                controls.filterKeys {
                    it.lowercase() !in setOf("start", "select", "menu")
                }
            for ((key, bounds) in gameplaySeats) {
                val center = bounds.center
                val radius = minOf(bounds.width, bounds.height) / 2f
                when (PivotArcPaintPolicy.materialKey(key)) {
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
                    "ANALOG" -> {
                        val collar = radius - 3.dp.toPx()
                        val inner = collar - 5.dp.toPx()
                        drawCircle(tokens.bezel, radius = collar, center = center)
                        drawCircle(
                            tokens.secondary,
                            radius = collar - 1.dp.toPx(),
                            center = center,
                            style = Stroke(width = 2.dp.toPx()),
                        )
                        drawCircle(
                            tokens.accent.copy(alpha = 0.52f),
                            radius = inner,
                            center = center,
                            style = Stroke(width = 1.dp.toPx()),
                        )
                        for (angle in listOf(42f, 132f, 222f, 312f)) {
                            drawArc(
                                tokens.onChassis.copy(alpha = 0.42f),
                                startAngle = angle,
                                sweepAngle = 6f,
                                useCenter = false,
                                topLeft = center - Offset(collar, collar),
                                size = Size(collar * 2f, collar * 2f),
                                style = Stroke(width = 1.3.dp.toPx()),
                            )
                        }
                    }
                    "D" -> {
                        val margin = 3.dp.toPx()
                        val wellTop = Offset(bounds.left + margin, bounds.top + margin)
                        val wellSize =
                            Size(
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
                            size =
                                Size(
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
                    size =
                        Size(
                            (bounds.width - 4.dp.toPx()).coerceAtLeast(0f),
                            (bounds.height - 4.dp.toPx()).coerceAtLeast(0f),
                        ),
                    cornerRadius = CornerRadius(9.dp.toPx()),
                )
            }
            if (joinedIsland == null) {
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
        }

        module.wordmark?.takeIf { showWordmark }?.let { region ->
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
