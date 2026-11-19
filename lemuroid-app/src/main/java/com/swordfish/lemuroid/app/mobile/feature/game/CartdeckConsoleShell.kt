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
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.shelf.CartdeckDecalLayer
import com.swordfish.lemuroid.app.mobile.feature.shelf.CartdeckLighting
import com.swordfish.lemuroid.app.mobile.feature.shelf.CartdeckPersonalDecal
import com.swordfish.lemuroid.app.mobile.feature.shelf.CartdeckSurfaceDesign
import com.swordfish.lemuroid.app.mobile.feature.shelf.colorContrast
import com.swordfish.lemuroid.app.mobile.feature.shelf.drawCartdeckLighting
import com.swordfish.lemuroid.app.mobile.feature.shelf.drawCartdeckSurfaceDesign
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
    patternId: String = CartdeckSurfaceDesign.PATTERN_NONE,
    stampId: String = CartdeckSurfaceDesign.STAMP_NONE,
    decal: CartdeckPersonalDecal? = null,
    lighting: CartdeckLighting = CartdeckLighting(),
    highContrast: Boolean = false,
    feedbackStrength: Float = 0f,
    isDualScreen: Boolean = false,
    minimal: Boolean = false,
) {
    val hole = CartdeckHardwareGeometry.inRoot(videoBounds, rootBounds)
    val controls = controlBounds.mapNotNull { CartdeckHardwareGeometry.inRoot(it, rootBounds) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        if (minimal) {
            Canvas(Modifier.fillMaxSize()) {
                if (hole != null) {
                    val trim = 3.dp.toPx()
                    val outer =
                        Rect(
                            (hole.left - trim).coerceAtLeast(0f),
                            (hole.top - trim).coerceAtLeast(0f),
                            (hole.right + trim).coerceAtMost(size.width),
                            (hole.bottom + trim).coerceAtMost(size.height),
                        )
                    val frame =
                        Path().apply {
                            fillType = PathFillType.EvenOdd
                            addRect(outer)
                            addRect(hole)
                        }
                    drawPath(frame, manufacturedFrameAccent(tokens, lighting, highContrast))
                }
            }
            return@BoxWithConstraints
        }
        val badgeTop = 25.dp
        val badgeHeight = 39.dp
        val badgeClearance = 9.dp
        val availableAboveVideo = hole?.top ?: 0f
        val seatFits =
            availableAboveVideo >= with(density) { (badgeTop + badgeHeight + badgeClearance).toPx() }
        val compactSeat = !isLandscape && !seatFits
        val landscapeSeat =
            isLandscape && hole != null && controls.isNotEmpty() &&
                CartdeckManufacturedGeometry.footerFits(
                    with(density) { maxWidth.toPx() },
                    with(density) { maxHeight.toPx() },
                    hole,
                    controls,
                    density.density,
                )
        val footerSeat = compactSeat || landscapeSeat
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

            // Responsive, noninteractive decoration; a valid measured aperture
            // is required before painting. Never paint into video, PadKit
            // bounds, or the protected CARTDECK brand seat.
            if (hole != null && !isLandscape) {
                val reservedBrand =
                    when {
                        !isLandscape && seatFits ->
                            Rect(
                                size.width / 2f - 120.dp.toPx(),
                                8.dp.toPx(),
                                size.width / 2f + 120.dp.toPx(),
                                80.dp.toPx(),
                            )
                        footerSeat ->
                            Rect(
                                size.width / 2f - 50.dp.toPx(),
                                size.height - 63.dp.toPx(),
                                size.width / 2f + 50.dp.toPx(),
                                size.height,
                            )
                        else -> null
                    }
                val exclusions = listOf(hole) + controls + listOfNotNull(reservedBrand)
                drawCartdeckSurfaceDesign(
                    patternId = patternId,
                    stampId = stampId,
                    frame =
                        Rect(
                            edge + 7.dp.toPx(),
                            edge + 7.dp.toPx(),
                            size.width - edge - 7.dp.toPx(),
                            size.height - edge - 7.dp.toPx(),
                        ),
                    exclusions = exclusions,
                    lineColour = tokens.accent,
                    stampColour = tokens.onChassis,
                    cellSize = maxOf(23.dp.toPx(), minOf(size.width, size.height) / 14f),
                    clearance = 7.dp.toPx(),
                )
            }
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
            if (isLandscape && hole != null && controls.isNotEmpty()) {
                val frame = Rect(0f, 0f, size.width, size.height)
                CartdeckManufacturedGeometry.sideHousings(frame, hole, controls, this.density).forEach { panel ->
                    drawRoundRect(
                        color = tokens.secondary,
                        topLeft = panel.topLeft,
                        size = panel.size,
                        cornerRadius = CornerRadius(12.dp.toPx()),
                    )
                    drawRoundRect(
                        color = tokens.bezel,
                        topLeft = panel.topLeft,
                        size = panel.size,
                        cornerRadius = CornerRadius(12.dp.toPx()),
                        style = Stroke(2.dp.toPx()),
                    )
                    drawRoundRect(
                        color = hardwareInlay(tokens),
                        topLeft = Offset(panel.left + 2.dp.toPx(), panel.top + 2.dp.toPx()),
                        size =
                            androidx.compose.ui.geometry.Size(
                                panel.width - 4.dp.toPx(),
                                panel.height - 4.dp.toPx(),
                            ),
                        cornerRadius = CornerRadius(10.dp.toPx()),
                        style = Stroke(1.dp.toPx()),
                    )
                }
            }
            // A landscape appearance is composed in intentional paired engraving bands,
            // not scattered across leftover pixels or the clean controller surfaces.
            if (isLandscape && hole != null) {
                val decoration =
                    CartdeckManufacturedGeometry.landscapeDecoration(
                        Rect(0f, 0f, size.width, size.height),
                        hole,
                        controls,
                        this.density,
                        decal?.anchor,
                        if (footerSeat) {
                            listOf(
                                Rect(
                                    size.width / 2f - 54.dp.toPx(),
                                    size.height - 66.dp.toPx(),
                                    size.width / 2f + 54.dp.toPx(),
                                    size.height,
                                ),
                            )
                        } else {
                            emptyList()
                        },
                    )
                clipPath(shellPath) {
                    decoration.bands.forEach { band ->
                        drawCartdeckSurfaceDesign(
                            patternId = patternId,
                            stampId = CartdeckSurfaceDesign.STAMP_NONE,
                            frame = band,
                            exclusions = emptyList(),
                            lineColour = tokens.accent,
                            stampColour = tokens.onChassis,
                            cellSize = 16.dp.toPx(),
                            clearance = 0f,
                        )
                    }
                    decoration.stampSeat?.let { seat ->
                        drawCartdeckSurfaceDesign(
                            patternId = CartdeckSurfaceDesign.PATTERN_NONE,
                            stampId = stampId,
                            frame = seat,
                            exclusions = emptyList(),
                            lineColour = tokens.accent,
                            stampColour = tokens.onChassis,
                            cellSize = 16.dp.toPx(),
                            clearance = 0f,
                            stampSeatOverride = seat,
                        )
                    }
                }
            }
            hole?.let { display ->
                val dsPortrait = isDualScreen && !isLandscape
                val border = if (dsPortrait) 3.dp.toPx() else 7.dp.toPx()
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
                    color = hardwareInlay(tokens).copy(alpha = if (dsPortrait) 0.55f else 0.9f),
                    topLeft = rim.topLeft,
                    size = rim.size,
                    cornerRadius = CornerRadius(10.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
            val left = 25.dp.toPx()
            if (hole != null) {
                val frame = Rect(0f, 0f, size.width, size.height)
                val brand =
                    if (footerSeat) {
                        listOf(
                            Rect(
                                size.width / 2f - 54.dp.toPx(),
                                size.height - 66.dp.toPx(),
                                size.width / 2f + 54.dp.toPx(),
                                size.height,
                            ),
                        )
                    } else {
                        emptyList()
                    }
                drawManufacturedChannels(
                    channels =
                        CartdeckManufacturedGeometry.channels(
                            frame,
                            hole,
                            controls + brand,
                            this.density,
                            isDualScreen && !isLandscape,
                            includeCrown = !minimal,
                        ),
                    tokens = tokens,
                    lighting = lighting,
                    highContrast = highContrast,
                    feedbackStrength = feedbackStrength,
                )
            }

            // Decorative lower seam uses live PadKit group bounds and the
            // protected compact badge. Neither Start nor Select is repositioned.
            val deckLineY = size.height - 30.dp.toPx()
            val compactBadge =
                if (footerSeat) {
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
            if (!minimal) {
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
            }
        }

        if (lighting.mode != CartdeckLighting.MODE_OFF && hole != null) {
            Canvas(Modifier.fillMaxSize()) {
                val brandSeat =
                    when {
                        !isLandscape && seatFits ->
                            Rect(
                                size.width / 2f - 120.dp.toPx(),
                                0f,
                                size.width / 2f + 120.dp.toPx(),
                                84.dp.toPx(),
                            )
                        footerSeat ->
                            Rect(
                                size.width / 2f - 54.dp.toPx(),
                                size.height - 66.dp.toPx(),
                                size.width / 2f + 54.dp.toPx(),
                                size.height,
                            )
                        else -> null
                    }
                drawCartdeckLighting(
                    lighting = lighting.copy(zones = lighting.zones.intersect(setOf(CartdeckLighting.CONTROL_WELLS))),
                    accent = tokens.accent,
                    video = hole,
                    controls = controls,
                    brand = listOfNotNull(brandSeat),
                    highContrast = highContrast,
                    thickness = 2.5.dp.toPx(),
                    clearance = 8.dp.toPx(),
                    feedbackStrength = feedbackStrength,
                )
            }
        }

        if (decal != null && hole != null && !minimal) {
            val w = with(density) { maxWidth.toPx() }
            val h = with(density) { maxHeight.toPx() }
            val protectedBrand =
                if (!isLandscape && seatFits) {
                    val half = with(density) { 120.dp.toPx() }
                    val bottom = with(density) { 84.dp.toPx() }
                    Rect(w / 2f - half, 0f, w / 2f + half, bottom)
                } else if (footerSeat) {
                    val half = with(density) { 54.dp.toPx() }
                    val top = h - with(density) { 66.dp.toPx() }
                    Rect(w / 2f - half, top, w / 2f + half, h)
                } else {
                    null
                }
            val dedicatedSeat =
                if (isLandscape) {
                    CartdeckManufacturedGeometry.landscapeDecoration(
                        Rect(0f, 0f, w, h),
                        hole,
                        controls,
                        density.density,
                        decal.anchor,
                        listOfNotNull(protectedBrand),
                    ).decalSeat
                } else {
                    null
                }
            if (!isLandscape || dedicatedSeat != null) {
                CartdeckDecalLayer(
                    decal = decal,
                    exclusions = listOf(hole) + controls + listOfNotNull(protectedBrand),
                    dedicatedSeat = dedicatedSeat,
                )
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
        if (footerSeat) {
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
