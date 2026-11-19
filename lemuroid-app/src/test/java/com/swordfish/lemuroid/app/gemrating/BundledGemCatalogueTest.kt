package com.swordfish.lemuroid.app.gemrating

import com.swordfish.lemuroid.lib.library.db.entity.Game
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BundledGemCatalogueTest {
    private fun asset(): File {
        val relative = "src/main/assets/${BundledGemCatalogue.ASSET_PATH}"
        return listOf(File(relative), File("lemuroid-app", relative)).first { it.isFile }
    }

    @Test
    fun reviewedAssetLoadsAndKeepsRegionalReleasesDistinct() {
        val loaded = asset().inputStream().use { BundledGemCatalogue.load(it) }
        assertEquals(BundledGemCatalogue.VERSION, loaded.manifest.catalogueVersion)
        assertEquals(16, loaded.manifest.releaseCount)
        val cartridge =
            Game(
                fileName = "advance-wars.gba",
                fileUri = "file:///advance-wars.gba",
                title = "Advance Wars",
                systemId = "gba",
                developer = null,
                coverFrontUrl = null,
                lastIndexedAt = 0,
                cartridgeId = "unchanged",
            )
        val match = loaded.match(cartridge) as GemCatalogueIndex.Match.Review
        assertEquals(listOf("TS:r1q96z7y", "TS:rk3061vr"), match.candidates.map { it.releaseId })
        assertEquals("unchanged", cartridge.cartridgeId)
    }

    @Test
    fun tamperedAssetFailsPinnedApproval() {
        val changed = asset().readBytes() + byteArrayOf(32)
        assertTrue(runCatching { BundledGemCatalogue.load(changed.inputStream()) }.isFailure)
    }
}
