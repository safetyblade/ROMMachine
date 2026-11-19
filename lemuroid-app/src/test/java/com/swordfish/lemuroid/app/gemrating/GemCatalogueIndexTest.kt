package com.swordfish.lemuroid.app.gemrating

import com.swordfish.lemuroid.lib.library.db.entity.Game
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GemCatalogueIndexTest {
    private val hash = "a".repeat(64)
    private val a = GemCatalogueIndex.Release("r1", "gba", "Example", setOf(hash), setOf("ABC-123"))
    private val b = GemCatalogueIndex.Release("r2", "gba", "Example", verifiedSerials = setOf("DEF-456"))
    private val other = GemCatalogueIndex.Release("r3", "nds", "Example", setOf(hash), setOf("ABC-123"))
    private val game =
        Game(
            fileName = "example.gba",
            fileUri = "file:///example.gba",
            title = "Example",
            systemId = "gba",
            developer = null,
            coverFrontUrl = null,
            lastIndexedAt = 0,
            cartridgeId = "fixture-cartridge",
        )
    private val index = GemCatalogueIndex("fixture-1", listOf(a, b, other))

    @Test
    fun uniqueHashConfirmsWithinPlatform() {
        val result = index.match(game, GemCatalogueIndex.Evidence(sha256 = hash)) as GemCatalogueIndex.Match.Confirmed
        assertEquals("r1", result.release.releaseId)
    }

    @Test
    fun uniqueSerialConfirmsWithinPlatform() {
        val result = index.match(game, GemCatalogueIndex.Evidence(serial = "ABC-123"))
        assertEquals("r1", (result as GemCatalogueIndex.Match.Confirmed).release.releaseId)
    }

    @Test
    fun titleOnlyIsNeverConfirmed() {
        val result = index.match(game) as GemCatalogueIndex.Match.Review
        assertEquals(listOf("r1", "r2"), result.candidates.map { it.releaseId })
        assertTrue(GemCatalogueIndex("v", listOf(a)).match(game) is GemCatalogueIndex.Match.Review)
    }

    @Test
    fun contradictoryStrongEvidenceRequiresReview() {
        assertTrue(index.match(game, GemCatalogueIndex.Evidence(hash, "DEF-456")) is GemCatalogueIndex.Match.Review)
    }

    @Test
    fun unknownProvidedSerialDoesNotSilentlyConfirmHash() {
        assertTrue(index.match(game, GemCatalogueIndex.Evidence(hash, "unknown")) is GemCatalogueIndex.Match.Review)
    }

    @Test
    fun malformedProvidedHashDoesNotSilentlyConfirmSerial() {
        val result = index.match(game, GemCatalogueIndex.Evidence("invalid", "ABC-123"))
        assertTrue(result is GemCatalogueIndex.Match.Review)
    }

    @Test
    fun blankProvidedSerialCannotConfirmExistingHash() {
        assertTrue(index.match(game, GemCatalogueIndex.Evidence(hash, "  ")) is GemCatalogueIndex.Match.Review)
    }

    @Test
    fun normalizedTitleIndexDoesNotCrossPlatforms() {
        val result = index.match(game.copy(title = " EXAMPLE  ")) as GemCatalogueIndex.Match.Review
        assertEquals(listOf("r1", "r2"), result.candidates.map { it.releaseId })
    }

    @Test
    fun duplicateStrongEvidenceRequiresReview() {
        val duplicate = b.copy(verifiedSha256 = setOf(hash))
        val result = GemCatalogueIndex("v", listOf(a, duplicate)).match(game, GemCatalogueIndex.Evidence(hash))
        assertTrue(result is GemCatalogueIndex.Match.Review)
    }

    @Test
    fun unmatchedRemainsUsableWithoutCatalogue() {
        assertEquals(GemCatalogueIndex.Match.Unmatched, GemCatalogueIndex("empty", emptyList()).match(game))
        assertEquals("fixture-cartridge", game.cartridgeId)
        assertEquals("example.gba", game.saveFileName())
    }

    @Test
    fun customDisplayNameDoesNotBecomeMatchEvidence() {
        val renamed = game.copy(title = "Unrelated", customName = "Example")
        assertEquals(GemCatalogueIndex.Match.Unmatched, index.match(renamed))
    }

    @Test
    fun duplicateReleaseIdsAreRejected() {
        assertTrue(runCatching { GemCatalogueIndex("v", listOf(a, a)) }.isFailure)
    }

    @Test
    fun malformedCatalogueHashIsRejected() {
        assertTrue(runCatching { GemCatalogueIndex("v", listOf(a.copy(verifiedSha256 = setOf("abc")))) }.isFailure)
    }
}
