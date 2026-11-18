package com.swordfish.lemuroid.app.gemrating

import com.swordfish.lemuroid.lib.library.db.entity.Game
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GemProductionCatalogueTest {
    private fun assets(): File =
        listOf(File("src/main/assets"), File("lemuroid-app/src/main/assets")).first { it.isDirectory }

    @Test
    fun allPinnedSourcePackagesLoadAndMatchWithoutReassigningIdentity() {
        val directory = assets()
        val loaded = GemProductionCatalogue.load { File(directory, it).inputStream() }
        assertEquals(14216, loaded.manifest.releaseCount)
        assertEquals(23, loaded.manifest.releases.map { it.systemId }.distinct().size)
        val advanceWars = loaded.manifest.releases.first { it.systemId == "gba" && it.title == "Advance Wars" }
        val game =
            Game(
                fileName = "advance-wars.gba",
                fileUri = "file:///advance-wars.gba",
                title = advanceWars.title,
                systemId = "gba",
                developer = null,
                coverFrontUrl = null,
                lastIndexedAt = 0,
                cartridgeId = "fixture",
            )
        val match = loaded.match(game) as GemCatalogueIndex.Match.Review
        assertTrue(match.candidates.any { it.releaseId == advanceWars.releaseId })
        assertEquals("fixture", game.cartridgeId)
        assertEquals("advance-wars.gba", game.saveFileName())
    }

    @Test
    fun alteredShardDoesNotProducePartialCatalogue() {
        val directory = assets()
        val result =
            runCatching {
                GemProductionCatalogue.load { path ->
                    val file = File(directory, path)
                    if (path == "gem/vdg/gba.json") {
                        (file.readBytes() + byteArrayOf(32)).inputStream()
                    } else {
                        file.inputStream()
                    }
                }
            }
        assertTrue(result.isFailure)
    }
}
