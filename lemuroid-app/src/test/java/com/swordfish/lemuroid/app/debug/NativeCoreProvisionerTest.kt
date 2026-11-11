package com.swordfish.lemuroid.app.debug

import com.swordfish.lemuroid.lib.core.NativeCoreProvisioner
import com.swordfish.lemuroid.lib.core.VersionedCoreStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

class NativeCoreProvisionerTest {
    private fun sha(file: File) =
        MessageDigest.getInstance("SHA-256")
            .digest(file.readBytes()).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    @Test
    fun corruptUpgradeFallsBackAndIsNotRetriedOnRelaunch() {
        val root = Files.createTempDirectory("native-fallback").toFile()
        try {
            val source = File(root, "source.so").apply { writeText("working engine") }
            val storeRoot = File(root, "versions")
            val savedata =
                File(root, "saves/vmu.bin").apply {
                    parentFile!!.mkdirs()
                    writeText("progress")
                }
            val stable = NativeCoreProvisioner.selectVersion(storeRoot, "flycast", "v1", source, sha(source))
            var failures = 0
            val fallback =
                NativeCoreProvisioner.selectVersion(
                    storeRoot,
                    "flycast",
                    "v2",
                    source,
                    "0".repeat(64),
                ) { failures++ }
            assertEquals(stable, fallback)
            assertEquals(1, failures)
            NativeCoreProvisioner.selectVersion(storeRoot, "flycast", "v2", source, "0".repeat(64)) { failures++ }
            assertEquals(1, failures)
            assertEquals("progress", savedata.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun corruptedInstalledCurrentRetainsVerifiedPreviousEngine() {
        val root = Files.createTempDirectory("native-corrupt").toFile()
        try {
            val source = File(root, "source.so").apply { writeText("v1") }
            val storeRoot = File(root, "versions")
            val stable = NativeCoreProvisioner.selectVersion(storeRoot, "flycast", "v1", source, sha(source))
            source.writeText("v2")
            val sha2 = sha(source)
            val current = NativeCoreProvisioner.selectVersion(storeRoot, "flycast", "v2", source, sha2)
            current.writeText("damaged")
            assertEquals(stable, NativeCoreProvisioner.selectVersion(storeRoot, "flycast", "v2", source, sha2))
            assertTrue(VersionedCoreStore(storeRoot).select("flycast")!!.usedFallback)
            assertFalse(stable.readText().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun manualPreviousRemainsPinnedUntilExplicitCurrentSelection() {
        val root = Files.createTempDirectory("native-pin").toFile()
        try {
            val source = File(root, "source.so").apply { writeText("v1") }
            val storeRoot = File(root, "versions")
            val stable = NativeCoreProvisioner.selectVersion(storeRoot, "flycast", "v1", source, sha(source))
            source.writeText("v2")
            val currentSha = sha(source)
            val current = NativeCoreProvisioner.selectVersion(storeRoot, "flycast", "v2", source, currentSha)
            assertTrue(NativeCoreProvisioner.choosePrevious(storeRoot, "flycast"))
            assertEquals(stable, NativeCoreProvisioner.selectVersion(storeRoot, "flycast", "v2", source, currentSha))
            NativeCoreProvisioner.chooseCurrent(storeRoot, "flycast")
            assertEquals(current, NativeCoreProvisioner.selectVersion(storeRoot, "flycast", "v2", source, currentSha))
        } finally {
            root.deleteRecursively()
        }
    }
}
