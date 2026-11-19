package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.swordfish.lemuroid.app.mobile.feature.shelf.CartdeckSurfaceDesign
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfAppearance
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfStore
import com.swordfish.lemuroid.app.mobile.feature.shelf.readableColor
import com.swordfish.touchinput.radial.CartdeckPadPalette
import kotlinx.coroutines.CancellationException

internal fun cartdeckEditionPreset(id: String): CartdeckEditionPreset =
    when (id.lowercase()) {
        "pink" -> CartdeckEditionPreset.PINK
        "green" -> CartdeckEditionPreset.GREEN
        else -> CartdeckEditionPreset.ORIGINAL
    }

internal fun ShelfAppearance.appearanceColour(
    role: String,
    fallback: Color,
): Color = custom[role]?.let { Color(it.toInt()) } ?: fallback

/** One read-only appearance snapshot; engines provide geometry and input, not cosmetics. */
internal data class ResolvedCartdeckAppearance(
    val source: ShelfAppearance,
    val edition: CartdeckEditionPreset,
    val tokens: CartdeckEditionTokens,
    val controllerPalette: CartdeckPadPalette,
) {
    val patternId get() = if (source.highContrast) CartdeckSurfaceDesign.PATTERN_NONE else source.patternId
    val stampId get() = if (source.highContrast) CartdeckSurfaceDesign.STAMP_NONE else source.stampId
    val decal get() = if (source.highContrast) null else source.decal
    val lighting get() = source.lighting
    val customColours get() = source.custom.isNotEmpty()
}

/** Resolve explicit hardware roles identically for both gameplay paths and the preview. */
internal fun resolveCartdeckAppearance(
    appearance: ShelfAppearance,
    base: CartdeckEditionTokens,
): ResolvedCartdeckAppearance {
    val tokens =
        base.copy(
            chassis = appearance.appearanceColour("chassis", base.chassis),
            secondary = appearance.appearanceColour("secondary", base.secondary),
            bezel = appearance.appearanceColour("bezel", base.bezel),
            glass = appearance.appearanceColour("glass", base.glass),
            button = appearance.appearanceColour("button", base.button),
            accent = appearance.appearanceColour("accent", base.accent),
            onChassis = appearance.appearanceColour("onChassis", base.onChassis),
            onButton = appearance.appearanceColour("onButton", base.onButton),
            focus = appearance.appearanceColour("focus", base.focus),
        )
    return ResolvedCartdeckAppearance(
        source = appearance,
        edition = cartdeckEditionPreset(appearance.edition),
        tokens = tokens,
        controllerPalette =
            CartdeckPadPalette(
                chassis = tokens.chassis,
                trim = tokens.secondary,
                bezel = tokens.bezel,
                button = tokens.button,
                accent = tokens.accent,
                onButton = readableColor(tokens.onButton, listOf(tokens.button, tokens.bezel), 4.5f),
                focus = tokens.focus,
                highContrast = appearance.highContrast,
            ),
    )
}

/** Load once per gameplay composition. ShelfStore owns IO and atomic persistence. */
@Composable
internal fun rememberGameplayAppearance(): ResolvedCartdeckAppearance {
    val context = LocalContext.current
    val appearance =
        produceState(initialValue = ShelfAppearance(), key1 = context) {
            value =
                runCatching { ShelfStore(context).read().appearance }.getOrElse {
                    if (it is CancellationException) throw it
                    ShelfAppearance()
                }
        }.value
    val edition = cartdeckEditionPreset(appearance.edition)
    val base = remember(context, edition) { context.resolveCartdeckEdition(edition) }
    return remember(base, appearance) { resolveCartdeckAppearance(appearance, base) }
}
