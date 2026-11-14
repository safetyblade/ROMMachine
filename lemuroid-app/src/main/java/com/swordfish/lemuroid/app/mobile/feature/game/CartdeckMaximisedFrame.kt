package com.swordfish.lemuroid.app.mobile.feature.game

import android.graphics.Rect as AndroidRect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartdeckEditionTokens

/**
 * Native-host game-first presentation: a real, measured maximised video aperture
 * with a narrow CARTDECK edition perimeter. No PadKit, hardware seats or virtual keys.
 * The renderer is sized to the INNER rectangle, so the trim cannot crop its image.
 */
@Composable
internal fun CartdeckMaximisedFrame(
    tokens: CartdeckEditionTokens,
    onViewport: (AndroidRect) -> Unit,
) {
    val trim = 6.dp
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val edge = trim.toPx()
            drawRoundRect(
                color = tokens.chassis,
                topLeft = Offset(edge / 2f, edge / 2f),
                size = Size(size.width - edge, size.height - edge),
                cornerRadius = CornerRadius(10.dp.toPx()),
                style = Stroke(edge),
            )
            drawRoundRect(
                color = tokens.accent.copy(alpha = .75f),
                topLeft = Offset(edge, edge),
                size = Size(size.width - edge * 2, size.height - edge * 2),
                cornerRadius = CornerRadius(5.dp.toPx()),
                style = Stroke(1.dp.toPx()),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .padding(trim)
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInRoot()
                    if (bounds.width > 0 && bounds.height > 0) {
                        onViewport(
                            AndroidRect(
                                bounds.left.toInt(),
                                bounds.top.toInt(),
                                bounds.right.toInt(),
                                bounds.bottom.toInt(),
                            ),
                        )
                    }
                },
        )
    }
}
