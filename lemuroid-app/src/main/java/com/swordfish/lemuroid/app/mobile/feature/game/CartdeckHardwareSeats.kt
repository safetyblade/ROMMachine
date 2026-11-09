package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartdeckEditionPreset
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartdeckEditionTokens
import kotlin.math.roundToInt

/**
 * Actual supported P1-C2A asset references. The six independent glow families
 * include masks and unlit/ON art; every ON layer is OFF until P1-C3 (#75).
 */
internal data class CartdeckGlowFamily(
    @RawRes val mask: Int,
    @DrawableRes val unlit: Int,
    @DrawableRes val illuminated: Int,
) {
    val enabledByDefault: Boolean = false
}

internal object CartdeckHardwareResource {
    fun seat(edition: CartdeckEditionPreset, key: String): Int? {
        val group =
            when {
                key == "dpad" -> "dpad"
                key == "face-ab" -> "ab"
                key == "face-abxy" -> "abxy"
                key == "start" || key == "select" -> "start-select"
                key.startsWith("shoulder-") -> "shoulder"
                key == "menu" -> "menu"
                key.startsWith("analog") -> "analog"
                key == "ds-mic" -> "ds-microphone"
                key == "ds-layout" -> "ds-screen-layout"
                else -> return null
            }
        return when (edition) {
            CartdeckEditionPreset.ORIGINAL ->
                when (group) {
                    "dpad" -> R.drawable.cartdeck_c2_original_controls_dpad_seat
                    "ab" -> R.drawable.cartdeck_c2_original_controls_ab_seat
                    "abxy" -> R.drawable.cartdeck_c2_original_controls_abxy_seat
                    "start-select" -> R.drawable.cartdeck_c2_original_controls_start_select_seat
                    "shoulder" -> R.drawable.cartdeck_c2_original_controls_shoulder_seat
                    "menu" -> R.drawable.cartdeck_c2_original_controls_menu_seat
                    "analog" -> R.drawable.cartdeck_c2_original_controls_analog_seat
                    "ds-microphone" -> R.drawable.cartdeck_c2_original_controls_ds_microphone_seat
                    "ds-screen-layout" -> R.drawable.cartdeck_c2_original_controls_ds_screen_layout_seat
                    else -> null
                }
            CartdeckEditionPreset.PINK ->
                when (group) {
                    "dpad" -> R.drawable.cartdeck_c2_pink_controls_dpad_seat
                    "ab" -> R.drawable.cartdeck_c2_pink_controls_ab_seat
                    "abxy" -> R.drawable.cartdeck_c2_pink_controls_abxy_seat
                    "start-select" -> R.drawable.cartdeck_c2_pink_controls_start_select_seat
                    "shoulder" -> R.drawable.cartdeck_c2_pink_controls_shoulder_seat
                    "menu" -> R.drawable.cartdeck_c2_pink_controls_menu_seat
                    "analog" -> R.drawable.cartdeck_c2_pink_controls_analog_seat
                    "ds-microphone" -> R.drawable.cartdeck_c2_pink_controls_ds_microphone_seat
                    "ds-screen-layout" -> R.drawable.cartdeck_c2_pink_controls_ds_screen_layout_seat
                    else -> null
                }
            CartdeckEditionPreset.GREEN ->
                when (group) {
                    "dpad" -> R.drawable.cartdeck_c2_green_controls_dpad_seat
                    "ab" -> R.drawable.cartdeck_c2_green_controls_ab_seat
                    "abxy" -> R.drawable.cartdeck_c2_green_controls_abxy_seat
                    "start-select" -> R.drawable.cartdeck_c2_green_controls_start_select_seat
                    "shoulder" -> R.drawable.cartdeck_c2_green_controls_shoulder_seat
                    "menu" -> R.drawable.cartdeck_c2_green_controls_menu_seat
                    "analog" -> R.drawable.cartdeck_c2_green_controls_analog_seat
                    "ds-microphone" -> R.drawable.cartdeck_c2_green_controls_ds_microphone_seat
                    "ds-screen-layout" -> R.drawable.cartdeck_c2_green_controls_ds_screen_layout_seat
                    else -> null
                }
        }
    }

    @DrawableRes
    fun sideRail(edition: CartdeckEditionPreset): Int =
        when (edition) {
            CartdeckEditionPreset.ORIGINAL -> R.drawable.cartdeck_c2_original_chassis_side_rail
            CartdeckEditionPreset.PINK -> R.drawable.cartdeck_c2_pink_chassis_side_rail
            CartdeckEditionPreset.GREEN -> R.drawable.cartdeck_c2_green_chassis_side_rail
        }

    @DrawableRes
    fun speaker(edition: CartdeckEditionPreset): Int =
        when (edition) {
            CartdeckEditionPreset.ORIGINAL -> R.drawable.cartdeck_c2_original_details_speaker_grille
            CartdeckEditionPreset.PINK -> R.drawable.cartdeck_c2_pink_details_speaker_grille
            CartdeckEditionPreset.GREEN -> R.drawable.cartdeck_c2_green_details_speaker_grille
        }

    @DrawableRes
    fun indicator(edition: CartdeckEditionPreset): Int =
        when (edition) {
            CartdeckEditionPreset.ORIGINAL -> R.drawable.cartdeck_c2_original_details_indicator_bars
            CartdeckEditionPreset.PINK -> R.drawable.cartdeck_c2_pink_details_indicator_bars
            CartdeckEditionPreset.GREEN -> R.drawable.cartdeck_c2_green_details_indicator_bars
        }

    /** Named, typed source access without making player-facing lighting settings. */
    fun glow(edition: CartdeckEditionPreset): Map<String, CartdeckGlowFamily> =
        when (edition) {
            CartdeckEditionPreset.ORIGINAL -> mapOf(
                "top-shoulder" to
                    CartdeckGlowFamily(
                        R.raw.cartdeck_c2_glow_top_shoulder_mask,
                        R.drawable.cartdeck_c2_original_glow_top_shoulder_unlit,
                        R.drawable.cartdeck_c2_original_glow_top_shoulder_on,
                    ),
                "bezel-edge" to
                    CartdeckGlowFamily(
                        R.raw.cartdeck_c2_glow_bezel_edge_mask,
                        R.drawable.cartdeck_c2_original_glow_bezel_edge_unlit,
                        R.drawable.cartdeck_c2_original_glow_bezel_edge_on,
                    ),
                "badge" to
                    CartdeckGlowFamily(
                        R.raw.cartdeck_c2_glow_badge_mask,
                        R.drawable.cartdeck_c2_original_glow_badge_unlit,
                        R.drawable.cartdeck_c2_original_glow_badge_on,
                    ),
                "speaker-indicator" to
                    CartdeckGlowFamily(
                        R.raw.cartdeck_c2_glow_speaker_indicator_mask,
                        R.drawable.cartdeck_c2_original_glow_speaker_indicator_unlit,
                        R.drawable.cartdeck_c2_original_glow_speaker_indicator_on,
                    ),
                "analog-ring" to
                    CartdeckGlowFamily(
                        R.raw.cartdeck_c2_glow_analog_ring_mask,
                        R.drawable.cartdeck_c2_original_glow_analog_ring_unlit,
                        R.drawable.cartdeck_c2_original_glow_analog_ring_on,
                    ),
                "deck-seam" to
                    CartdeckGlowFamily(
                        R.raw.cartdeck_c2_glow_deck_seam_mask,
                        R.drawable.cartdeck_c2_original_glow_deck_seam_unlit,
                        R.drawable.cartdeck_c2_original_glow_deck_seam_on,
                    ),
            )
            CartdeckEditionPreset.PINK -> mapOf(
                "top-shoulder" to
                    CartdeckGlowFamily(
                        R.raw.cartdeck_c2_glow_top_shoulder_mask,
                        R.drawable.cartdeck_c2_pink_glow_top_shoulder_unlit,
                        R.drawable.cartdeck_c2_pink_glow_top_shoulder_on,
                    ),
                "bezel-edge" to
                    CartdeckGlowFamily(
                        R.raw.cartdeck_c2_glow_bezel_edge_mask,
                        R.drawable.cartdeck_c2_pink_glow_bezel_edge_unlit,
                        R.drawable.cartdeck_c2_pink_glow_bezel_edge_on,
                    ),
                "badge" to
                    CartdeckGlowFamily(
                        R.raw.cartdeck_c2_glow_badge_mask,
                        R.drawable.cartdeck_c2_pink_glow_badge_unlit,
                        R.drawable.cartdeck_c2_pink_glow_badge_on,
                    ),
                "speaker-indicator" to
                    CartdeckGlowFamily(
                        R.raw.cartdeck_c2_glow_speaker_indicator_mask,
                        R.drawable.cartdeck_c2_pink_glow_speaker_indicator_unlit,
                        R.drawable.cartdeck_c2_pink_glow_speaker_indicator_on,
                    ),
                "analog-ring" to
                    CartdeckGlowFamily(
                        R.raw.cartdeck_c2_glow_analog_ring_mask,
                        R.drawable.cartdeck_c2_pink_glow_analog_ring_unlit,
                        R.drawable.cartdeck_c2_pink_glow_analog_ring_on,
                    ),
                "deck-seam" to
                    CartdeckGlowFamily(
                        R.raw.cartdeck_c2_glow_deck_seam_mask,
                        R.drawable.cartdeck_c2_pink_glow_deck_seam_unlit,
                        R.drawable.cartdeck_c2_pink_glow_deck_seam_on,
                    ),
            )
            CartdeckEditionPreset.GREEN -> mapOf(
                "top-shoulder" to
                    CartdeckGlowFamily(
                        R.raw.cartdeck_c2_glow_top_shoulder_mask,
                        R.drawable.cartdeck_c2_green_glow_top_shoulder_unlit,
                        R.drawable.cartdeck_c2_green_glow_top_shoulder_on,
                    ),
                "bezel-edge" to
                    CartdeckGlowFamily(
                        R.raw.cartdeck_c2_glow_bezel_edge_mask,
                        R.drawable.cartdeck_c2_green_glow_bezel_edge_unlit,
                        R.drawable.cartdeck_c2_green_glow_bezel_edge_on,
                    ),
                "badge" to
                    CartdeckGlowFamily(
                        R.raw.cartdeck_c2_glow_badge_mask,
                        R.drawable.cartdeck_c2_green_glow_badge_unlit,
                        R.drawable.cartdeck_c2_green_glow_badge_on,
                    ),
                "speaker-indicator" to
                    CartdeckGlowFamily(
                        R.raw.cartdeck_c2_glow_speaker_indicator_mask,
                        R.drawable.cartdeck_c2_green_glow_speaker_indicator_unlit,
                        R.drawable.cartdeck_c2_green_glow_speaker_indicator_on,
                    ),
                "analog-ring" to
                    CartdeckGlowFamily(
                        R.raw.cartdeck_c2_glow_analog_ring_mask,
                        R.drawable.cartdeck_c2_green_glow_analog_ring_unlit,
                        R.drawable.cartdeck_c2_green_glow_analog_ring_on,
                    ),
                "deck-seam" to
                    CartdeckGlowFamily(
                        R.raw.cartdeck_c2_glow_deck_seam_mask,
                        R.drawable.cartdeck_c2_green_glow_deck_seam_unlit,
                        R.drawable.cartdeck_c2_green_glow_deck_seam_on,
                    ),
            )
        }
}

internal object CartdeckHardwareSeatPolicy {
    fun at(
        key: String,
        control: Rect,
        others: Map<String, Rect>,
        video: Rect?,
        root: Rect,
        margin: Float,
        clearance: Float,
    ): Rect? {
        if (control.width <= 0f || control.height <= 0f) return null
        if (video != null && CartdeckHardwareGeometry.intersects(control, video)) return null
        val proposed = CartdeckHardwareGeometry.expanded(control, margin)
        val screen = Rect(0f, 0f, root.width, root.height)
        if (
            proposed.left < screen.left || proposed.top < screen.top ||
            proposed.right > screen.right || proposed.bottom > screen.bottom ||
            (video != null && CartdeckHardwareGeometry.intersects(proposed, video))
        ) return null
        if (
            others.any { (id, bounds) ->
                id != key &&
                    CartdeckHardwareGeometry.intersects(
                        proposed,
                        CartdeckHardwareGeometry.expanded(bounds, clearance),
                    )
            }
        ) return null
        return proposed
    }
}

/** Rendered before PadKit, not clickable. Never paints across the game viewport. */
@Composable
internal fun CartdeckHardwareSeats(
    modifier: Modifier = Modifier,
    rootBounds: Rect?,
    videoBounds: Rect?,
    inputBounds: Map<String, Rect>,
    tokens: CartdeckEditionTokens,
    edition: CartdeckEditionPreset,
    isDualScreen: Boolean,
    customColours: Boolean,
) {
    if (rootBounds == null || inputBounds.isEmpty()) return
    val density = LocalDensity.current
    val video = CartdeckHardwareGeometry.inRoot(videoBounds, rootBounds)
    val controls = inputBounds.mapValues { CartdeckHardwareGeometry.inRoot(it.value, rootBounds)!! }
    val margin = with(density) { 3.dp.toPx() }
    val clearance = with(density) { 4.dp.toPx() }
    val tint = if (customColours) ColorFilter.tint(tokens.secondary) else null
    val protectedBrand =
        if (isDualScreen) {
            val halfWidth = with(density) { 49.dp.toPx() }
            val height = with(density) { 54.dp.toPx() }
            Rect(
                rootBounds.width / 2f - halfWidth,
                rootBounds.height - height,
                rootBounds.width / 2f + halfWidth,
                rootBounds.height,
            )
        } else {
            null
        }
    val glowSources = CartdeckHardwareResource.glow(edition)
    // Deliberately unused until P1-C3: the static six-zone lookup proves that
    // the full mask/unlit/on source family is available without enabling light.
    check(glowSources.size == 6 && glowSources.values.none { it.enabledByDefault })

    Box(modifier = modifier.fillMaxSize()) {
        // P1-C2A side-rail masters sized to the real external screen margin,
        // never stretched across the game aperture or touch controls.
        if (video != null) {
            val inset = with(density) { 4.dp.toPx() }
            val railWidth =
                minOf(
                    with(density) { 10.dp.toPx() },
                    video.left - inset,
                    rootBounds.width - video.right - inset,
                )
            val railTop = video.top + with(density) { 7.dp.toPx() }
            val railHeight = video.height - with(density) { 14.dp.toPx() }
            if (railWidth > with(density) { 3.dp.toPx() } && railHeight > 0f) {
                val railRects =
                    listOf(
                        Rect(inset, railTop, inset + railWidth, railTop + railHeight),
                        Rect(
                            rootBounds.width - inset - railWidth,
                            railTop,
                            rootBounds.width - inset,
                            railTop + railHeight,
                        ),
                    )
                railRects.forEach { rectangle ->
                    if (
                        controls.values.none {
                            CartdeckHardwareGeometry.intersects(
                                rectangle,
                                CartdeckHardwareGeometry.expanded(it, clearance),
                            )
                        }
                    ) {
                        Image(
                            painter = painterResource(CartdeckHardwareResource.sideRail(edition)),
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .offset { IntOffset(rectangle.left.roundToInt(), rectangle.top.roundToInt()) }
                                    .width(with(density) { rectangle.width.toDp() })
                                    .height(with(density) { rectangle.height.toDp() }),
                            contentScale = ContentScale.FillBounds,
                            colorFilter = tint,
                        )
                    }
                }
            }
        }
        if (isDualScreen) {
            val utility = listOfNotNull(controls["ds-mic"], controls["ds-layout"], controls["menu"])
            if (utility.size >= 2) {
                val left = utility.minOf { it.left } - margin
                val top = utility.minOf { it.top } - margin
                val right = utility.maxOf { it.right } + margin
                val bottom = utility.maxOf { it.bottom } + margin
                val band = Rect(left, top, right, bottom)
                val start = controls["start"]
                val select = controls["select"]
                val badgeWidth = with(density) { 42.dp.toPx() }
                val badgeHeight = with(density) { 51.dp.toPx() }
                val compactBrand =
                    Rect(
                        rootBounds.width / 2f - badgeWidth,
                        rootBounds.height - badgeHeight,
                        rootBounds.width / 2f + badgeWidth,
                        rootBounds.height,
                    )
                val safe =
                    band.left >= 0 && band.top >= 0 &&
                        band.right <= rootBounds.width && band.bottom <= rootBounds.height &&
                        (video == null || !CartdeckHardwareGeometry.intersects(band, video)) &&
                        listOfNotNull(start, select, compactBrand).none {
                            CartdeckHardwareGeometry.intersects(band, it)
                        }
                if (safe) {
                    Canvas(
                        Modifier
                            .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                            .width(with(density) { band.width.toDp() })
                            .height(with(density) { band.height.toDp() }),
                    ) {
                        drawRoundRect(
                            color = tokens.secondary,
                            cornerRadius = CornerRadius(12.dp.toPx()),
                        )
                        drawRoundRect(
                            color = tokens.bezel,
                            cornerRadius = CornerRadius(12.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                }
            }
        }

        // Speakers and indicators only occupy confirmed unused residual
        // chassis. They are never added to the game's pointer hierarchy.
        val speakerWidth = with(density) { 70.dp.toPx() }
        val speakerHeight = with(density) { 17.dp.toPx() }
        val indicatorWidth = with(density) { 62.dp.toPx() }
        val gapBelowVideo = with(density) { 18.dp.toPx() }
        val firstControl = controls.values.minOfOrNull { it.top } ?: rootBounds.height
        val spaceAboveControls = with(density) { 37.dp.toPx() }
        val detailY = maxOf((video?.bottom ?: 0f) + gapBelowVideo, firstControl - spaceAboveControls)
        val details =
            listOf(
                Rect(
                    rootBounds.width * 0.18f - speakerWidth / 2f,
                    detailY,
                    rootBounds.width * 0.18f + speakerWidth / 2f,
                    detailY + speakerHeight,
                ) to CartdeckHardwareResource.speaker(edition),
                Rect(
                    rootBounds.width * 0.82f - speakerWidth / 2f,
                    detailY,
                    rootBounds.width * 0.82f + speakerWidth / 2f,
                    detailY + speakerHeight,
                ) to CartdeckHardwareResource.speaker(edition),
                Rect(
                    rootBounds.width / 2f - indicatorWidth / 2f,
                    detailY,
                    rootBounds.width / 2f + indicatorWidth / 2f,
                    detailY + speakerHeight,
                ) to CartdeckHardwareResource.indicator(edition),
            )
        details.forEach { (rect, resource) ->
            val clear =
                rect.left >= 4f && rect.right <= rootBounds.width - 4f &&
                    rect.bottom < rootBounds.height - 4f &&
                    (video == null || !CartdeckHardwareGeometry.intersects(rect, video)) &&
                    controls.values.none {
                        CartdeckHardwareGeometry.intersects(
                            rect,
                            CartdeckHardwareGeometry.expanded(it, clearance),
                        )
                    } &&
                    (protectedBrand == null || !CartdeckHardwareGeometry.intersects(rect, protectedBrand))
            if (clear) {
                Image(
                    painter = painterResource(resource),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
                            .width(with(density) { rect.width.toDp() })
                            .height(with(density) { rect.height.toDp() }),
                    contentScale = ContentScale.FillBounds,
                    colorFilter = tint,
                )
            }
        }

        controls.toSortedMap().forEach { (key, control) ->
            val drawable = CartdeckHardwareResource.seat(edition, key) ?: return@forEach
            val safe =
                CartdeckHardwareSeatPolicy.at(
                    key = key,
                    control = control,
                    others = controls,
                    video = video,
                    root = Rect(0f, 0f, rootBounds.width, rootBounds.height),
                    margin = margin,
                    clearance = clearance,
                ) ?: return@forEach
            if (protectedBrand != null && CartdeckHardwareGeometry.intersects(safe, protectedBrand)) {
                return@forEach
            }
            val w = with(density) { safe.width.toDp() }
            val h = with(density) { safe.height.toDp() }
            val base =
                Modifier
                    .offset { IntOffset(safe.left.roundToInt(), safe.top.roundToInt()) }
                    .width(w)
                    .height(h)
            if (key == "start" || key == "select") {
                // The approved SVG is a *pair* of pill seats. Crop its first
                // half around each existing button; never move either button.
                Box(modifier = base.clip(RoundedCornerShape(100))) {
                    Image(
                        painter = painterResource(drawable),
                        contentDescription = null,
                        modifier = Modifier.width(w * 2).height(h).align(Alignment.CenterStart),
                        contentScale = ContentScale.FillBounds,
                        colorFilter = tint,
                    )
                }
            } else {
                Image(
                    painter = painterResource(drawable),
                    contentDescription = null,
                    modifier = base,
                    contentScale = ContentScale.FillBounds,
                    colorFilter = tint,
                )
            }
        }
    }
}

