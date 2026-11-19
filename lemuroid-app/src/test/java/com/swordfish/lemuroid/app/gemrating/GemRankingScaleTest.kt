package com.swordfish.lemuroid.app.gemrating

import com.swordfish.lemuroid.app.gemrating.GemRatingRecommender.Eligibility
import com.swordfish.lemuroid.app.gemrating.GemRatingRecommender.Evidence
import com.swordfish.lemuroid.app.gemrating.GemRatingRecommender.Game
import com.swordfish.lemuroid.app.gemrating.GemRatingRecommender.Rating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt
import kotlin.system.measureTimeMillis

class GemRankingScaleTest {
    private fun game(
        id: String,
        genre: String,
        mechanics: Set<String> = emptySet(),
    ) = Game(
        releaseId = id,
        title = id,
        platform = "gba",
        primaryGenre = genre,
        mechanics = mechanics,
        eligibility = Eligibility.GAME,
        evidence = Evidence.SOURCE_BACKED,
    )

    @Test
    fun sparseScorerPreservesSignedCosineAndDeterministicExplanations() {
        val liked = game("a-liked", "Strategy", setOf("Deckbuilding"))
        val disliked = game("b-disliked", "Action", setOf("Deckbuilding"))
        val target = game("target", "Strategy", setOf("Deckbuilding"))
        val ratings = listOf(Rating(liked.releaseId, 10.0), Rating(disliked.releaseId, 3.0))
        val catalogue = listOf(target, disliked, liked)
        val result = GemRatingRecommender.recommend(catalogue, ratings, wildcardShare = 0.0).single()
        // Genre weight .25 and single mechanics weight .50; same vector cosine = 1.
        val cross = 0.50 * 0.50 / (sqrt(0.25 * 0.25 + 0.50 * 0.50) * sqrt(0.25 * 0.25 + 0.50 * 0.50))
        val expected = (1.0 + cross * ((3.0 - 5.5) / 4.5)) / (1.0 + cross)
        assertEquals(expected, result.score, 1e-10)
        assertTrue(result.reason.contains(liked.title))
        assertEquals(
            result,
            GemRatingRecommender.recommend(catalogue.reversed(), ratings.reversed(), wildcardShare = 0.0).single(),
        )
    }

    @Test
    fun fullCatalogueScaleIsBoundedDeterministicAndRespectsScope() {
        val catalogue = (0 until 14_216).map { game("release-$it", "genre-${it % 100}") }
        val ratings = (0 until 500).map { Rating("release-$it", if (it % 3 == 0) 3.0 else 9.0) }
        val allowed = catalogue.filterIndexed { index, _ -> index % 2 == 0 }.map { it.releaseId }.toSet()
        val rated = ratings.map { it.releaseId }.toSet()
        val elapsed =
            measureTimeMillis {
                val result =
                    GemRatingRecommender.recommend(catalogue, ratings, limit = 20, candidateIds = allowed)
                assertEquals(20, result.size)
                assertTrue(result.all { it.game.releaseId in allowed && it.game.releaseId !in rated && !it.wildcard })
                val repeated =
                    GemRatingRecommender.recommend(catalogue.reversed(), ratings.reversed(), 20, candidateIds = allowed)
                assertEquals(result, repeated)
            }
        // CI timing is diagnostic, not a device-specific acceptance threshold.
        println("GEM synthetic 14,216 records / 500 ratings / 2 rankings: ${elapsed}ms")
    }
}
