package com.swordfish.touchinput.radial

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Pure visual factory finish injected by the Android app. The touchinput
 * module never depends on the shelf/appearance module or its Gradle graph.
 * These colours do not affect PadKit IDs, hit regions or event dispatch.
 */
class LemuroidPadTheme(
    val button: Color = Color(0xFF111923),
    val accent: Color = Color(0xFF4ED8C4),
    val glyph: Color = Color(0xFFEDF6F2),
    val trim: Color = Color(0xFF626F7A),
) {
    val foregroundPadding: Dp = 8.dp
    val padding: Dp = 4.dp

    val level3Shadow = Color.Black.copy(alpha = 0.32f)
    val level3ShadowWidth = 4.dp
    val level2Shadow = Color.Black.copy(alpha = 0.10f)
    val level2ShadowWidth = 1.dp
    val level1Fill = Color.Transparent
    val level1Shadow = Color.Transparent
    val level1ShadowWidth = 0.dp
    val level0CornerRadius = 0.dp
    val level0Fill = Color.Transparent
    val level0Shadow = Color.Transparent
    val level0ShadowWidth = 0.dp

    fun compositeFill(pressed: Boolean): Color = if (pressed) accent.copy(alpha = 0.16f) else Color.Transparent

    fun foregroundFill(pressed: Boolean): Color = if (pressed) trim else button

    fun icons(pressed: Boolean): Color = glyph
}

val LocalLemuroidPadTheme =
    compositionLocalOf<LemuroidPadTheme> {
        error("LemuroidPadTheme is missing")
    }
