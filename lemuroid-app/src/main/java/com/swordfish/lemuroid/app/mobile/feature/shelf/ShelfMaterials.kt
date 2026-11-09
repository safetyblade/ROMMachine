package com.swordfish.lemuroid.app.mobile.feature.shelf

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/** Generated canonical values: design/cartdeck-v06/bookshelf-palette.json (PR #44). */
data class ShelfMaterials(
    val roomBackdrop: Color,
    val shelfBackboard: Color,
    val shelfDeck: Color,
    val shelfLip: Color,
    val shelfGroove: Color,
    val edgeHighlight: Color,
    val shelfShadow: Color,
    val sectionDivider: Color,
    val vacantCell: Color,
    val sectionText: Color,
    val focusOutline: Color,
    val interactionAccent: Color,
)

val LocalShelfAppearance = staticCompositionLocalOf { ShelfAppearance() }
val LocalShelfMaterials = staticCompositionLocalOf { canonicalShelfMaterials("original") }

fun canonicalShelfMaterials(pink: Boolean): ShelfMaterials = canonicalShelfMaterials(if (pink) "pink" else "original")

/** Source: design/cartdeck-v06/bookshelf-palette.json; all editions share geometry. */
fun canonicalShelfMaterials(edition: String): ShelfMaterials =
    when (edition.lowercase()) {
        "pink" ->
            ShelfMaterials(
                roomBackdrop = Color(0xFF221722),
                shelfBackboard = Color(0xFF382536),
                shelfDeck = Color(0xFF69445D),
                shelfLip = Color(0xFF9E6B88),
                shelfGroove = Color(0xFF261827),
                edgeHighlight = Color(0xFFD99CBA),
                shelfShadow = Color(0xFF140C15),
                sectionDivider = Color(0xFF765069),
                vacantCell = Color(0xFF4B3045),
                sectionText = Color(0xFFFFF3F7),
                focusOutline = Color(0xFFFFE5F1),
                interactionAccent = Color(0xFFFFD5E7),
            )
        "green" ->
            ShelfMaterials(
                roomBackdrop = Color(0xFF0D1914),
                shelfBackboard = Color(0xFF163127),
                shelfDeck = Color(0xFF315443),
                shelfLip = Color(0xFF658978),
                shelfGroove = Color(0xFF0B241B),
                edgeHighlight = Color(0xFF9DC5A6),
                shelfShadow = Color(0xFF06110D),
                sectionDivider = Color(0xFF315E4C),
                vacantCell = Color(0xFF254336),
                sectionText = Color(0xFFEBF8E8),
                focusOutline = Color(0xFFD4F47D),
                interactionAccent = Color(0xFFD4F47D),
            )
        else ->
            ShelfMaterials(
                roomBackdrop = Color(0xFF0B131B),
                shelfBackboard = Color(0xFF14212D),
                shelfDeck = Color(0xFF293847),
                shelfLip = Color(0xFF485B6C),
                shelfGroove = Color(0xFF0C1923),
                edgeHighlight = Color(0xFF5D7885),
                shelfShadow = Color(0xFF060C12),
                sectionDivider = Color(0xFF334653),
                vacantCell = Color(0xFF1D2C39),
                sectionText = Color(0xFFEAF4F2),
                focusOutline = Color(0xFF4ED8C4),
                interactionAccent = Color(0xFF4ED8C4),
            )
    }

fun colorContrast(
    first: Color,
    second: Color,
): Float {
    val a = first.luminance() + 0.05f
    val b = second.luminance() + 0.05f
    return maxOf(a, b) / minOf(a, b)
}

fun readableColor(
    preferred: Color,
    backgrounds: List<Color>,
    minimum: Float,
): Color {
    val candidates = listOf(preferred, Color.White, Color.Black) + (0..255 step 8).map { Color(it, it, it) }
    return candidates.firstOrNull { color -> backgrounds.all { colorContrast(color, it) >= minimum } }
        ?: candidates.maxBy { color -> backgrounds.minOf { colorContrast(color, it) } }
}

fun shelfMaterials(appearance: ShelfAppearance): ShelfMaterials {
    val base = canonicalShelfMaterials(appearance.edition)

    fun override(
        role: String,
        fallback: Color,
    ) = appearance.custom[role]?.let { Color(it.toInt()) } ?: fallback
    val hardwareChanged = appearance.custom.containsKey("chassis") || appearance.custom.containsKey("accent")
    val chassis = override("chassis", base.shelfDeck)
    val accent = override("accent", base.interactionAccent)
    val board =
        override("shelfBackboard", if (hardwareChanged) lerp(chassis, Color.Black, 0.7f) else base.shelfBackboard)
    val vacant = override("vacantCell", if (hardwareChanged) lerp(chassis, Color.Black, 0.5f) else base.vacantCell)
    val text = readableColor(override("sectionText", base.sectionText), listOf(board), 4.5f)
    val focus = readableColor(override("focusOutline", accent), listOf(board, vacant), 3f)
    return base.copy(
        roomBackdrop =
            override(
                "roomBackdrop",
                if (hardwareChanged) lerp(chassis, Color.Black, 0.85f) else base.roomBackdrop,
            ),
        shelfBackboard = board,
        shelfDeck = override("shelfDeck", if (hardwareChanged) chassis else base.shelfDeck),
        shelfLip = override("shelfLip", if (hardwareChanged) lerp(chassis, Color.White, 0.2f) else base.shelfLip),
        shelfGroove = override("shelfGroove", base.shelfGroove),
        edgeHighlight = override("edgeHighlight", base.edgeHighlight),
        shelfShadow = override("shelfShadow", base.shelfShadow),
        sectionDivider = override("sectionDivider", base.sectionDivider),
        vacantCell = vacant,
        sectionText = text,
        focusOutline = focus,
        interactionAccent = readableColor(override("interactionAccent", accent), listOf(board), 3f),
    ).let { colors ->
        if (appearance.highContrast) {
            colors.copy(
                roomBackdrop = Color.Black,
                shelfBackboard = Color.Black,
                vacantCell = Color.Black,
                sectionText = Color.White,
                focusOutline = Color.White,
                interactionAccent = Color.White,
            )
        } else {
            colors
        }
    }
}
