package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartdeckEditionTokens
import android.graphics.Rect as AndroidRect

/**
 * Game-first native host display. Its actual SurfaceView is positioned inside
 * this measured rectangle; no second renderer, viewport crop or touch owner.
 * TV keeps overscan clearance in addition to Android safeDrawing insets.
 */
@Composable
internal fun CartdeckMaximisedFrame(
    tokens: CartdeckEditionTokens,
    onViewport: (AndroidRect) -> Unit,
    presentationAspect: Float = 4f / 3f,
    isTv: Boolean = false,
    showBorder: Boolean = true,
) {
    val trim = if (showBorder) 6.dp else 0.dp
    BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        val density = LocalDensity.current
        val safeMargin = if (isTv) 32.dp else trim
        val availableWidth = (maxWidth - safeMargin * 2).coerceAtLeast(0.dp)
        val availableHeight = (maxHeight - safeMargin * 2).coerceAtLeast(0.dp)
        // Work in Dp after converting the one-pixel raster guard. The final
        // AndroidRect is rounded inward, never spilling into the glass.
        val guardDp = with(density) { 1f.toDp() }
        val fit =
            NativeViewportPolicy.fit(
                availableWidth.value,
                availableHeight.value,
                presentationAspect,
                guardDp.value,
            )
        if (showBorder) {
            Canvas(Modifier.fillMaxSize()) {
                val edge = trim.toPx()
                drawRoundRect(
                    color = tokens.chassis,
                    topLeft = Offset(edge / 2f, edge / 2f),
                    size = Size((size.width - edge).coerceAtLeast(0f), (size.height - edge).coerceAtLeast(0f)),
                    cornerRadius = CornerRadius(10.dp.toPx()),
                    style = Stroke(edge),
                )
                drawRoundRect(
                    color = tokens.accent.copy(alpha = 0.75f),
                    topLeft = Offset(edge, edge),
                    size =
                        Size(
                            (size.width - edge * 2f).coerceAtLeast(0f),
                            (size.height - edge * 2f).coerceAtLeast(0f),
                        ),
                    cornerRadius = CornerRadius(5.dp.toPx()),
                    style = Stroke(1.dp.toPx()),
                )
            }
        }
        if (fit.width > 0f && fit.height > 0f) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(fit.width.dp, fit.height.dp)
                    .onGloballyPositioned { coordinates ->
                        val bounds = coordinates.boundsInRoot()
                        NativeViewportPolicy.inwardPixels(
                            bounds.left,
                            bounds.top,
                            bounds.right,
                            bounds.bottom,
                        )?.let { pixels ->
                            onViewport(
                                AndroidRect(pixels.left, pixels.top, pixels.right, pixels.bottom),
                            )
                        }
                    },
            )
        }
    }
}
