package com.swordfish.lemuroid.app.mobile.feature.shelf

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.min

/**
 * A player's cosmetic lighting recipe. The default OFF matches the original
 * shipped app exactly. No emulator, input, audio, camera or ROM dependencies.
 */
data class CartdeckLighting(
    val mode: String = MODE_OFF,
    val intensity: Int = 65,
    val colour: Long? = null,
    val zones: Set<String> = DEFAULT_ZONES,
) {
    companion object {
        const val MODE_OFF = "off"
        const val MODE_STATIC = "static"
        const val MODE_FEEDBACK = "feedback"

        const val SCREEN = "screen"
        const val SIDE_RAILS = "side-rails"
        const val CONTROL_WELLS = "control-wells"
        const val SHELF_RAIL = "shelf-rail"

        val availableZones =
            linkedMapOf(
                SCREEN to "Screen surround",
                SIDE_RAILS to "Side rails",
                CONTROL_WELLS to "Control wells",
                SHELF_RAIL to "Cartridge shelf rail",
            )
        val DEFAULT_ZONES: Set<String> = availableZones.keys.toSet()
    }

    fun validate(): CartdeckLighting {
        require(mode in setOf(MODE_OFF, MODE_STATIC, MODE_FEEDBACK)) { "Unsupported lighting mode" }
        require(intensity in 0..100) { "Invalid lighting intensity" }
        require(colour == null || colour in 0L..0xFFFFFFFFL) { "Invalid lighting colour" }
        require(zones.all { it in availableZones }) { "Unsupported lighting zone" }
        return this
    }

    fun active(
        zone: String,
        highContrast: Boolean = false,
    ): Boolean = !highContrast && mode != MODE_OFF && intensity > 0 && zone in zones

    /**
     * Game Feedback adds no more than 35% of the baseline intensity, capped
     * at 100. Static and shelf-preview appearances never receive an event.
     */
    fun tint(
        accent: Color,
        feedbackStrength: Float = 0f,
    ): Color {
        val strength =
            if (mode == MODE_FEEDBACK && feedbackStrength.isFinite()) {
                feedbackStrength.coerceIn(0f, 1f)
            } else {
                0f
            }
        val baseline = intensity.toFloat()
        val boost = min(baseline * 0.35f, 100f - baseline) * strength
        return (colour?.let { Color(it.toInt()) } ?: accent)
            .copy(alpha = ((baseline + boost) / 100f) * 0.78f)
    }
}

/** Static manufacturing-style LED tracks; bounds are validated before drawing. */
internal object CartdeckLightingGeometry {
    fun candidates(
        frame: Rect,
        video: Rect,
        controls: List<Rect>,
        thickness: Float,
        gap: Float,
        settings: CartdeckLighting,
        protectedRects: List<Rect> = emptyList(),
    ): List<Rect> {
        if (settings.mode == CartdeckLighting.MODE_OFF || settings.intensity == 0) return emptyList()
        if (frame.width <= thickness * 4 || frame.height <= thickness * 4) return emptyList()
        val strips = mutableListOf<Rect>()
        val t = thickness.coerceAtLeast(1f)
        val margin = gap.coerceAtLeast(1f)
        if (CartdeckLighting.SCREEN in settings.zones) {
            // Place trim OUTSIDE the measured emulator aperture, never over a video pixel.
            val left = video.left - margin
            val right = video.right + margin
            val top = video.top - margin
            val bottom = video.bottom + margin
            strips += Rect(left, top, right, top + t)
            strips += Rect(left, bottom - t, right, bottom)
            strips += Rect(left, top, left + t, bottom)
            strips += Rect(right - t, top, right, bottom)
        }
        if (CartdeckLighting.SIDE_RAILS in settings.zones) {
            val y0 = frame.top + frame.height * 0.22f
            val y1 = frame.top + frame.height * 0.48f
            strips += Rect(frame.left + margin, y0, frame.left + margin + t, y1)
            strips += Rect(frame.right - margin - t, y0, frame.right - margin, y1)
        }
        if (CartdeckLighting.CONTROL_WELLS in settings.zones) {
            controls.take(12).forEach { control ->
                val half = min(control.width * 0.22f, 15f)
                val cx = control.center.x
                strips +=
                    Rect(
                        cx - half,
                        control.bottom + margin,
                        cx + half,
                        control.bottom + margin + t,
                    )
            }
        }
        val blocked = listOf(video) + controls + protectedRects
        return strips.filter { strip ->
            strip.width > 0f && strip.height > 0f &&
                strip.left >= frame.left && strip.right <= frame.right &&
                strip.top >= frame.top && strip.bottom <= frame.bottom &&
                blocked.none { obstacle ->
                    strip.left < obstacle.right && strip.right > obstacle.left &&
                        strip.top < obstacle.bottom && strip.bottom > obstacle.top
                }
        }
    }
}

/** Called only by the existing passive manufacturing shell Canvas. No timers. */
internal fun DrawScope.drawCartdeckLighting(
    lighting: CartdeckLighting,
    accent: Color,
    video: Rect?,
    controls: List<Rect>,
    brand: List<Rect>,
    highContrast: Boolean,
    thickness: Float,
    clearance: Float,
    feedbackStrength: Float = 0f,
) {
    if (video == null || highContrast || lighting.mode == CartdeckLighting.MODE_OFF) return
    val frame = Rect(0f, 0f, size.width, size.height)
    val strips =
        CartdeckLightingGeometry.candidates(
            frame = frame,
            video = video,
            controls = controls,
            thickness = thickness,
            gap = clearance,
            settings = lighting,
            protectedRects = brand,
        )
    val colour = lighting.tint(accent, feedbackStrength)
    strips.forEach { strip ->
        // Light never bleeds into protected video/control areas: don't paint
        // unbounded bloom outside the already validated physical strip.
        drawRect(color = colour, topLeft = strip.topLeft, size = strip.size)
    }
}
