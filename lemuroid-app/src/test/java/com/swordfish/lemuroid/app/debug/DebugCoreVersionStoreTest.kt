package com.swordfish.lemuroid.app.debug

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

class DebugCoreVersionStoreTest {
    private fun sha(data: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(data)
            .joinToString("") { "%02x".format(it) }

    @Test
    fun preserveActiveAndFallbackOnCorruption() {
        val root = Files.createTempDirectory("dc02-test-").toFile()
        try {
            val store = DebugCoreVersionStore(File(root, "store"))
            val first = File(root, "first.so").apply { writeText("stable") }
            val second = File(root, "second.so").apply { writeText("candidate") }
            store.install("flycast", "v1", first, sha(first.readBytes()))
            assertTrue(store.activate("flycast", "v1"))
            store.install("flycast", "v2", second, sha(second.readBytes()))
            assertTrue(store.activate("flycast", "v2"))
            assertEquals("v2", store.select("flycast")?.version)
            val core = store.select("flycast")!!.coreFile
            core.writeText("corrupted")
            val recovered = store.select("flycast")
            assertNotNull(recovered)
            assertEquals("v1", recovered?.version)
            assertTrue(recovered!!.usedFallback)
            assertFalse(store.rollback("flycast"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rollbackSwapsValidVersionWithoutDeletingSaves() {
        val root = Files.createTempDirectory("dc02-saves-").toFile()
        try {
            val save = File(root, "saves/slot1.vmu").apply {
                parentFile!!.mkdirs()
                writeText("unchanged-game-progress")
            }
            val store = DebugCoreVersionStore(File(root, "cores"))
            for (v in listOf("v1", "v2")) {
                val source = File(root, "$v.so").apply { writeText(v) }
                store.install("flycast", v, source, sha(source.readBytes()))
                assertTrue(store.activate("flycast", v))
            }
            assertTrue(store.rollback("flycast"))
            assertEquals("v1", store.select("flycast")?.version)
            assertEquals("unchanged-game-progress", save.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectTraversalAndChecksumMismatch() {
        val root = Files.createTempDirectory("dc02-safety-").toFile()
        try {
            val store = DebugCoreVersionStore(File(root, "cores"))
            val source = File(root, "test.so").apply { writeText("original") }
            assertNull(store.select("flycast"))
            var rejected = false
            try {
                store.install("../flycast", "v1", source, sha(source.readBytes()))
            } catch (_: IllegalArgumentException) {
                rejected = true
            }
            assertTrue(rejected)
            rejected = false
            try {
                store.install("flycast", "v1", source, "0".repeat(64))
            } catch (_: IllegalArgumentException) {
                rejected = true
            }
            assertTrue(rejected)
        } finally {
            root.deleteRecursively()
        }
    }
}
