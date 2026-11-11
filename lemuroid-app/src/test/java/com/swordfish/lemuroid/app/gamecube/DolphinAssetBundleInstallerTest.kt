package com.swordfish.lemuroid.app.gamecube

import com.swordfish.lemuroid.lib.core.assetsmanager.DolphinAssetBundleInstaller
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DolphinAssetBundleInstallerTest {
    private fun zip(vararg entries: Pair<String, String>): ByteArray =
        ByteArrayOutputStream().also { bytes ->
            ZipOutputStream(bytes).use { stream ->
                entries.forEach { (name, content) ->
                    stream.putNextEntry(ZipEntry(name))
                    stream.write(content.toByteArray())
                    stream.closeEntry()
                }
            }
        }.toByteArray()

    private fun hash(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 255) }

    private fun manifest(
        core: File,
        archive: ByteArray,
        revision: String = DolphinAssetBundleInstaller.SOURCE_REVISION,
    ) = ByteArrayInputStream(
        (
            "upstream.revision=$revision\ncore.sha256=${hash(core.readBytes())}\n" +
                "assets.sha256=${hash(archive)}\n"
        ).toByteArray(),
    )

    private inline fun fixture(run: (File, File) -> Unit) {
        val root = Files.createTempDirectory("dolphin-bundle").toFile()
        try {
            val core = File(root, "core.so").apply { writeText("core fixture") }
            run(root, core)
        } finally {
            root.deleteRecursively()
        }
    }

    private val valid get() = zip("GC/font.bin" to "font", "GameSettings/test.ini" to "rules")

    @Test fun installsSysAndPreservesMemoryCardsAndRollback() =
        fixture { root, core ->
            val previous =
                File(root, "dolphin-emu/Sys/old.ini").apply {
                    parentFile.mkdirs()
                    writeText("old")
                }
            val card =
                File(root, "saves/User/GC/MemoryCardA.raw").apply {
                    parentFile.mkdirs()
                    writeText("progress")
                }
            val archive = valid
            DolphinAssetBundleInstaller.install(manifest(core, archive), core, { ByteArrayInputStream(archive) }, root)
            assertTrue(File(root, "dolphin-emu/Sys/GC/font.bin").isFile)
            assertFalse(previous.exists())
            val rollback = File(root, "dolphin-emu").listFiles()!!.single { it.name.startsWith("Sys.rollback-") }
            assertEquals("old", File(rollback, "old.ini").readText())
            assertEquals("progress", card.readText())
            DolphinAssetBundleInstaller.install(manifest(core, archive), core, { ByteArrayInputStream(archive) }, root)
            assertEquals(1, File(root, "dolphin-emu").listFiles()!!.count { it.name.startsWith("Sys.rollback-") })
        }

    @Test fun rejectsArchiveMismatchWithoutReplacingWorkingSys() =
        fixture { root, core ->
            val old =
                File(root, "dolphin-emu/Sys/old").apply {
                    parentFile.mkdirs()
                    writeText("working")
                }
            assertThrows(IllegalStateException::class.java) {
                DolphinAssetBundleInstaller.install(
                    manifest(core, valid),
                    core,
                    { ByteArrayInputStream(byteArrayOf(1)) },
                    root,
                )
            }
            assertEquals("working", old.readText())
        }

    @Test fun rejectsCoreMismatch() =
        fixture { root, core ->
            val archive = valid
            val metadata = manifest(core, archive)
            core.writeText("another revision")
            assertThrows(IllegalStateException::class.java) {
                DolphinAssetBundleInstaller.install(metadata, core, { ByteArrayInputStream(archive) }, root)
            }
            assertFalse(File(root, "dolphin-emu/Sys").exists())
        }

    @Test fun rejectsWrongRevisionAndIncompleteSys() =
        fixture { root, core ->
            assertThrows(IllegalStateException::class.java) {
                DolphinAssetBundleInstaller.install(
                    manifest(core, valid, "wrong"),
                    core,
                    { ByteArrayInputStream(valid) },
                    root,
                )
            }
            val incomplete = zip("GC/font.bin" to "font")
            assertThrows(IllegalStateException::class.java) {
                DolphinAssetBundleInstaller.install(
                    manifest(core, incomplete),
                    core,
                    { ByteArrayInputStream(incomplete) },
                    root,
                )
            }
            assertFalse(File(root, "dolphin-emu/Sys").exists())
        }

    @Test fun rejectsTraversalAndRestoresInterruptedPreviousVersion() =
        fixture { root, core ->
            val previous =
                File(root, "dolphin-emu/Sys.rollback-test/old").apply {
                    parentFile.mkdirs()
                    writeText("working")
                }
            val unsafe = zip("../User/GC/card.raw" to "damage")
            assertThrows(IllegalStateException::class.java) {
                DolphinAssetBundleInstaller.install(
                    manifest(core, unsafe),
                    core,
                    { ByteArrayInputStream(unsafe) },
                    root,
                )
            }
            assertFalse(previous.exists())
            assertEquals("working", File(root, "dolphin-emu/Sys/old").readText())
            assertFalse(File(root, "dolphin-emu/User/GC/card.raw").exists())
        }
}
