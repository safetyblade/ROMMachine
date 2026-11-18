package com.swordfish.lemuroid.app.gemrating

import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class GemCataloguePackageTest {
    private val source = GemCataloguePackage.Source("fixture", "Synthetic fixture", "https://example.invalid", "Test only", "Synthetic", "1")
    private val release = GemCataloguePackage.Release("r1", "gba", "Fixture", source.id, "s1", verifiedSerials = setOf("ABC"))
    private val manifest = GemCataloguePackage.Manifest(1, "fixture-1", 1, listOf(source), listOf(release))
    private fun bytes(value: GemCataloguePackage.Manifest) =
        Json.encodeToString(GemCataloguePackage.Manifest.serializer(), value).encodeToByteArray()
    private fun approval(data: ByteArray) = GemCataloguePackage.Approval(
        "fixture-1",
        MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it.toInt() and 255) },
        listOf(source),
    )

    @Test
    fun validatedPackageConnectsToMatcher() {
        val data = bytes(manifest)
        val loaded = GemCataloguePackage.load(data.inputStream(), approval(data))
        val game = Game(fileName = "fixture.gba", fileUri = "file:///fixture.gba", title = "Fixture",
            systemId = "gba", developer = null, coverFrontUrl = null, lastIndexedAt = 0, cartridgeId = "stable")
        assertTrue(loaded.match(game) is GemCatalogueIndex.Match.Review)
        assertEquals("r1", (loaded.match(game, GemCatalogueIndex.Evidence(serial = "ABC")) as GemCatalogueIndex.Match.Confirmed).release.releaseId)
        assertEquals("stable", game.cartridgeId)
    }

    @Test
    fun alteredBytesRejected() {
        val data = bytes(manifest)
        assertTrue(runCatching { GemCataloguePackage.load((data + byteArrayOf(32)).inputStream(), approval(data)) }.isFailure)
    }

    @Test
    fun unsupportedSchemaRejectedEvenWithMatchingDigest() {
        val data = bytes(manifest.copy(schemaVersion = 2))
        assertTrue(runCatching { GemCataloguePackage.load(data.inputStream(), approval(data)) }.isFailure)
    }

    @Test
    fun unapprovedSourceRejected() {
        val data = bytes(manifest.copy(sources = listOf(source.copy(licence = "different"))))
        assertTrue(runCatching { GemCataloguePackage.load(data.inputStream(), approval(data)) }.isFailure)
    }

    @Test
    fun incorrectCountRejected() {
        val data = bytes(manifest.copy(releaseCount = 2))
        assertTrue(runCatching { GemCataloguePackage.load(data.inputStream(), approval(data)) }.isFailure)
    }

    @Test
    fun duplicateReleaseRejected() {
        val data = bytes(manifest.copy(releaseCount = 2, releases = listOf(release, release.copy(sourceRecordId = "s2"))))
        assertTrue(runCatching { GemCataloguePackage.load(data.inputStream(), approval(data)) }.isFailure)
    }
}
