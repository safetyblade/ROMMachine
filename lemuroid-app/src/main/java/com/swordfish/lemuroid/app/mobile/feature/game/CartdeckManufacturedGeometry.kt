package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import com.swordfish.lemuroid.app.mobile.feature.shelf.CartdeckLighting
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartdeckEditionTokens

internal data class CartdeckHardwareChannel(
    val bounds: Rect,
    val zone: String,
    val housing: Rect? = null,
    val glowBounds: Rect? = null,
)

/** Measured geometry. Decorative trim disappears rather than moving a game or input. */
internal object CartdeckManufacturedGeometry {
    private fun fits(
        rect: Rect,
        frame: Rect,
        blocked: List<Rect>,
        clearance: Float,
    ): Boolean =
        rect.width > 0f && rect.height > 0f &&
            rect.left >= frame.left && rect.right <= frame.right &&
            rect.top >= frame.top && rect.bottom <= frame.bottom &&
            blocked.none {
                CartdeckHardwareGeometry.intersects(rect, CartdeckHardwareGeometry.expanded(it, clearance))
            }

    fun channels(
        frame: Rect,
        video: Rect,
        controls: List<Rect>,
        density: Float,
        dualScreen: Boolean,
        includeCrown: Boolean = true,
    ): List<CartdeckHardwareChannel> {
        if (!density.isFinite() || density <= 0f || frame.width <= 0f || frame.height <= 0f) return emptyList()
        val blocked = listOf(video) + controls
        val edge = 9f * density
        val safe = Rect(frame.left + edge, frame.top + edge, frame.right - edge, frame.bottom - edge)
        val tracks = mutableListOf<CartdeckHardwareChannel>()

        fun glowBounds(rect: Rect): Rect? =
            CartdeckHardwareGeometry.expanded(rect, 1.2f * density).takeIf {
                fits(it, safe, blocked, 0.5f * density)
            }

        fun group(
            zone: String,
            rects: List<Rect>,
        ) {
            if (rects.all { fits(it, safe, blocked, 0.5f * density) }) {
                tracks += rects.map { CartdeckHardwareChannel(it, zone, glowBounds = glowBounds(it)) }
            }
        }

        val inset = (if (dualScreen) 1.5f else 4f) * density
        val thickness = (if (dualScreen) 0.9f else 1.6f) * density
        val trim = 9f * density
        if (video.height > trim * 2f) {
            val left = video.left - inset
            val right = video.right + inset
            group(
                CartdeckLighting.SIDE_RAILS,
                listOf(
                    Rect(left - thickness / 2f, video.top + trim, left + thickness / 2f, video.bottom - trim),
                    Rect(right - thickness / 2f, video.top + trim, right + thickness / 2f, video.bottom - trim),
                ),
            )
        }
        if (video.width > trim * 2f) {
            val top = video.top - inset
            val bottom = video.bottom + inset
            group(
                CartdeckLighting.SCREEN,
                listOf(
                    Rect(video.left + trim, top - thickness / 2f, video.right - trim, top + thickness / 2f),
                    Rect(video.left + trim, bottom - thickness / 2f, video.right - trim, bottom + thickness / 2f),
                ),
            )
        }

        // A common horizontal datum, mirrored side tracks and centre indicator.
        // The 40dp horizontal inset clears the manufactured 30dp chamfer.
        if (includeCrown && frame.width >= 270f * density && video.top >= frame.top + 25f * density) {
            val center = frame.center.x
            val y = frame.top + 16f * density
            val start = frame.left + 40f * density
            val end = minOf(frame.left + frame.width * 0.30f, center - 62f * density)
            val half = 1.5f * density
            val housing =
                Rect(
                    center - 48f * density,
                    y - 6f * density,
                    center + 48f * density,
                    y + 6f * density,
                )
            val crown =
                listOf(
                    Rect(start, y - half, end, y + half),
                    Rect(center - 15f * density, y - half, center + 15f * density, y + half),
                    Rect(2f * center - end, y - half, 2f * center - start, y + half),
                )
            if (
                end - start >= 14f * density && fits(housing, safe, blocked, 0.5f * density) &&
                crown.all { fits(it, safe, blocked, 0.5f * density) }
            ) {
                tracks +=
                    crown.mapIndexed { index, rect ->
                        CartdeckHardwareChannel(
                            rect,
                            CartdeckLighting.SIDE_RAILS,
                            housing.takeIf { index == 1 },
                            glowBounds(rect),
                        )
                    }
            }
        }
        return tracks
    }

    fun footerFits(
        width: Float,
        height: Float,
        video: Rect,
        controls: List<Rect>,
        density: Float,
    ): Boolean {
        val frame = Rect(0f, 0f, width, height)
        val badge =
            Rect(
                width / 2f - 54f * density,
                height - 62f * density,
                width / 2f + 54f * density,
                height - 9f * density,
            )
        return fits(badge, frame, listOf(video) + controls, 4f * density)
    }

    fun sideHousings(
        frame: Rect,
        video: Rect,
        controls: List<Rect>,
        density: Float,
    ): List<Rect> {
        val edge = 12f * density
        val gap = 10f * density
        val verticalInset = 38f * density
        val sides =
            listOf(
                Rect(frame.left + edge, frame.top + verticalInset, video.left - gap, frame.bottom - verticalInset),
                Rect(video.right + gap, frame.top + verticalInset, frame.right - edge, frame.bottom - verticalInset),
            )
        return sides.filter { panel ->
            panel.width >= 24f * density && panel.height >= 32f * density &&
                fits(panel, frame, listOf(video), density) &&
                controls.any { CartdeckHardwareGeometry.intersects(it, panel) }
        }
    }
}

/** No extra renderer, animation loop or input listener. */
internal fun DrawScope.drawManufacturedChannels(
    channels: List<CartdeckHardwareChannel>,
    tokens: CartdeckEditionTokens,
    lighting: CartdeckLighting,
    highContrast: Boolean,
    feedbackStrength: Float,
) {
    channels.forEach { channel ->
        channel.housing?.let { housing ->
            drawRoundRect(
                color = tokens.secondary,
                topLeft = housing.topLeft,
                size = housing.size,
                cornerRadius = CornerRadius(housing.height / 3f),
            )
        }
        drawMaterialTrack(
            channel.bounds,
            tokens,
            lighting,
            channel.zone,
            highContrast,
            feedbackStrength,
            channel.glowBounds,
        )
    }
}

/** Permanent accent material is independent of emitted light colour. */
internal fun hardwareInlay(tokens: CartdeckEditionTokens): Color =
    lerp(tokens.bezel, tokens.accent, 0.76f)

/** Soft layered emission stays inside the validated hardware footprint. */
internal fun DrawScope.drawMaterialTrack(
    rect: Rect,
    tokens: CartdeckEditionTokens,
    lighting: CartdeckLighting,
    zone: String,
    highContrast: Boolean,
    feedbackStrength: Float = 0f,
    glowBounds: Rect? = null,
) {
    val radius = minOf(rect.width, rect.height) / 2f
    if (radius <= 0f) return

    fun layer(
        fraction: Float,
        colour: Color,
    ) {
        val inset = radius * fraction
        drawRoundRect(
            color = colour,
            topLeft = Offset(rect.left + inset, rect.top + inset),
            size = Size(rect.width - inset * 2f, rect.height - inset * 2f),
            cornerRadius = CornerRadius(radius - inset),
        )
    }

    val active = lighting.active(zone, highContrast)
    val light = lighting.tint(tokens.accent, feedbackStrength)
    if (active && glowBounds != null) {
        drawRoundRect(
            color = light.copy(alpha = light.alpha * 0.09f),
            topLeft = glowBounds.topLeft,
            size = glowBounds.size,
            cornerRadius = CornerRadius(minOf(glowBounds.width, glowBounds.height) / 2f),
        )
    }
    layer(0.08f, tokens.bezel)
    layer(0.48f, hardwareInlay(tokens))
    if (active) {
        layer(0.06f, light.copy(alpha = light.alpha * 0.16f))
        layer(0.28f, light.copy(alpha = light.alpha * 0.34f))
        layer(0.55f, light.copy(alpha = light.alpha * 0.82f))
    }
}

/** Minimal surround retains a material finish when lighting is off. */
internal fun manufacturedFrameAccent(
    tokens: CartdeckEditionTokens,
    lighting: CartdeckLighting,
    highContrast: Boolean,
): Color =
    when {
        highContrast -> Color.White
        lighting.active(CartdeckLighting.SCREEN) -> lighting.tint(tokens.accent)
        else -> hardwareInlay(tokens)
    }
