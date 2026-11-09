package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import android.content.Context
import androidx.annotation.ColorRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.swordfish.lemuroid.R

/**
 * CARTDECK Original/Pink/Green colour-only contract.
 *
 * Resource colours are deterministically generated from canonical editable
 * design/cartdeck-v03/editions/{original,pink,green}.json by ASSET-H1.
 *
 * Consumed by the P1-A2 library hardware painters; gameplay uses the same saved edition.
 * It does not change Android dynamic theme, renderer, game pixel colours,
 * PadKit input, saves, package identity, launcher icons or user cover artwork.
 */
internal enum class CartdeckEditionPreset {
    ORIGINAL,
    PINK,
    GREEN,
}

@Immutable
internal data class CartdeckEditionTokens(
    val chassis: Color,
    val secondary: Color,
    val bezel: Color,
    val glass: Color,
    val button: Color,
    val accent: Color,
    val onChassis: Color,
    val onButton: Color,
    val focus: Color,
    val focusOnChassis: Color,
    val focusOnSecondary: Color,
)

/** Resolve a palette on demand; the caller's Compose state owns reactivity. */
internal fun Context.resolveCartdeckEdition(edition: CartdeckEditionPreset): CartdeckEditionTokens {
    fun nativeColor(
        @ColorRes resource: Int,
    ): Color = Color(ContextCompat.getColor(this, resource))

    return when (edition) {
        CartdeckEditionPreset.ORIGINAL ->
            CartdeckEditionTokens(
                chassis = nativeColor(R.color.cartdeck_original_chassis),
                secondary = nativeColor(R.color.cartdeck_original_secondary),
                bezel = nativeColor(R.color.cartdeck_original_bezel),
                glass = nativeColor(R.color.cartdeck_original_glass),
                button = nativeColor(R.color.cartdeck_original_button),
                accent = nativeColor(R.color.cartdeck_original_accent),
                onChassis = nativeColor(R.color.cartdeck_original_on_chassis),
                onButton = nativeColor(R.color.cartdeck_original_on_button),
                focus = nativeColor(R.color.cartdeck_original_focus),
                focusOnChassis = nativeColor(R.color.cartdeck_original_focus_on_chassis),
                focusOnSecondary = nativeColor(R.color.cartdeck_original_focus_on_secondary),
            )
        CartdeckEditionPreset.PINK ->
            CartdeckEditionTokens(
                chassis = nativeColor(R.color.cartdeck_pink_chassis),
                secondary = nativeColor(R.color.cartdeck_pink_secondary),
                bezel = nativeColor(R.color.cartdeck_pink_bezel),
                glass = nativeColor(R.color.cartdeck_pink_glass),
                button = nativeColor(R.color.cartdeck_pink_button),
                accent = nativeColor(R.color.cartdeck_pink_accent),
                onChassis = nativeColor(R.color.cartdeck_pink_on_chassis),
                onButton = nativeColor(R.color.cartdeck_pink_on_button),
                focus = nativeColor(R.color.cartdeck_pink_focus),
                focusOnChassis = nativeColor(R.color.cartdeck_pink_focus_on_chassis),
                focusOnSecondary = nativeColor(R.color.cartdeck_pink_focus_on_secondary),
            )
        CartdeckEditionPreset.GREEN ->
            CartdeckEditionTokens(
                chassis = nativeColor(R.color.cartdeck_green_chassis),
                secondary = nativeColor(R.color.cartdeck_green_secondary),
                bezel = nativeColor(R.color.cartdeck_green_bezel),
                glass = nativeColor(R.color.cartdeck_green_glass),
                button = nativeColor(R.color.cartdeck_green_button),
                accent = nativeColor(R.color.cartdeck_green_accent),
                onChassis = nativeColor(R.color.cartdeck_green_on_chassis),
                onButton = nativeColor(R.color.cartdeck_green_on_button),
                focus = nativeColor(R.color.cartdeck_green_focus),
                focusOnChassis = nativeColor(R.color.cartdeck_green_focus_on_chassis),
                focusOnSecondary = nativeColor(R.color.cartdeck_green_focus_on_secondary),
            )
    }
}
