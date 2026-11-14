package com.swordfish.lemuroid.app.mobile.feature.shelf

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CartdeckFeedbackLightingTest {
    @Test
    fun legacyOffAndStaticRemainUnchangedAndFeedbackSurvivesProfileRoundtrip() {
        val base = ShelfLayout().reconcile(mapOf("gba-1" to "Example"))
        val old = JSONObject(encode(base))
        old.getJSONObject("appearance").remove("lighting")
        assertEquals(CartdeckLighting.MODE_OFF, decode(old.toString()).appearance.lighting.mode)

        val static = CartdeckLighting(mode = CartdeckLighting.MODE_STATIC, intensity = 47)
        val staticRoundTrip =
            decode(encode(base.copy(appearance = base.appearance.copy(lighting = static))))
                .appearance.lighting
        assertEquals(static, staticRoundTrip)

        val feedback =
            CartdeckLighting(
                mode = CartdeckLighting.MODE_FEEDBACK,
                intensity = 65,
                zones = setOf(CartdeckLighting.SCREEN, CartdeckLighting.SIDE_RAILS),
            )
        val named = base.appearance.copy(lighting = feedback).saveNewProfile("My Lights", now = 123L)
        val restored = decode(encode(base.copy(appearance = named)))
        assertEquals(base.sections, restored.sections)
        assertEquals(feedback, restored.appearance.lighting)
        assertEquals(feedback, restored.appearance.profiles.single().lighting)
        val copy = restored.appearance.duplicateProfile(restored.appearance.profiles.single().id)
        assertEquals(feedback, copy.profiles.last().lighting)
        assertEquals(CartdeckLighting.MODE_OFF, copy.choosePreset("green").lighting.mode)
    }

    @Test
    fun eventIntensityCannotStrobeAboveThirtyFivePercentOfBaseline() {
        val light = CartdeckLighting(mode = CartdeckLighting.MODE_FEEDBACK, intensity = 65)
        val baseline = light.tint(Color.Green).alpha
        val full = light.tint(Color.Green, feedbackStrength = 1f).alpha
        val static = light.copy(mode = CartdeckLighting.MODE_STATIC)
        assertEquals(baseline, light.tint(Color.Green, 0f).alpha, 0.0001f)
        assertEquals(baseline, static.tint(Color.Green, 1f).alpha, 0.0001f)
        assertEquals(0.65f * 0.78f, baseline, 0.0001f)
        assertEquals(0.8775f * 0.78f, full, 0.0001f)
        assertTrue(full <= baseline * 1.35f + 0.001f)
        assertEquals(baseline, light.tint(Color.Green, Float.NaN).alpha, 0.0001f)
        assertEquals(0f, light.copy(intensity = 0).tint(Color.Green, 1f).alpha, 0.0001f)
        assertTrue(light.copy(intensity = 100).tint(Color.Green, 1f).alpha <= 0.78f)
    }

    @Test
    fun feedbackUsesOnlyExistingStaticManufacturedGeometry() {
        val frame = Rect(0f, 0f, 393f, 852f)
        val video = Rect(15f, 92f, 378f, 402f)
        val controls = listOf(Rect(20f, 610f, 165f, 820f), Rect(210f, 610f, 373f, 820f))
        val settings = CartdeckLighting(mode = CartdeckLighting.MODE_FEEDBACK)
        val strips = CartdeckLightingGeometry.candidates(frame, video, controls, 3f, 9f, settings)
        assertTrue(strips.isNotEmpty())
        val anyOverlap =
            strips.any { light ->
                (listOf(video) + controls).any { area ->
                    light.left < area.right && light.right > area.left &&
                        light.top < area.bottom && light.bottom > area.top
                }
            }
        assertFalse(anyOverlap)
        assertEquals(
            CartdeckLightingGeometry.candidates(
                frame,
                video,
                controls,
                3f,
                9f,
                settings.copy(mode = CartdeckLighting.MODE_STATIC),
            ),
            strips,
        )
        assertFalse(settings.active(CartdeckLighting.SCREEN, highContrast = true))
        assertTrue(settings.active(CartdeckLighting.SHELF_RAIL))
        val offStrips =
            CartdeckLightingGeometry.candidates(
                frame,
                video,
                controls,
                3f,
                9f,
                settings.copy(mode = CartdeckLighting.MODE_OFF),
            )
        assertTrue(offStrips.isEmpty())
    }

    @Test
    fun unsupportedModesRemainRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ShelfLayout(appearance = ShelfAppearance(lighting = CartdeckLighting(mode = "audio-beats"))).validate()
        }
    }
}
