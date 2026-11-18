package com.swordfish.lemuroid.app.gemrating

import com.swordfish.lemuroid.app.gemrating.GemRatingRecommender.Eligibility
import com.swordfish.lemuroid.app.gemrating.GemRatingRecommender.Evidence
import com.swordfish.lemuroid.app.gemrating.GemRatingRecommender.Game
import com.swordfish.lemuroid.app.gemrating.GemRatingRecommender.Rating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GemDiscoveryTest {
    private fun game(
        id: String,
        genre: String = "Strategy",
        mechanics: Set<String> = setOf("Deckbuilding"),
        work: String? = null,
        eligibility: Eligibility = Eligibility.GAME,
    ) = Game(
        releaseId = id,
        title = id,
        platform = "gba",
        primaryGenre = genre,
        mechanics = mechanics,
        verifiedWorkId = work,
        eligibility = eligibility,
        evidence = Evidence.SOURCE_BACKED,
    )

    @Test
    fun wildcardQuotaNeverSpillsIntoStandardSlots() {
        val games =
            listOf(game("liked")) +
                (1..14).map { game("ordinary-$it") } +
                (1..14).map { game("novel-$it", "Action") }
        val ratings = listOf(Rating("liked", 10.0))
        for (percent in listOf(20, 30, 50)) {
            val result = GemRatingRecommender.recommend(games, ratings, 10, percent / 100.0)
            assertEquals(10, result.size)
            assertEquals(percent / 10, result.count { it.wildcard })
        }
    }

    @Test
    fun sparseFeedDoesNotExceedQuota() {
        val games = listOf(game("liked"), game("normal"), game("novel", "Action"))
        val result = GemRatingRecommender.recommend(games, listOf(Rating("liked", 10.0)))
        assertEquals(listOf("normal"), result.map { it.game.releaseId })
    }

    @Test
    fun surpriseRequiresMeaningfulDivergenceAndAvoidsRecentChoices() {
        val games =
            listOf(
                game("liked"),
                game("near"),
                game("novel-a", "Action"),
                game("novel-b", "Adventure"),
                game("unrelated", "Racing", setOf("Lap racing")),
            )
        val ratings = listOf(Rating("liked", 10.0))
        val first = requireNotNull(GemRatingRecommender.surprise(games, ratings))
        assertTrue(first.game.releaseId.startsWith("novel-"))
        val second =
            requireNotNull(GemRatingRecommender.surprise(games, ratings, setOf(first.game.releaseId)))
        assertTrue(second.game.releaseId.startsWith("novel-") && first != second)
        assertNull(GemRatingRecommender.surprise(games, ratings, setOf(first.game.releaseId, second.game.releaseId)))
    }

    @Test
    fun surpriseDoesNotInventColdStartOrGenreOnlyBridge() {
        val games = listOf(game("liked", mechanics = emptySet()), game("same", mechanics = emptySet()))
        assertNull(GemRatingRecommender.surprise(games, emptyList()))
        assertNull(GemRatingRecommender.surprise(games, listOf(Rating("liked", 10.0))))
    }

    @Test
    fun scopedRankingKeepsTrainingOutsideTheCandidateSet() {
        val games = listOf(game("liked"), game("in-scope"), game("outside"))
        val result =
            GemRatingRecommender.recommend(
                games,
                listOf(Rating("liked", 10.0)),
                candidateIds = setOf("in-scope"),
            )
        assertEquals(listOf("in-scope"), result.map { it.game.releaseId })
    }

    @Test
    fun researchRatingsAndDuplicateVerifiedWorksDoNotPolluteFeed() {
        val games =
            listOf(
                game("liked"),
                game("version-one", work = "known"),
                game("version-two", work = "known"),
                game("research", eligibility = Eligibility.RESEARCH_ONLY),
            )
        assertTrue(GemRatingRecommender.recommend(games, listOf(Rating("research", 10.0))).isEmpty())
        val result = GemRatingRecommender.recommend(games, listOf(Rating("liked", 10.0)))
        assertEquals(1, result.size)
    }

    @Test
    fun titleOnlyOwnershipAndRatingRemainUnconfirmed() {
        val release = GemCataloguePackage.Release("r", "gba", "Same", "source", "id", "Strategy")
        val score = GemRatingRepository.Rating("cartridge", 9, 1L, 123L, "Same", "gba")
        val shelf = listOf(GemDiscovery.ShelfEntry("cartridge", "gba", "Same"))
        val input = GemDiscovery.prepare(listOf(release), listOf(score), emptyMap(), shelf)
        assertTrue(input.ratings.isEmpty())
        assertEquals(1, input.unlinkedRatings)
        assertTrue(input.ownedIds.isEmpty())
        assertTrue(input.candidates(GemDiscovery.Scope.DISCOVER).isEmpty())
    }

    @Test
    fun removedLinkedRatingStillTrainsButDoesNotClaimOwnership() {
        val release = GemCataloguePackage.Release("r", "gba", "Same", "source", "id", "Strategy")
        val score = GemRatingRepository.Rating("cartridge", 9, 1L, 123L, "Same", "gba")
        val input = GemDiscovery.prepare(listOf(release), listOf(score), mapOf("cartridge" to "r"), emptyList())
        assertEquals(listOf(Rating("r", 9.0)), input.ratings)
        assertTrue(input.ownedIds.isEmpty())
        assertEquals(0, input.unlinkedRatings)
    }

    @Test
    fun provisionalFeatureSeedRequiresExactIdAndTitle() {
        assertTrue(GemGameplayFeatures.find("VDG:xb:row-85", "Phantom Dust") != null)
        assertNull(GemGameplayFeatures.find("VDG:xb:row-85", "Another game"))
    }
}
