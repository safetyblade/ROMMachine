package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.shelf.colorContrast
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartdeckEditionTokens

/**
 * The only image aperture is measured from the existing LibretroDroid renderer.
 * This purely visual layer does not own touch gestures or game render lifecycle.
 */
@Composable
internal fun CartdeckConsoleShell(
    tokens: CartdeckEditionTokens,
    videoBounds: Rect?,
    rootBounds: Rect?,
    isLandscape: Boolean,
    controlBounds: List<Rect> = emptyList(),
) {
    val hole = CartdeckHardwareGeometry.inRoot(videoBounds, rootBounds)
    val controls = controlBounds.mapNotNull { CartdeckHardwareGeometry.inRoot(it, rootBounds) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val badgeTop = 25.dp
        val badgeHeight = 39.dp
        val badgeClearance = 9.dp
        val availableAboveVideo = hole?.top ?: 0f
        val seatFits =
            availableAboveVideo >= with(density) { (badgeTop + badgeHeight + badgeClearance).toPx() }
        val compactSeat = !isLandscape && !seatFits
        Canvas(Modifier.fillMaxSize()) {
            val edge = 8.dp.toPx()
            val chamfer = 30.dp.toPx()
            val shellPath =
                Path().apply {
                    fillType = PathFillType.EvenOdd
                    moveTo(edge + chamfer, edge)
                    lineTo(size.width - edge - chamfer, edge)
                    lineTo(size.width - edge, edge + chamfer)
                    lineTo(size.width - edge, size.height - edge - chamfer)
                    lineTo(size.width - edge - chamfer, size.height - edge)
                    lineTo(edge + chamfer, size.height - edge)
                    lineTo(edge, size.height - edge - chamfer)
                    lineTo(edge, edge + chamfer)
                    close()
                    hole?.let { addRoundRect(RoundRect(it, CornerRadius(2.dp.toPx()))) }
                }
            drawPath(shellPath, tokens.chassis)
            // Draw the P1-C2A solid control-deck.svg geometry directly on
            // the noninteractive shell. That places decorative hardware seats
            // ABOVE the manufactured deck but BELOW the functional PadKit.
            if (!isLandscape && controls.isNotEmpty()) {
                val earliestControl = controls.minOf { it.top }
                val rimBottom = (hole?.bottom ?: 0f) + 12.dp.toPx()
                val deckTop = maxOf(rimBottom, earliestControl - 23.dp.toPx())
                val deckBottom = size.height - 5.dp.toPx()
                val inset = 9.dp.toPx()
                if (deckBottom - deckTop > 25.dp.toPx()) {
                    val cut = minOf(21.dp.toPx(), (deckBottom - deckTop) * 0.12f)
                    val panel =
                        Path().apply {
                            moveTo(inset + cut, deckTop)
                            lineTo(size.width - inset - cut, deckTop)
                            lineTo(size.width - inset, deckTop + cut)
                            lineTo(size.width - inset, deckBottom - cut)
                            lineTo(size.width - inset - cut, deckBottom)
                            lineTo(inset + cut, deckBottom)
                            lineTo(inset, deckBottom - cut)
                            lineTo(inset, deckTop + cut)
                            close()
                        }
                    drawPath(panel, tokens.secondary)
                    drawPath(
                        panel,
                        tokens.bezel,
                        style = Stroke(width = 3.dp.toPx()),
                    )
                }
            }
            hole?.let { display ->
                val border = 7.dp.toPx()
                val rim =
                    Rect(
                        display.left - border,
                        display.top - border,
                        display.right + border,
                        display.bottom + border,
                    )
                val bezel =
                    Path().apply {
                        fillType = PathFillType.EvenOdd
                        addRoundRect(RoundRect(rim, CornerRadius(10.dp.toPx())))
                        addRoundRect(RoundRect(display, CornerRadius(2.dp.toPx())))
                    }
                drawPath(bezel, tokens.bezel)
                // A black-glass lip outside (not on top of) actual video pixels.
                val glassLip =
                    Path().apply {
                        fillType = PathFillType.EvenOdd
                        addRoundRect(
                            RoundRect(
                                Rect(
                                    display.left - 2.dp.toPx(),
                                    display.top - 2.dp.toPx(),
                                    display.right + 2.dp.toPx(),
                                    display.bottom + 2.dp.toPx(),
                                ),
                                CornerRadius(3.dp.toPx()),
                            ),
                        )
                        addRoundRect(RoundRect(display, CornerRadius(2.dp.toPx())))
                    }
                drawPath(glassLip, tokens.glass)
                drawRoundRect(
                    color = tokens.accent.copy(alpha = 0.50f),
                    topLeft = rim.topLeft,
                    size = rim.size,
                    cornerRadius = CornerRadius(10.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
            val left = 25.dp.toPx()
            val end = size.width * 0.20f
            val lightY = 19.dp.toPx()
            // Never paint decorative shell lighting over a maximised viewport.
            // The old fixed top clearance guaranteed room for this detail;
            // screen-first layouts may intentionally remove that clearance.
            if (hole == null || hole.top > lightY + 2.dp.toPx()) {
                drawLine(
                    color = tokens.accent.copy(alpha = 0.6f),
                    start = Offset(left, lightY),
                    end = Offset(end, lightY),
                    strokeWidth = 2.dp.toPx(),
                )
                drawLine(
                    color = tokens.accent.copy(alpha = 0.6f),
                    start = Offset(size.width - end, lightY),
                    end = Offset(size.width - left, lightY),
                    strokeWidth = 2.dp.toPx(),
                )
            }
            // Decorative lower seam uses live PadKit group bounds and the
            // protected compact badge. Neither Start nor Select is repositioned.
            val deckLineY = size.height - 30.dp.toPx()
            val compactBadge =
                if (compactSeat) {
                    listOf(
                        Rect(
                            size.width / 2f - 40.dp.toPx(),
                            size.height - 52.dp.toPx(),
                            size.width / 2f + 40.dp.toPx(),
                            size.height - 5.dp.toPx(),
                        ),
                    )
                } else {
                    emptyList()
                }
            CartdeckHardwareGeometry.seamSegments(
                left = left,
                right = size.width - left,
                y = deckLineY,
                exclusions = controls + compactBadge + listOfNotNull(hole),
                clearance = 4.dp.toPx(),
            ).forEach { (start, finish) ->
                drawLine(
                    color = tokens.secondary,
                    start = Offset(start, deckLineY),
                    end = Offset(finish, deckLineY),
                    strokeWidth = 2.dp.toPx(),
                )
            }
            // These speaker and indicator details use exposed manufactured
            // chassis, not the renderer's letterboxing or PadKit hit regions.
            if (!isLandscape && seatFits) {
                hole?.let { display ->
                    val detailY = display.bottom + 24.dp.toPx()
                    val speakerClearance =
                        Rect(
                            size.width * 0.19f,
                            detailY - 6.dp.toPx(),
                            size.width * 0.81f,
                            detailY + 6.dp.toPx(),
                        )
                    if (
                        detailY < size.height * 0.67f &&
                        controls.none { CartdeckHardwareGeometry.intersects(it, speakerClearance) }
                    ) {
                        for (index in 0 until 5) {
                            val offset = index * 7.dp.toPx()
                            drawCircle(
                                color = tokens.secondary,
                                radius = 1.8.dp.toPx(),
                                center = Offset(size.width * 0.23f + offset, detailY),
                            )
                            drawCircle(
                                color = tokens.secondary,
                                radius = 1.8.dp.toPx(),
                                center = Offset(size.width * 0.77f - offset, detailY),
                            )
                        }
                        drawLine(
                            color = tokens.accent.copy(alpha = 0.65f),
                            start = Offset(size.width * 0.44f, detailY),
                            end = Offset(size.width * 0.56f, detailY),
                            strokeWidth = 2.dp.toPx(),
                        )
                    }
                }
            }
        }

        // Full brand returns above the standard manufactured screen; the
        // expanded DS layout keeps the full height and uses a footer C-slot.
        if (!isLandscape && seatFits) {
            val fullWordmarkFits = maxWidth >= 360.dp
            val inks = listOf(tokens.onChassis, tokens.onButton, Color.White, Color.Black)
            val brandInk = inks.maxBy { colorContrast(it, tokens.secondary) }
            Row(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = badgeTop)
                        .background(tokens.secondary, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.cartdeck_original_cslot_master),
                    contentDescription = "CARTDECK",
                    tint = brandInk,
                    modifier = Modifier.size(25.dp),
                )
                if (fullWordmarkFits) {
                    Icon(
                        painter = painterResource(R.drawable.cartdeck_wordmark_native),
                        contentDescription = null,
                        tint = brandInk,
                        modifier = Modifier.width(155.dp).height(25.dp),
                    )
                }
            }
        }
        if (compactSeat) {
            val inks = listOf(tokens.onChassis, tokens.onButton, Color.White, Color.Black)
            val brandInk = inks.maxBy { colorContrast(it, tokens.secondary) }
            Row(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 17.dp)
                        .background(tokens.secondary, RoundedCornerShape(7.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.cartdeck_original_cslot_master),
                    contentDescription = "CARTDECK",
                    tint = brandInk,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}
