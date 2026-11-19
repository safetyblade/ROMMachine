package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.ui.graphics.Color
import com.swordfish.lemuroid.app.mobile.feature.shelf.CartdeckLighting
import com.swordfish.lemuroid.app.mobile.feature.shelf.CartdeckPersonalDecal
import com.swordfish.lemuroid.app.mobile.feature.shelf.CartdeckSurfaceDesign
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfAppearance
import com.swordfish.lemuroid.app.mobile.feature.shelf.colorContrast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CartdeckAppearanceResolverTest {
    private val base =
        CartdeckEditionTokens(
            chassis = Color.DarkGray,
            secondary = Color.Gray,
            bezel = Color.Black,
            glass = Color.Black,
            button = Color.DarkGray,
            accent = Color.Cyan,
            onChassis = Color.White,
            onButton = Color.White,
            focus = Color.Cyan,
            focusOnChassis = Color.White,
            focusOnSecondary = Color.Black,
        )

    @Test
    fun editionsAndDefaultsAreStable() {
        assertEquals(CartdeckEditionPreset.ORIGINAL, cartdeckEditionPreset("original"))
        assertEquals(CartdeckEditionPreset.PINK, cartdeckEditionPreset("Pink"))
        assertEquals(CartdeckEditionPreset.GREEN, cartdeckEditionPreset("green"))
        assertEquals(CartdeckEditionPreset.ORIGINAL, cartdeckEditionPreset("unknown"))
        val resolved = resolveCartdeckAppearance(ShelfAppearance(), base)
        assertEquals(base, resolved.tokens)
        assertFalse(resolved.customColours)
    }

    @Test
    fun customRolesAndDecorationArePreservedWithoutChangingTheSource() {
        val decal = CartdeckPersonalDecal("78bb531a-8ef3-4390-9713-3e6dfb7852e1")
        val lighting = CartdeckLighting(mode = CartdeckLighting.MODE_STATIC)
        val source =
            ShelfAppearance(
                edition = "pink",
                custom = mapOf("chassis" to 0xFF442266L, "accent" to 0xFFD9B477L),
                patternId = "example-pattern",
                stampId = "example-stamp",
                decal = decal,
                lighting = lighting,
            )
        val resolved = resolveCartdeckAppearance(source, base)
        assertEquals(Color(0xFF442266), resolved.tokens.chassis)
        assertEquals(Color(0xFFD9B477), resolved.controllerPalette.accent)
        assertEquals(base.glass, resolved.tokens.glass)
        assertEquals(source.patternId, resolved.patternId)
        assertEquals(source.stampId, resolved.stampId)
        assertEquals(decal, resolved.decal)
        assertEquals(lighting, resolved.lighting)
        assertEquals(source, resolved.source)
        assertTrue(resolved.customColours)
    }

    @Test
    fun accessibilitySuppressesDecorationAndUsesReadableControls() {
        val source =
            ShelfAppearance(
                custom = mapOf("onButton" to 0xFF000000L),
                patternId = "example-pattern",
                stampId = "example-stamp",
                decal = CartdeckPersonalDecal("78bb531a-8ef3-4390-9713-3e6dfb7852e1"),
                highContrast = true,
                reducedMotion = true,
            )
        val resolved = resolveCartdeckAppearance(source, base)
        assertEquals(CartdeckSurfaceDesign.PATTERN_NONE, resolved.patternId)
        assertEquals(CartdeckSurfaceDesign.STAMP_NONE, resolved.stampId)
        assertNull(resolved.decal)
        assertTrue(resolved.controllerPalette.highContrast)
        assertTrue(resolved.source.reducedMotion)
        assertTrue(colorContrast(resolved.controllerPalette.onButton, base.bezel) >= 4.5f)
        assertTrue(colorContrast(resolved.controllerPalette.onButton, base.button) >= 4.5f)
    }
}
