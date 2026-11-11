package com.swordfish.lemuroid.app.gamecube

import com.swordfish.lemuroid.lib.core.VersionedCoreStore
import com.swordfish.lemuroid.lib.core.assetsmanager.VersionedDolphinAssets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class VersionedDolphinAssetsTest {
    private class Bundle(val revision: String, val core: File, val archive: ByteArray) {
        val sha =
            MessageDigest.getInstance("SHA-256").digest(core.readBytes()).joinToString("") {
                "%02x".format(it.toInt() and 255)
            }
        val version = "${revision.take(12)}-${sha.take(12)}"

        fun manifest() =
            (
                "upstream.revision=$revision\ncore.sha256=$sha\nassets.sha256=" +
                    MessageDigest.getInstance("SHA-256").digest(archive).joinToString("") {
                        "%02x".format(it.toInt() and 255)
                    } + "\n"
            ).byteInputStream()
    }

    private fun bundle(
        root: File,
        name: String,
        revision: String,
    ): Bundle {
        val core = File(root, "$name.so").apply { writeText("$name engine") }
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            listOf("GC/font.bin", "GameSettings/test.ini").forEach { path ->
                zip.putNextEntry(ZipEntry(path))
                zip.write("$name Sys".toByteArray())
                zip.closeEntry()
            }
        }
        return Bundle(revision, core, bytes.toByteArray())
    }

    private inline fun fixture(run: (File, VersionedCoreStore, Bundle, Bundle) -> Unit) {
        val root = Files.createTempDirectory("dolphin-versions").toFile()
        try {
            run(
                root,
                VersionedCoreStore(File(root, "cores")),
                bundle(root, "old", "a".repeat(40)),
                bundle(root, "new", "b".repeat(40)),
            )
        } finally {
            root.deleteRecursively()
        }
    }

    private fun stage(
        bundle: Bundle,
        selected: File,
        root: File,
        archive: () -> java.io.InputStream = {
            bundle.archive.inputStream()
        },
    ): File =
        VersionedDolphinAssets.installAndSelect(
            bundle.manifest(),
            bundle.core,
            selected,
            archive,
            File(root, "system"),
            bundle.revision,
        )

    @Test fun oldNewAndRollbackSelectTheirOwnSysAndKeepMemoryCard() =
        fixture { root, store, old, new ->
            val card =
                File(root, "saves/native/gc/User/GC/MemoryCardA.raw").apply {
                    parentFile.mkdirs()
                    writeText("progress")
                }
            store.install("dolphin", old.version, old.core, old.sha)
            assertTrue(store.activate("dolphin", old.version))
            val oldDirectory = stage(old, store.select("dolphin")!!.coreFile, root)
            store.install("dolphin", new.version, new.core, new.sha)
            assertTrue(store.activate("dolphin", new.version))
            val newDirectory = stage(new, store.select("dolphin")!!.coreFile, root)
            assertNotEquals(oldDirectory, newDirectory)
            assertTrue(store.rollback("dolphin"))
            val selected = store.select("dolphin")!!.coreFile
            val restored =
                stage(new, selected, root) { error("Rollback must use retained Sys, never the new APK archive") }
            assertEquals(oldDirectory, restored)
            assertEquals("old Sys", File(restored, "dolphin-emu/Sys/GC/font.bin").readText())
            assertEquals("new Sys", File(newDirectory, "dolphin-emu/Sys/GC/font.bin").readText())
            assertEquals("progress", card.readText())
        }

    @Test fun missingOldAssetsFailClosedWithoutUsingCurrentSys() =
        fixture { root, store, old, new ->
            val oldCore = store.install("dolphin", old.version, old.core, old.sha)
            assertThrows(IllegalStateException::class.java) { stage(new, oldCore, root) }
            assertFalse(File(root, "system/engine-versions/${old.version}").exists())
        }

    @Test fun damagedRetainedSysCannotBeUsedForRollback() =
        fixture { root, store, old, new ->
            val oldCore = store.install("dolphin", old.version, old.core, old.sha)
            val directory = stage(old, oldCore, root)
            File(directory, "dolphin-emu/Sys/GC/font.bin").writeText("damaged")
            assertThrows(IllegalStateException::class.java) { stage(new, oldCore, root) }
        }

    @Test fun selectedCoreMustMatchItsVersionAndAssets() =
        fixture { root, store, old, new ->
            val oldCore = store.install("dolphin", old.version, old.core, old.sha)
            stage(old, oldCore, root)
            oldCore.writeBytes(new.core.readBytes())
            assertThrows(IllegalStateException::class.java) { stage(new, oldCore, root) }
        }
}
