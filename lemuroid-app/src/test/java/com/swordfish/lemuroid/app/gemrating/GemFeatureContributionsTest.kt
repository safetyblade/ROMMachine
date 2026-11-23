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
    fun previewShowsAdditionsAndChangesAndRejectsAmbiguousDuplicates() {
        val raw =
            """
            {
              "schemaVersion": 1,
              "records": [
                {"releaseId":"r-1","title":"Rated","mechanics":["Deckbuilding"]},
                {"releaseId":"r-2","title":"Candidate","playstyle":"Turn-based"},
                {"releaseId":"r-3","title":"Other","mechanics":["Lap racing"]},
                {"releaseId":"r-3","title":"Other","mechanics":["Drifting"]}
              ]
            }
            """.trimIndent()
        val current = mapOf("r-1" to GemFeatureContributions.Record("r-1", "Rated", playstyle = "Turn-based"))
        val preview = GemFeatureContributions.parse(raw, releases).preview(current)
        assertEquals(setOf("r-2"), preview.added)
        assertEquals(setOf("r-1"), preview.changed)
        assertEquals(listOf("r-3", "r-3"), preview.rejected.map { it.releaseId })
        assertEquals("Turn-based", current.getValue("r-1").playstyle)
    }

    @Test
    fun workIdentityNeedsIndependentEvidenceAndCanNeverBeInferredFromTitle() {
        val raw =
            """
            {
              "schemaVersion":1,
              "records":[
                {"releaseId":"r-1","title":"Rated","primaryGenre":"Strategy",
                 "verifiedWorkId":"work:shared"},
                {"releaseId":"r-2","title":"Candidate","primaryGenre":"Strategy",
                 "verifiedWorkId":"work:shared","workEvidence":"Verified cross-edition work identifier",
                 "sourceId":"independent-release-register"}
              ]
            }
            """.trimIndent()
        val report = GemFeatureContributions.parse(raw, releases)
        assertEquals(setOf("r-2"), report.accepted.keys)
        assertEquals(1, report.rejected.size)
        assertEquals("Verified work identity needs independent source and evidence", report.rejected.single().reason)
        assertEquals("work:shared", report.accepted.getValue("r-2").verifiedWorkId)
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
