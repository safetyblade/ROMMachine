package com.swordfish.lemuroid.app.gemrating

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class GemFeatureContributionsTest {
    private val releases =
        listOf(
            GemCataloguePackage.Release("r-1", "gba", "Rated", "source", "1", "Strategy"),
            GemCataloguePackage.Release("r-2", "gba", "Candidate", "source", "2", "Strategy"),
            GemCataloguePackage.Release("r-3", "gba", "Other", "source", "3", "Racing"),
        )

    @Test
    fun exactReleaseRowsAreAcceptedWhileUnknownAndStaleNamesAreReported() {
        val raw =
            """
            {
              "schemaVersion": 1,
              "records": [
                {"releaseId":"r-1","title":"Rated","mechanics":["Deckbuilding"]},
                {"releaseId":"r-2","title":"Candidate","mechanics":["Deckbuilding"],"playstyle":"Turn-based"},
                {"releaseId":"missing","title":"Unknown","mechanics":["Deckbuilding"]},
                {"releaseId":"r-3","title":"Wrong title","mechanics":["Lap racing"]}
              ]
            }
            """.trimIndent()
        val report = GemFeatureContributions.load(ByteArrayInputStream(raw.toByteArray()), releases)
        assertEquals(setOf("r-1", "r-2"), report.accepted.keys)
        assertEquals(2, report.rejected.size)
        assertEquals(listOf("Deckbuilding"), report.accepted.getValue("r-2").mechanics)
        assertEquals("Strategy", releases.first().primaryGenre)
    }

    @Test
    fun addedMechanicsFlowIntoExistingOfflineRankingWithoutCreatingRatings() {
        val raw =
            """
            {
              "schemaVersion": 1,
              "records": [
                {"releaseId":"r-1","title":"Rated","mechanics":["Deckbuilding"]},
                {"releaseId":"r-2","title":"Candidate","mechanics":["Deckbuilding"]}
              ]
            }
            """.trimIndent()
        val additions = GemFeatureContributions.parse(raw, releases).accepted
        val noRatings = GemDiscovery.prepare(releases, emptyList(), emptyMap(), emptyList(), additions)
        assertTrue(noRatings.ratings.isEmpty())
        assertEquals(setOf("Deckbuilding"), noRatings.catalogue[1].mechanics)
        val rating =
            GemRatingRepository.Rating(
                cartridgeId = GemRatingRepository.catalogueRatingId("r-1"),
                score = 9,
                revision = 1L,
                updatedAt = 1L,
                displayTitle = "Rated",
                platformId = "gba",
                catalogueReleaseId = "r-1",
            )
        val prepared = GemDiscovery.prepare(releases, listOf(rating), emptyMap(), emptyList(), additions)
        assertEquals(listOf(GemRatingRecommender.Rating("r-1", 9.0)), prepared.ratings)
        val results =
            GemRatingRecommender.recommend(
                prepared.catalogue,
                prepared.ratings,
                candidateIds = setOf("r-2"),
            )
        assertEquals("r-2", results.single().game.releaseId)
        assertTrue(results.single().reason.contains("deckbuilding", ignoreCase = true))
        val cleared = GemDiscovery.prepare(releases, emptyList(), emptyMap(), emptyList(), additions)
        assertFalse(
            GemRatingRecommender.recommend(cleared.catalogue, cleared.ratings).isNotEmpty(),
        )
    }
}
