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
        // Inscribed image fits inside the 1.414x safety square at ANY allowed rotation.
        val side = seat.width / sqrt(2f)
        val left = seat.center.x - side / 2f
        val top = seat.center.y - side / 2f
        rotate(degrees = decal.rotation, pivot = seat.center) {
            drawImage(
                image = asset!!,
                dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                dstSize = IntSize(side.roundToInt(), side.roundToInt()),
                alpha = decal.opacity,
            )
        }
    }
}
