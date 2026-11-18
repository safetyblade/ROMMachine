package com.swordfish.lemuroid.app.gemrating

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.security.KeyPairGenerator
import java.security.Signature

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class GemCatalogueVersionsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val source =
        GemCataloguePackage.Source(
            "fixture",
            "Fixture only",
            "https://example.invalid/catalogue",
            "CC0-1.0",
            "Test fixture",
            "1",
        )
    private val first = GemCataloguePackage.Release("first", "gba", "First", source.id, "1", "Strategy")
    private val second = GemCataloguePackage.Release("second", "gba", "Second", source.id, "2", "Puzzle")
    private val third = GemCataloguePackage.Release("third", "gba", "Third", source.id, "3", "Racing")

    private fun payload(
        version: String,
        releases: List<GemCataloguePackage.Release>,
    ): ByteArray =
        Json.encodeToString(
            GemCataloguePackage.Manifest.serializer(),
            GemCataloguePackage.Manifest(1, version, releases.size, listOf(source), releases),
        ).encodeToByteArray()

    private fun baseline(): GemCataloguePackage.Loaded {
        val version = GemProductionCatalogue.VERSION
        val data = payload(version, listOf(first, second))
        val approval = GemCataloguePackage.Approval(version, GemProfileBackup.digest(data), listOf(source))
        return GemCataloguePackage.load(data.inputStream(), approval)
    }

    private fun signed(
        sequence: Long,
        version: String,
        releases: List<GemCataloguePackage.Release>,
    ): ByteArray {
        val data = payload(version, releases)
        val header = "GEMC1".encodeToByteArray() + ByteBuffer.allocate(12).putLong(sequence).putInt(data.size).array()
        val signer = Signature.getInstance("SHA256withRSA")
        signer.initSign(keyPair.private)
        signer.update(header)
        signer.update(data)
        val signature = signer.sign()
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use {
            it.write(header)
            it.write(data)
            it.writeInt(signature.size)
            it.write(signature)
        }
        return output.toByteArray()
    }

    private fun versions() = GemCatalogueVersions(context, keyPair.public, ::baseline)

    @Test
    fun activationRetiresMissingRecordsAndRollbackRestoresPreviousVersion() {
        val manager = versions()
        val update = signed(1, "update-1", listOf(first, third))
        val preview = manager.preview(update.inputStream())
        assertEquals(1, preview.added)
        assertEquals(1, preview.retired)
        assertEquals(GemProductionCatalogue.VERSION, manager.load().manifest.catalogueVersion)
        assertEquals("update-1", manager.activate(preview))
        assertNull(versions().load().index.find(second.releaseId))
        assertEquals(GemProductionCatalogue.VERSION, versions().rollback())
        assertEquals(second.title, versions().load().index.find(second.releaseId)?.title)
        assertTrue(runCatching { versions().preview(update.inputStream()) }.isFailure)
    }

    @Test
    fun retiredIdCannotBeReassignedAcrossUpdates() {
        val manager = versions()
        manager.activate(manager.preview(signed(1, "update-1", listOf(first, third)).inputStream()))
        val wrong = second.copy(title = "Another game")
        val reassigned = signed(2, "update-2", listOf(first, wrong))
        assertTrue(runCatching { manager.preview(reassigned.inputStream()) }.isFailure)
        assertEquals("update-1", manager.load().manifest.catalogueVersion)
    }

    @Test
    fun signatureTamperAndStalePreviewAreRejected() {
        val manager = versions()
        val valid = signed(1, "update-1", listOf(first, third))
        val tampered = valid.copyOf().apply { this[lastIndex] = (this[lastIndex].toInt() xor 1).toByte() }
        assertTrue(runCatching { manager.preview(tampered.inputStream()) }.isFailure)
        val oldPreview = manager.preview(valid.inputStream())
        manager.activate(manager.preview(signed(2, "update-2", listOf(first)).inputStream()))
        assertTrue(runCatching { manager.activate(oldPreview) }.isFailure)
        assertEquals("update-2", manager.load().manifest.catalogueVersion)
    }

    @Test
    fun corruptActivePackageFallsBackToValidatedBaseline() {
        val manager = versions()
        val update = signed(1, "update-1", listOf(first))
        manager.activate(manager.preview(update.inputStream()))
        val root = File(context.noBackupFilesDir, "gem-catalogue-versions")
        File(root, GemProfileBackup.digest(update) + ".gemcatalogue").writeBytes(byteArrayOf(0))
        val status = versions().status()
        assertEquals(GemProductionCatalogue.VERSION, status.loaded)
        assertTrue(status.warning != null)
        assertEquals(GemProductionCatalogue.VERSION, versions().rollback())
    }
}
