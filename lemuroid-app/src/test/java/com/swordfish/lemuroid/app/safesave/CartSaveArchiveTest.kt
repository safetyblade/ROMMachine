package com.swordfish.lemuroid.app.safesave

import com.swordfish.lemuroid.app.mobile.feature.product.storeUnassignedArchive
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

class CartSaveArchiveTest {
    @get:Rule val temp = TemporaryFolder()

    @Test
    fun preservesTheWholeCardAndIndependentStateSlots() {
        val fixture = fixture()
        val archive = File(temp.root, "backup.cartsave")
        CartSaveArchive.write(archive, fixture.first, fixture.second)
        assertEquals(fixture.first, CartSaveArchive.inspect(archive))
        assertEquals(131072L, fixture.first.saveGroups.single().artifacts.first().byteLength)
    }

    @Test
    fun unassignedArchiveIsRetainedWithoutRewritingOrTrustingChangedInput() {
        val (manifest, payloads) = fixture()
        val archive = File(temp.root, "incoming.cartsave")
        CartSaveArchive.write(archive, manifest, payloads)
        val unassigned = File(temp.root, "unassigned")
        val stored = storeUnassignedArchive(unassigned, manifest, archive::inputStream)
        assertEquals(manifest, CartSaveArchive.inspect(stored))
        assertEquals(stored, storeUnassignedArchive(unassigned, manifest, archive::inputStream))
        assertThrows(IllegalArgumentException::class.java) {
            storeUnassignedArchive(unassigned, manifest) { byteArrayOf(1, 2, 3).inputStream() }
        }
        assertEquals(listOf(stored.name), unassigned.list()?.toList())
        assertEquals(manifest, CartSaveArchive.inspect(stored))
    }

    @Test
    fun ignoresNeitherCorruptPayloadsNorUndeclaredEntries() {
        val fixture = fixture()
        val archive = File(temp.root, "backup.cartsave")
        CartSaveArchive.write(archive, fixture.first, fixture.second)
        val artifact = fixture.first.saveGroups.single().artifacts.first()
        val corrupt = File(temp.root, "corrupt.cartsave")
        rewrite(archive, corrupt, artifact.path) { zip ->
            zip.putNextEntry(ZipEntry(artifact.path))
            zip.write(ByteArray(artifact.byteLength.toInt()) { 1 })
            zip.closeEntry()
        }
        assertThrows(IllegalArgumentException::class.java) { CartSaveArchive.inspect(corrupt) }
        val traversal = File(temp.root, "traversal.cartsave")
        rewrite(archive, traversal) { zip ->
            zip.putNextEntry(ZipEntry("../live.srm"))
            zip.write(1)
            zip.closeEntry()
        }
        assertThrows(IllegalArgumentException::class.java) { CartSaveArchive.inspect(traversal) }
    }

    @Test
    fun sourceChangedAfterInventoryIsNotPublished() {
        val (manifest, files) = fixture()
        files.values.first().appendBytes(byteArrayOf(1))
        val archive = File(temp.root, "backup.cartsave")
        assertThrows(IllegalArgumentException::class.java) { CartSaveArchive.write(archive, manifest, files) }
        assertFalse(archive.exists())
    }

    private fun rewrite(
        source: File,
        destination: File,
        omitted: String? = null,
        extra: (ZipOutputStream) -> Unit,
    ) {
        ZipFile(source).use { input ->
            ZipOutputStream(destination.outputStream()).use { output ->
                output.setLevel(0)
                input.entries().asSequence().filter { it.name != omitted }.forEach { entry ->
                    output.putNextEntry(ZipEntry(entry.name))
                    input.getInputStream(entry).use { it.copyTo(output) }
                    output.closeEntry()
                }
                extra(output)
            }
        }
    }

    private fun fixture(): Pair<CartSaveArchive.Manifest, Map<String, File>> {
        val kinds =
            listOf(
                CartSaveArchive.Kind.MEMORY_CARD,
                CartSaveArchive.Kind.MANUAL_STATE,
                CartSaveArchive.Kind.MANUAL_STATE,
                CartSaveArchive.Kind.AUTOSAVE,
                CartSaveArchive.Kind.STATE_METADATA,
                CartSaveArchive.Kind.STATE_PREVIEW,
            )
        val files =
            kinds.mapIndexed { index, _ ->
                temp.newFile().canonicalFile.apply { writeBytes(ByteArray(if (index == 0) 131072 else 16)) }
            }
        val artifacts =
            kinds.mapIndexed { index, kind ->
                CartSaveArchive.artifact(kind, files[index], "file-$index", index.toString())
            }
        val player = UUID.randomUUID().toString()
        return CartSaveArchive.Manifest(
            UUID.randomUUID().toString(),
            "2026-11-22T02:03:04Z",
            CartSaveArchive.Scope.GAME,
            CartSaveArchive.Source(13, CartSaveArchive.Player(player)),
            CartSaveArchive.Coverage(1, 0, emptyList()),
            listOf(
                CartSaveArchive.Group(
                    UUID.randomUUID().toString(),
                    CartSaveArchive.Scope.CARD_BANK,
                    "psx",
                    "pcsx_rearmed",
                    "fixture",
                    listOf(UUID.randomUUID().toString()),
                    artifacts,
                    ownerId = player,
                ),
            ),
        ) to artifacts.map { it.artifactId }.zip(files).toMap()
    }
}
