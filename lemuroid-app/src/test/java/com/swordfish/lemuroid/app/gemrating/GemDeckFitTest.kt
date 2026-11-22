package com.swordfish.lemuroid.app.gemrating

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.lib.library.SystemID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class GemDeckFitTest {
    @Test
    fun `researched touch ratings take precedence without a controller bonus`() {
        val pokemon = GemDeckFit.estimate("VDG:gb:94", "gb", "Strategy")
        assertEquals(10, pokemon.halfSteps)
        assertEquals(GemDeckFit.Basis.REVIEWED_TOUCH, pokemon.basis)
        assertEquals("DeckFit 5/5 · Provisional", pokemon.displayText)

        val ikaruga = GemDeckFit.estimate("VDG:gc:382", "gc", "Shooter")
        assertEquals(6, ikaruga.halfSteps)
        assertEquals(GemDeckFit.Basis.REVIEWED_TOUCH, ikaruga.basis)

        val marioKart = GemDeckFit.estimate("VDG:n64:153", "n64", "Racing")
        assertEquals(6, marioKart.halfSteps)
        assertEquals(GemDeckFit.Basis.REVIEWED_TOUCH, marioKart.basis)
    }

    @Test
    fun `special-input holds cannot be presented as ordinary genre estimates`() {
        val tilt = GemDeckFit.estimate("VDG:gb:40", "gb", "Puzzle")
        assertEquals(GemDeckFit.Basis.INPUT_REVIEW, tilt.basis)
        assertNull(tilt.halfSteps)
        assertTrue(tilt.evidenceText.contains("Special input"))
    }

    @Test
    fun `system and genre fallbacks are conservative deterministic and bounded`() {
        assertEquals(
            GemDeckFit.Basis.PLATFORM_GENRE,
            GemDeckFit.estimate("UNKNOWN-1", "gba", "Puzzle").basis,
        )
        assertEquals(
            GemDeckFit.Basis.PLATFORM_ONLY,
            GemDeckFit.estimate("UNKNOWN-2", "gba", "Action").basis,
        )
        assertNull(GemDeckFit.estimate("UNKNOWN-3", "future-system", "Strategy").halfSteps)
        for (system in SystemID.values()) {
            val estimate = GemDeckFit.estimate("TEST:${system.dbname}", system.dbname, null)
            assertTrue("${system.dbname} must remain in DeckFit bounds", requireNotNull(estimate.halfSteps) in 2..10)
            assertEquals(GemDeckFit.Basis.PLATFORM_ONLY, estimate.basis)
        }
    }

    @Test
    fun `QuickScore respects console baselines and evidence-backed half-point modifiers`() {
        assertEquals(9, GemDeckFit.estimate("TEST:gbc", "gbc", null).halfSteps)
        assertEquals(8, GemDeckFit.estimate("TEST:3ds", "3ds", null).halfSteps)
        assertEquals(6, GemDeckFit.estimate("TEST:ps2", "ps2", null).halfSteps)

        val gbcComplex =
            GemDeckFit.estimate(
                "TEST:gbc-complex",
                "gbc",
                "Action",
                traits = setOf(GemDeckFit.Trait.COMPLEX_CHORDS),
            )
        assertEquals(8, gbcComplex.halfSteps)

        val ps2TwoDimensional =
            GemDeckFit.estimate(
                "TEST:ps2-2d",
                "ps2",
                "Action",
                traits = setOf(GemDeckFit.Trait.SIMPLE_2D),
            )
        assertEquals(7, ps2TwoDimensional.halfSteps)

        val ps2Tactics =
            GemDeckFit.estimate(
                "VDG:ps2:1270",
                "ps2",
                "Role-Playing",
                "Disgaea: Hour of Darkness",
            )
        assertEquals(8, ps2Tactics.halfSteps)
        assertEquals(GemDeckFit.Basis.GAME_TRAITS, ps2Tactics.basis)

        val mappedTilt =
            GemDeckFit.estimate(
                "TEST:3ds-tilt",
                "3ds",
                "Action",
                traits = setOf(GemDeckFit.Trait.MOTION_FALLBACK),
            )
        assertEquals(6, mappedTilt.halfSteps)

        val unsupportedMotion =
            GemDeckFit.estimate(
                "VDG:3ds:15",
                "3ds",
                "Action",
                "Beyblade Evolution",
            )
        assertNull(unsupportedMotion.halfSteps)
        assertEquals(GemDeckFit.Basis.INPUT_REVIEW, unsupportedMotion.basis)

        val onePointCap =
            GemDeckFit.estimate(
                "TEST:gbc-difficult",
                "gbc",
                "Action",
                traits =
                    setOf(
                        GemDeckFit.Trait.COMPLEX_CHORDS,
                        GemDeckFit.Trait.PRECISION_ACTION,
                        GemDeckFit.Trait.SMALL_TEXT,
                    ),
            )
        assertEquals(7, onePointCap.halfSteps)
    }

    @Test
    fun `exact source-qualified IDs retain curated scores and special-input holds`() {
        val reviewed =
            mapOf(
                "VDG:xb:row-522" to 4,
                "VDG:xb:row-85" to 5,
                "VDG:xb:row-519" to 4,
                "VDG:xb:row-452" to 6,
                "VDG:xb:row-79" to 3,
                "VDG:xb:row-455" to 7,
                "VDG:xb:row-572" to 4,
                "VDG:xb:row-336" to 5,
                "VDG:xb:row-210" to 5,
                "VDG:wii:row-784" to 5,
                "VDG:wii:row-864" to 6,
            )
        val held =
            setOf(
                "VDG:wii:row-1252",
                "VDG:wii:row-616",
                "VDG:wii:row-736",
                "VDG:wii:row-1253",
            )
        val catalogue =
            GemProductionCatalogue.load(ApplicationProvider.getApplicationContext<Context>())
                .manifest.releases.associateBy { it.releaseId }
        for ((id, expectedScore) in reviewed) {
            val release = requireNotNull(catalogue[id]) { "Missing reviewed release: $id" }
            val estimate = GemDeckFit.estimate(release)
            assertEquals(id, expectedScore, estimate.halfSteps)
            assertEquals(id, GemDeckFit.Basis.REVIEWED_TOUCH, estimate.basis)
        }
        for (id in held) {
            val release = requireNotNull(catalogue[id]) { "Missing held release: $id" }
            val estimate = GemDeckFit.estimate(release)
            assertNull(id, estimate.halfSteps)
            assertEquals(id, GemDeckFit.Basis.INPUT_REVIEW, estimate.basis)
        }
        assertEquals(
            GemDeckFit.Basis.PLATFORM_ONLY,
            GemDeckFit.estimate("VDG:xb:522", "xbox", null).basis,
        )
    }

    @Test
    fun `every currently bundled release receives a provisional DeckFit projection`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val catalogue = GemProductionCatalogue.load(context).manifest.releases
        assertEquals(GemProductionCatalogue.RELEASE_COUNT, catalogue.size)
        for (release in catalogue) {
            val estimate = GemDeckFit.estimate(release)
            if (estimate.basis == GemDeckFit.Basis.INPUT_REVIEW) {
                assertNull(estimate.halfSteps)
                assertTrue(estimate.displayText.contains("Input review"))
            } else {
                val score = requireNotNull(estimate.halfSteps)
                assertTrue("${release.releaseId} missing valid DeckFit", score in 2..10)
                assertTrue(estimate.displayText.contains("Provisional"))
            }
            assertTrue(estimate.evidenceText.contains("not device-tested"))
        }
    }
}
