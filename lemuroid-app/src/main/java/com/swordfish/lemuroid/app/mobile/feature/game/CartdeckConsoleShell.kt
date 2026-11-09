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
) {
    val hole =
        if (videoBounds != null && rootBounds != null) {
            Rect(
                videoBounds.left - rootBounds.left,
                videoBounds.top - rootBounds.top,
                videoBounds.right - rootBounds.left,
                videoBounds.bottom - rootBounds.top,
            ).takeIf { it.width > 0f && it.height > 0f }
        } else {
            null
        }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
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
            hole?.let { display ->
                val border = 15.dp.toPx()
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
                        addRoundRect(RoundRect(rim, CornerRadius(18.dp.toPx())))
                        addRoundRect(RoundRect(display, CornerRadius(2.dp.toPx())))
                    }
                drawPath(bezel, tokens.bezel)
                drawRoundRect(
                    color = tokens.accent.copy(alpha = 0.50f),
                    topLeft = rim.topLeft,
                    size = rim.size,
                    cornerRadius = CornerRadius(18.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx()),
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
            drawLine(
                color = tokens.secondary,
                start = Offset(left, size.height - 30.dp.toPx()),
                end = Offset(size.width - left, size.height - 30.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
            )
        }

        // Reserve a physical brand seat above the measured game aperture.
        // Compact devices lose the wordmark first; game pixels and touch targets
        // always take precedence over decorative branding.
        val badgeTop = 28.dp
        val badgeHeight = 39.dp
        val badgeClearance = 8.dp
        val availableAboveVideo = hole?.top ?: 0f
        val seatFits =
            availableAboveVideo >= with(density) { (badgeTop + badgeHeight + badgeClearance).toPx() }
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
    }
}
