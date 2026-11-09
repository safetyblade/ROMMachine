package com.swordfish.touchinput.radial

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Visual-only colour contract. The app injects its saved Original/Pink/Green
 * appearance; the touch module has no dependency on app resources or storage.
 */
data class CartdeckPadPalette(
    val chassis: Color = Color(0xFF303B49),
    val trim: Color = Color(0xFF626F7A),
    val bezel: Color = Color(0xFF0D1721),
    val button: Color = Color(0xFF111923),
    val accent: Color = Color(0xFF4ED8C4),
    val onButton: Color = Color(0xFFEDF6F2),
    val focus: Color = Color(0xFF4ED8C4),
    val highContrast: Boolean = false,
)

class LemuroidPadTheme(val palette: CartdeckPadPalette = CartdeckPadPalette()) {
    val foregroundPadding: Dp = 4.dp
    val padding: Dp = 4.dp

    val level3Shadow = palette.bezel.copy(alpha = 0.45f)
    val level3ShadowWidth = 2.dp
    val level2Shadow = palette.bezel.copy(alpha = 0.30f)
    val level2ShadowWidth = 2.dp

    val level1Fill = palette.bezel.copy(alpha = 0.42f)
    val level1Shadow = palette.bezel.copy(alpha = 0.42f)
    val level1ShadowWidth = 2.dp

    val level0CornerRadius = 0.dp
    val level0Fill = palette.chassis.copy(alpha = 0.18f)
    val level0Shadow = palette.bezel.copy(alpha = 0.18f)
    val level0ShadowWidth = 1.dp

    fun compositeFill(pressed: Boolean): Color =
        if (pressed) palette.accent.copy(alpha = 0.24f) else palette.bezel.copy(alpha = 0.18f)

    fun foregroundFill(pressed: Boolean): Color =
        if (pressed) lerp(palette.button, palette.accent, 0.28f) else palette.button

    fun icons(pressed: Boolean): Color =
        if (palette.highContrast) {
            palette.onButton
        } else if (pressed) {
            palette.onButton
        } else {
            palette.onButton.copy(alpha = 0.92f)
        }
}

val LocalLemuroidPadTheme =
    compositionLocalOf<LemuroidPadTheme> {
        error("LemuroidPadTheme is missing")
    }
