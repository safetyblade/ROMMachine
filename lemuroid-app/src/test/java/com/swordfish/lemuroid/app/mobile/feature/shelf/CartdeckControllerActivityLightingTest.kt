package com.swordfish.lemuroid.app.mobile.feature.shelf

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CartdeckControllerActivityLightingTest {
    @Test
    fun controllerModeRoundTripsInExistingAppearanceWithoutChangingShelfCells() {
        val baseline = ShelfLayout().reconcile(mapOf("gba-1" to "Example", "gbc-2" to "Second"))
        val chosen =
            CartdeckLighting(
                mode = CartdeckLighting.MODE_CONTROLLER,
                intensity = 54,
                colour = 0xFF77DD99L,
                zones = setOf(CartdeckLighting.CONTROL_WELLS, CartdeckLighting.SIDE_RAILS),
            )
        val saved = baseline.appearance.copy(lighting = chosen).saveNewProfile("Button Lights", now = 99L)
        val restored = decode(encode(baseline.copy(appearance = saved)))
        assertEquals(baseline.sections, restored.sections)
        assertEquals(chosen, restored.appearance.lighting)
        assertEquals(chosen, restored.appearance.profiles.single().lighting)
        val originalId = restored.appearance.profiles.single().id
        assertEquals(chosen, restored.appearance.duplicateProfile(originalId).profiles.last().lighting)
        assertEquals(CartdeckLighting.MODE_OFF, restored.appearance.choosePreset("pink").lighting.mode)
    }

    @Test
    fun legacyFilesStillDefaultOffAndAllThreePriorModesRemainValid() {
        val layout = ShelfLayout()
        val json = JSONObject(encode(layout))
        json.getJSONObject("appearance").remove("lighting")
        assertEquals(CartdeckLighting.MODE_OFF, decode(json.toString()).appearance.lighting.mode)
        for (mode in listOf(
            CartdeckLighting.MODE_OFF,
            CartdeckLighting.MODE_STATIC,
            CartdeckLighting.MODE_FEEDBACK,
            CartdeckLighting.MODE_CONTROLLER,
        )) {
            assertEquals(mode, CartdeckLighting(mode = mode).validate().mode)
        }
    }

    @Test
    fun mappedActivityUsesExistingSafeZonesWithThirtyFivePercentMaxIncrement() {
        val controller = CartdeckLighting(mode = CartdeckLighting.MODE_CONTROLLER, intensity = 60)
        val static = controller.copy(mode = CartdeckLighting.MODE_STATIC)
        val baseline = static.tint(Color.Green).alpha
        val response = controller.tint(Color.Green, feedbackStrength = 1f).alpha
        assertEquals(baseline, controller.tint(Color.Green, feedbackStrength = 0f).alpha, 0.0001f)
        assertEquals(baseline, static.tint(Color.Green, feedbackStrength = 1f).alpha, 0.0001f)
        val alphaStep = 1f / 255f
        assertEquals(0.60f * 0.78f, baseline, alphaStep)
        assertEquals(0.81f * 0.78f, response, alphaStep)
        assertTrue(response <= baseline * 1.35f + 2f * alphaStep)

        val frame = Rect(0f, 0f, 393f, 852f)
        val video = Rect(15f, 92f, 378f, 402f)
        val controls = listOf(Rect(20f, 610f, 165f, 820f), Rect(210f, 610f, 373f, 820f))
        val safe = CartdeckLightingGeometry.candidates(frame, video, controls, 3f, 9f, controller)
        assertEquals(
            CartdeckLightingGeometry.candidates(frame, video, controls, 3f, 9f, static),
            safe,
        )
        assertTrue(safe.isNotEmpty())
        assertFalse(controller.active(CartdeckLighting.SCREEN, highContrast = true))
        val anyOverlap =
            safe.any { strip ->
                (controls + video).any { blocked ->
                    strip.left < blocked.right && strip.right > blocked.left &&
                        strip.top < blocked.bottom && strip.bottom > blocked.top
                }
            }
        assertFalse(anyOverlap)
    }
}
