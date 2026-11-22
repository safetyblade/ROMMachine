package com.swordfish.lemuroid.app.gemrating

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.lib.library.SystemID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
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
            assertTrue("${system.dbname} must remain in DeckFit bounds", estimate.halfSteps in 2..10)
            assertEquals(GemDeckFit.Basis.PLATFORM_ONLY, estimate.basis)
        }
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
