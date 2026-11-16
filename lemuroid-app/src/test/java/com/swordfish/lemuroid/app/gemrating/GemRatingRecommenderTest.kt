package com.swordfish.lemuroid.app.gemrating

import com.swordfish.lemuroid.app.gemrating.GemRatingRecommender.Evidence
import com.swordfish.lemuroid.app.gemrating.GemRatingRecommender.Eligibility
import com.swordfish.lemuroid.app.gemrating.GemRatingRecommender.Game
import com.swordfish.lemuroid.app.gemrating.GemRatingRecommender.Rating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class GemRatingRecommenderTest {
    private fun game(id: String, genre: String = "Strategy", mechanic: String = "Grid combat", work: String? = null,
        eligibility: Eligibility = Eligibility.GAME, evidence: Evidence = Evidence.SOURCE_BACKED) = Game(
        releaseId = id, title = id, platform = "GBA", primaryGenre = genre,
        mechanics = setOf(mechanic), verifiedWorkId = work, eligibility = eligibility, evidence = evidence,
    )

    @Test fun coldStartDoesNotInventPreferences() {
        assertTrue(GemRatingRecommender.recommend(listOf(game("A")), emptyList()).isEmpty())
    }

    @Test fun highRatingFindsSpecificSharedFeature() {
        val data = listOf(game("rated"), game("match"), game("other", "Racing", "Cars"))
        assertEquals("match", GemRatingRecommender.recommend(data, listOf(Rating("rated", 9.0))).first().game.releaseId)
    }

    @Test fun negativeRatingsPenalizeRatherThanTrainPositiveMatches() {
        val data = listOf(game("liked"), game("disliked", "Shooter", "FPS"), game("good"), game("bad", "Shooter", "FPS"))
        val r = GemRatingRecommender.recommend(data, listOf(Rating("liked", 9.0), Rating("disliked", 2.0)))
        assertEquals(listOf("good"), r.map { it.game.releaseId })
    }

    @Test fun unknownGenresCannotBecomePositiveSignals() {
        val data = listOf(game("rated", "Unknown", "Arcade"), game("candidate", "Unknown", "Arcade"))
        assertTrue(GemRatingRecommender.recommend(data, listOf(Rating("rated", 10.0))).isEmpty())
    }

    @Test fun knownSameWorkDoesNotInheritRatingOrRecommendDuplicate() {
        val data = listOf(game("EU", work = "verified-work"), game("US", work = "verified-work"), game("other"))
        assertEquals(listOf("other"), GemRatingRecommender.recommend(data, listOf(Rating("EU", 9.0))).map { it.game.releaseId })
    }

    @Test fun identicalNamesWithDifferentReleaseIdsAreNotAutoMerged() {
        val data = listOf(game("Aladdin-SNES"), game("Aladdin-GEN"))
        assertEquals("Aladdin-GEN", GemRatingRecommender.recommend(data, listOf(Rating("Aladdin-SNES", 9.0))).single().game.releaseId)
    }

    @Test fun originalAndDxRemainSeparateWhenNoVerifiedWork() {
        val data = listOf(game("original"), game("dx"))
        assertEquals("dx", GemRatingRecommender.recommend(data, listOf(Rating("original", 9.0))).single().game.releaseId)
    }

    @Test fun excludesHoldAndResearchOnlyAsPlayableSuggestions() {
        val data = listOf(game("rated"), game("demo", eligibility = Eligibility.EXCLUDED), game("research", eligibility = Eligibility.RESEARCH_ONLY))
        assertTrue(GemRatingRecommender.recommend(data, listOf(Rating("rated", 9.0))).isEmpty())
    }

    @Test fun editorialConfidenceDoesNotOutrankEquivalentSourcedEvidence() {
        val data = listOf(game("rated"),game("sourced"),game("editorial", evidence = Evidence.EDITORIAL_PROVISIONAL))
        assertEquals("sourced", GemRatingRecommender.recommend(data, listOf(Rating("rated", 9.0))).first().game.releaseId)
    }

    @Test fun ratingsMustBeInAllowedRange() {
        assertThrows(IllegalArgumentException::class.java) {
            GemRatingRecommender.recommend(listOf(game("A")), listOf(Rating("A", 0.0)))
        }
    }

    @Test fun sameRatingCannotBePassedTwice() {
        assertThrows(IllegalArgumentException::class.java) {
            GemRatingRecommender.recommend(listOf(game("A")), listOf(Rating("A", 8.0), Rating("A", 9.0)))
        }
    }

    @Test fun determinismAcrossCalls() {
        val data = listOf(game("rated"), game("c"), game("b"))
        val x = GemRatingRecommender.recommend(data, listOf(Rating("rated", 9.0)))
        assertEquals(x, GemRatingRecommender.recommend(data, listOf(Rating("rated", 9.0))))
    }

    @Test fun wildcardShareCannotExceedHalf() {
        assertThrows(IllegalArgumentException::class.java) {
            GemRatingRecommender.recommend(listOf(game("A")), emptyList(), wildcardShare = 0.8)
        }
    }

    @Test fun recommendationReasonReferencesActualEvidence() {
        val data = listOf(game("rated"), game("candidate"))
        val rec = GemRatingRecommender.recommend(data, listOf(Rating("rated", 10.0))).single()
        assertTrue(rec.reason.contains("grid combat"))
        assertTrue(rec.reason.contains("rated"))
    }
}
