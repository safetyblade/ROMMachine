package com.swordfish.lemuroid.app.mobile.feature.shelf

import androidx.compose.ui.geometry.Rect
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CartdeckLightingTest {
    @Test
    fun defaultIsOffAndLegacyAppearanceLoadsWithoutLighting() {
        val base = ShelfLayout().reconcile(mapOf("gba-1" to "My game"))
        assertEquals(CartdeckLighting.MODE_OFF, base.appearance.lighting.mode)
        val json = JSONObject(encode(base))
        val appearance = json.getJSONObject("appearance")
        appearance.remove("lighting")
        val profiles = appearance.getJSONArray("profiles")
        for (i in 0 until profiles.length()) profiles.getJSONObject(i).remove("lighting")
        assertEquals(base, decode(json.toString()))
        assertFalse(base.appearance.lighting.active(CartdeckLighting.SHELF_RAIL))
    }

    @Test
    fun preferencesSurviveNamedProfileCopySwitchAndResetWithoutTouchingShelf() {
        val lighting =
            CartdeckLighting(
                mode = CartdeckLighting.MODE_STATIC,
                intensity = 35,
                colour = 0xFF9BC74AL,
                zones = setOf(CartdeckLighting.SHELF_RAIL, CartdeckLighting.SIDE_RAILS),
            )
        val initial =
            ShelfLayout().reconcile(mapOf("gbc-a" to "Game A", "gba-b" to "Game B"))
                .insertSpace(ShelfLayout.UNSORTED)
        val saved = initial.appearance.copy(lighting = lighting).saveNewProfile("Low Light", now = 99L)
        val after = decode(encode(initial.copy(appearance = saved)))
        assertEquals(initial.sections, after.sections)
        assertEquals(lighting, after.appearance.lighting)
        val firstId = after.appearance.profiles.single().id
        val copy = after.appearance.duplicateProfile(firstId, now = 123L)
        assertEquals(lighting, copy.profiles.last().lighting)
        assertEquals(lighting, copy.selectProfile(firstId).lighting)
        assertEquals(CartdeckLighting.MODE_OFF, copy.choosePreset("pink").lighting.mode)
        assertEquals(lighting, copy.selectProfile(firstId).lighting)
        assertEquals(copy.profiles, copy.choosePreset("green").profiles)
    }

    @Test
    fun offAndHighContrastNeverActivateLightTracks() {
        val off = CartdeckLighting()
        val static = off.copy(mode = CartdeckLighting.MODE_STATIC)
        assertFalse(off.active(CartdeckLighting.SCREEN))
        assertFalse(static.active(CartdeckLighting.SCREEN, highContrast = true))
        assertTrue(static.active(CartdeckLighting.SCREEN))
        assertFalse(static.copy(intensity = 0).active(CartdeckLighting.SHELF_RAIL))
        assertFalse(static.copy(zones = emptySet()).active(CartdeckLighting.SIDE_RAILS))
    }

    @Test
    fun candidateLightsNeverCoverVideoNativeInputsOrProtectedBrandOnAnyFormFactor() {
        val variants =
            listOf(
                Rect(0f, 0f, 393f, 852f) to Rect(15f, 92f, 378f, 402f),
                Rect(0f, 0f, 852f, 393f) to Rect(118f, 10f, 733f, 379f),
                Rect(0f, 0f, 1600f, 1100f) to Rect(130f, 110f, 1470f, 730f),
            )
        val lighting = CartdeckLighting(mode = CartdeckLighting.MODE_STATIC)
        for ((frame, video) in variants) {
            val controls =
                listOf(
                    Rect(30f, frame.height * 0.73f, 145f, frame.height - 15f),
                    Rect(frame.width - 145f, frame.height * 0.73f, frame.width - 30f, frame.height - 15f),
                )
            val brand = Rect(frame.width * 0.34f, 0f, frame.width * 0.66f, 80f)
            val strips =
                CartdeckLightingGeometry.candidates(
                    frame = frame,
                    video = video,
                    controls = controls,
                    thickness = 3f,
                    gap = 9f,
                    settings = lighting,
                    protectedRects = listOf(brand),
                )
            assertTrue("Expected a manufactured lighting track", strips.isNotEmpty())
            assertTrue(
                strips.all { r ->
                    r.left >= frame.left && r.top >= frame.top &&
                        r.right <= frame.right && r.bottom <= frame.bottom
                },
            )
            assertTrue(
                strips.none { strip ->
                    (listOf(video, brand) + controls).any { blocked ->
                        strip.left < blocked.right && strip.right > blocked.left &&
                            strip.top < blocked.bottom && strip.bottom > blocked.top
                    }
                },
            )
            assertTrue(
                CartdeckLightingGeometry.candidates(
                    frame,
                    video,
                    controls,
                    3f,
                    9f,
                    lighting.copy(mode = CartdeckLighting.MODE_OFF),
                ).isEmpty(),
            )
        }
    }

    @Test
    fun invalidModesZonesAndIntensityCannotEnterSavedAppearance() {
        assertThrows(IllegalArgumentException::class.java) {
            ShelfLayout(
                appearance =
                    ShelfAppearance(lighting = CartdeckLighting(mode = "fake-reactive")),
            ).validate()
        }
        assertThrows(IllegalArgumentException::class.java) {
            CartdeckLighting(mode = CartdeckLighting.MODE_STATIC, intensity = 101).validate()
        }
        assertThrows(IllegalArgumentException::class.java) {
            CartdeckLighting(zones = setOf("screen-pixels")).validate()
        }
        val malformed = JSONObject(encode(ShelfLayout()))
        malformed.getJSONObject("appearance").getJSONObject("lighting").put("mode", "fake-reactive")
        assertThrows(IllegalArgumentException::class.java) { decode(malformed.toString()) }
    }
}
