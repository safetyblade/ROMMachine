package com.swordfish.lemuroid.app.gemrating

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class GemGenreTaxonomyTest {
    @Test
    fun sourceSubgenresAreNormalisedWithoutFabricatingControlEvidence() {
        val platform = GemGenreTaxonomy.classify("Platformer", "Example")
        assertTrue(platform.gameEligible)
        assertEquals("Platform", platform.primary)
        assertEquals(setOf("Platforming"), platform.mechanics)

        val fps = GemGenreTaxonomy.classify("First-person shooter", "Example")
        assertEquals("Shooter", fps.primary)
        assertEquals("First-person", fps.playstyle)
        assertFalse(GemDeckFit.Trait.DUAL_ANALOG_AIM in GemDeckFit.evidenceTraits(fps.playstyle, fps.mechanics))

        val turns = GemGenreTaxonomy.classify("Turn-based strategy", "Example")
        assertEquals("Strategy", turns.primary)
        assertEquals("Turn-based", turns.playstyle)
        assertTrue(GemDeckFit.Trait.TURN_BASED in GemDeckFit.evidenceTraits(turns.playstyle, turns.mechanics))

        val collection = GemGenreTaxonomy.classify("Compilation", "Castlevania Double Pack")
        assertTrue(collection.gameEligible)
        assertEquals("Collection", collection.primary)
    }

    @Test
    fun sourceGenreAndIdentityAreAuthoritativeNotTitleKeywordGuesses() {
        assertTrue(GemGenreTaxonomy.classify("Action", "Prototype 2").gameEligible)
        assertTrue(GemGenreTaxonomy.classify("Racing", "Urban Trial Freestyle").gameEligible)
        assertFalse(GemGenreTaxonomy.classify("Application", "Firmware Tool").gameEligible)
        assertFalse(GemGenreTaxonomy.classify(null, "Unknown Title").gameEligible)
        assertTrue(GemGenreTaxonomy.classify("Action", "BIOS Utility").reviewFlag)
    }

    @Test
    fun allBundledFeatureRowsMatchExactReleaseIdsAndTitles() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val releases = GemProductionCatalogue.load(context).manifest.releases
        val result =
            context.assets.open("gem/features/v1.json").use {
                GemFeatureContributions.load(it, releases)
            }
        assertEquals(1235, result.accepted.size)
        assertTrue("Every bundled feature must match exact source ID and title", result.rejected.isEmpty())
        assertTrue(result.accepted.values.count { it.sourceId == "titlescreen" } >= 1200)
        val indexed = releases.associateBy { it.releaseId }
        for ((id, feature) in result.accepted) {
            assertEquals(id, indexed[id]?.releaseId)
            assertEquals(feature.title, indexed[id]?.title)
            assertNotNull(feature.evidence)
        }
    }

    @Test
    fun enrichedFeaturesAndCanonicalGenreReachExistingRecommender() {
        val releases =
            listOf(
                GemCataloguePackage.Release("r1", "gba", "Liked", "source", "1", "Platformer"),
                GemCataloguePackage.Release("r2", "gba", "Candidate", "source", "2", "Platformer"),
                GemCataloguePackage.Release("r3", "gba", "Prototype 2", "source", "3", "Action"),
            )
        val features =
            mapOf(
                "r1" to GemFeatureContributions.Record("r1", "Liked", mechanics = listOf("Platforming")),
                "r2" to GemFeatureContributions.Record("r2", "Candidate", mechanics = listOf("Platforming")),
            )
        val rated =
            GemRatingRepository.Rating(
                cartridgeId = GemRatingRepository.catalogueRatingId("r1"),
                score = 10,
                revision = 1,
                updatedAt = 1,
                platformId = "gba",
                catalogueReleaseId = "r1",
            )
        val discovery = GemDiscovery.prepare(releases, listOf(rated), emptyMap(), emptyList(), features)
        assertEquals(setOf("r1", "r2", "r3"), discovery.catalogue.map { it.releaseId }.toSet())
        assertTrue(discovery.catalogue.all { it.eligibility == GemRatingRecommender.Eligibility.GAME })
        assertEquals("Platform", discovery.catalogue.first().primaryGenre)
        val result = GemRatingRecommender.recommend(discovery.catalogue, discovery.ratings)
        assertTrue(result.any { it.game.releaseId == "r2" })
    }
}
