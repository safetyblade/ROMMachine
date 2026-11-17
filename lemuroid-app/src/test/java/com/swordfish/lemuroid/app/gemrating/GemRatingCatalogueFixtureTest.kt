package com.swordfish.lemuroid.app.gemrating

import com.swordfish.lemuroid.app.gemrating.GemRatingRecommender.Eligibility
import com.swordfish.lemuroid.app.gemrating.GemRatingRecommender.Evidence
import com.swordfish.lemuroid.app.gemrating.GemRatingRecommender.Game
import com.swordfish.lemuroid.app.gemrating.GemRatingRecommender.Rating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** GEM Master sample IDs and feature labels from 2026-11-16.
 * Eligibility.GAME is a synthetic eligibility fixture, NOT emulator compatibility proof.
 */
class GemRatingCatalogueFixtureTest {
    @Test fun firstPersonShooterPreferenceRanksGoldenEyeAboveUnrelatedShooter() {
        val catalogue =
            listOf(
                Game(
                    "VDG:xb:522",
                    "Halo: Combat Evolved",
                    "Xbox",
                    primaryGenre = "Shooter",
                    playstyle = "First-person",
                    mechanics = setOf("FPS"),
                    eligibility = Eligibility.GAME,
                    evidence = Evidence.EDITORIAL_PROVISIONAL,
                ),
                Game(
                    "VDG:n64:202",
                    "GoldenEye 007",
                    "Nintendo 64",
                    primaryGenre = "Shooter",
                    playstyle = "First-person",
                    mechanics = setOf("FPS", "Stealth"),
                    eligibility = Eligibility.GAME,
                    evidence = Evidence.EDITORIAL_PROVISIONAL,
                ),
                Game(
                    "VDG:gc:382",
                    "Ikaruga",
                    "GameCube",
                    primaryGenre = "Shooter",
                    playstyle = "Vertical scrolling",
                    mechanics = setOf("Polarity switching"),
                    eligibility = Eligibility.GAME,
                    evidence = Evidence.SOURCE_BACKED,
                ),
            )
        val results = GemRatingRecommender.recommend(catalogue, listOf(Rating("VDG:xb:522", 9.0)))
        assertEquals("VDG:n64:202", results.first().game.releaseId)
        assertTrue(results.first().reason.contains("first-person"))
    }

    @Test fun researchOnlyCatalogueEntriesAreNotPresentedAsPlayable() {
        val catalogue =
            listOf(
                Game(
                    "VDG:gb:94",
                    "Pokémon Trading Card Game",
                    "Game Boy Color",
                    primaryGenre = "Strategy",
                    crossoverGenre = "Card Game",
                    mechanics = setOf("Deckbuilding"),
                    eligibility = Eligibility.GAME,
                ),
                Game(
                    "VDG:xb:85",
                    "Phantom Dust",
                    "Xbox",
                    primaryGenre = "Action",
                    crossoverGenre = "Card Game",
                    mechanics = setOf("Deckbuilding"),
                    eligibility = Eligibility.RESEARCH_ONLY,
                ),
            )
        assertTrue(GemRatingRecommender.recommend(catalogue, listOf(Rating("VDG:gb:94", 10.0))).isEmpty())
    }
}
