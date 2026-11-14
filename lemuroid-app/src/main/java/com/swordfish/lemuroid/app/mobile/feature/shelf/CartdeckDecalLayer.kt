package com.swordfish.lemuroid.app.mobile.feature.shelf

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Passive visual image. Parent supplies the current local-space measured video,
 * native touch rectangles and protected brand areas. Never reserves layout space.
 */
@Composable
internal fun CartdeckDecalLayer(
    decal: CartdeckPersonalDecal?,
    exclusions: List<Rect>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val asset by produceState<ImageBitmap?>(initialValue = null, key1 = decal?.assetId) {
        value = decal?.let { CartdeckDecalStore(context).readImage(it.assetId) }
    }
    if (decal == null || asset == null) return
    Canvas(modifier.fillMaxSize()) {
        val margin = 12.dp.toPx()
        val seat =
            CartdeckDecalGeometry.place(
                frame = Rect(margin, margin, size.width - margin, size.height - margin),
                exclusions = exclusions,
                decal = decal,
                baseSize = 98.dp.toPx(),
                clearance = 8.dp.toPx(),
            ) ?: return@Canvas
        // The image is aspect-contained in the inscribed square. Its rotation
        // stays inside the larger exclusion-tested envelope.
        val image = asset ?: return@Canvas
        val side = seat.width / sqrt(2f)
        val aspect = image.width.toFloat() / image.height.toFloat()
        val width = if (aspect >= 1f) side else side * aspect
        val height = if (aspect >= 1f) side / aspect else side
        val left = seat.center.x - width / 2f
        val top = seat.center.y - height / 2f
        rotate(degrees = decal.rotation, pivot = seat.center) {
            drawImage(
                image = image,
                dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                dstSize = IntSize(width.roundToInt(), height.roundToInt()),
                alpha = decal.opacity,
            )
        }
    }
}
