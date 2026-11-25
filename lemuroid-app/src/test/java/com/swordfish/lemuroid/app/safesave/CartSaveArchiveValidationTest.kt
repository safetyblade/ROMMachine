package com.swordfish.lemuroid.app.safesave

import com.swordfish.lemuroid.lib.saves.safesave.CartSaveArchive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * SAVE-A03 — CartSaveArchive rejection contracts not covered by
 * [CartSaveArchiveTest]: an archive that changed after preview must never be
 * extracted, packaging must never overwrite an existing staging file or
 * publish a payload set that does not match the manifest, a tampered
 * manifest checksum must fail inspection, and native account identifiers
 * must never be packaged for sharing.
 */
class CartSaveArchiveValidationTest {
    @get:Rule val temp = TemporaryFolder()

    @Test
    fun extractVerifiedRefusesAnArchiveThatChangedAfterPreview() {
        val (manifest, payloads) = fixture()
        val archive = File(temp.root, "backup.cartsave")
        CartSaveArchive.write(archive, manifest, payloads)
        val changedAfterPreview = manifest.copy(createdAtUtc = "2026-11-22T02:03:05Z")
        val stagingParent = File(temp.root, "staging")
        assertThrows(IllegalArgumentException::class.java) {
            CartSaveArchive.extractVerified(archive, stagingParent, changedAfterPreview)
        }
        assertFalse(stagingParent.exists())
    }

    @Test
    fun writeRefusesToOverwriteAnExistingStagingFile() {
        val (manifest, payloads) = fixture()
        val archive = temp.newFile("backup.cartsave")
        assertThrows(IllegalStateException::class.java) {
            CartSaveArchive.write(archive, manifest, payloads)
        }
        assertEquals(0L, archive.length())
    }

    @Test
    fun writeRejectsAPayloadSetThatDoesNotMatchTheManifest() {
        val (manifest, _) = fixture()
        val archive = File(temp.root, "backup.cartsave")
        assertThrows(IllegalArgumentException::class.java) {
            CartSaveArchive.write(archive, manifest, emptyMap())
        }
        assertFalse(archive.exists())
    }

    @Test
    fun inspectRejectsATamperedManifestChecksum() {
        val (manifest, payloads) = fixture()
        val archive = File(temp.root, "backup.cartsave")
        CartSaveArchive.write(archive, manifest, payloads)
        val tampered = File(temp.root, "tampered.cartsave")
        rewriteReplacing(archive, tampered, "manifest.sha256") {
            "0".repeat(64).toByteArray()
        }
        assertThrows(IllegalArgumentException::class.java) { CartSaveArchive.inspect(tampered) }
    }

    @Test
    fun writeRejectsNativeAccountIdentifiers() {
        val (manifest, payloads) = fixture(nativeUserRef = "native-user-1")
        val archive = File(temp.root, "backup.cartsave")
        assertThrows(IllegalArgumentException::class.java) {
            CartSaveArchive.write(archive, manifest, payloads)
        }
        assertFalse(archive.exists())
    }

    private fun rewriteReplacing(
        source: File,
        destination: File,
        replaced: String,
        replacement: () -> ByteArray,
    ) {
        ZipFile(source).use { input ->
            ZipOutputStream(destination.outputStream()).use { output ->
                output.setLevel(0)
                input.entries().asSequence().forEach { entry ->
                    output.putNextEntry(ZipEntry(entry.name))
                    if (entry.name == replaced) {
                        output.write(replacement())
                    } else {
                        input.getInputStream(entry).use { it.copyTo(output) }
                    }
                    output.closeEntry()
                }
            }
        }
    }

    private fun fixture(nativeUserRef: String? = null): Pair<CartSaveArchive.Manifest, Map<String, File>> {
        val file = temp.newFile().canonicalFile.apply { writeBytes("level-3-progress".toByteArray()) }
        val artifact = CartSaveArchive.artifact(CartSaveArchive.Kind.GAME_SAVE, file, "DATA.BIN", "0")
        val player = UUID.randomUUID().toString()
        val manifest =
            CartSaveArchive.Manifest(
                UUID.randomUUID().toString(),
                "2026-11-22T02:03:04Z",
                CartSaveArchive.Scope.GAME,
                CartSaveArchive.Source(13, CartSaveArchive.Player(player)),
                CartSaveArchive.Coverage(1, 0, emptyList()),
                listOf(
                    CartSaveArchive.Group(
                        UUID.randomUUID().toString(),
                        CartSaveArchive.Scope.GAME,
                        "psp",
                        "ppsspp",
                        "fixture",
                        listOf(UUID.randomUUID().toString()),
                        listOf(artifact),
                        nativeUserRef = nativeUserRef,
                        ownerId = player,
                    ),
                ),
            )
        return manifest to mapOf(artifact.artifactId to file)
    }
}
