package com.swordfish.lemuroid.lib.library.pc

import java.io.File
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class PcWorkspaceTest {
    @Test fun rejectsExtensionOnlyAndPreservesSource() {
        val root = java.nio.file.Files.createTempDirectory("pc-test").toFile()
        try {
            val original = File(root, "console.iso").apply { writeBytes(ByteArray(40000)) }
            val workspace = PcWorkspace(root, UUID.randomUUID(), UUID.randomUUID())
            assertEquals(PcMediaInspector.Kind.UNSUPPORTED, PcMediaInspector.inspect(original))
            try {
                original.inputStream().use { workspace.importMedia("iso", it) }
                fail("Invalid media accepted")
            } catch (_: IllegalArgumentException) {}
            assertEquals(40000L, original.length())
            assertTrue(workspace.media.listFiles()!!.isEmpty())
        } finally { root.deleteRecursively() }
    }

    @Test fun copiesRecognisedMediaAndSeparatesProfiles() {
        val root = java.nio.file.Files.createTempDirectory("pc-test").toFile()
        try {
            val id = UUID.randomUUID()
            val a = PcWorkspace(root, UUID.randomUUID(), id)
            val b = PcWorkspace(root, UUID.randomUUID(), id)
            val bytes = ByteArray(40000)
            "CD001".toByteArray().copyInto(bytes, 32769)
            val imported = bytes.inputStream().use { a.importMedia("iso", it) }
            assertArrayEquals(bytes, imported.readBytes())
            a.prepareWritableDirectories()
            b.prepareWritableDirectories()
            assertNotEquals(a.progress.canonicalPath, b.progress.canonicalPath)
            assertNotEquals(a.guest.canonicalPath, a.media.canonicalPath)
        } finally { root.deleteRecursively() }
    }
}
