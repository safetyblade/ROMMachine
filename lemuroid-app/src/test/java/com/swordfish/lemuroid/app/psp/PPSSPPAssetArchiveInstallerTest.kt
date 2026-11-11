package com.swordfish.lemuroid.app.psp

import com.swordfish.lemuroid.lib.core.assetsmanager.PPSSPPAssetArchiveInstaller
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class PPSSPPAssetArchiveInstallerTest {
    private fun archive(vararg entries: Pair<String, String>): ZipInputStream {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            entries.forEach { (name, contents) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(contents.toByteArray())
                zip.closeEntry()
            }
        }
        return ZipInputStream(ByteArrayInputStream(bytes.toByteArray()))
    }

    @Test
    fun installationPreservesPreviousAssetsForRollback() {
        val parent = Files.createTempDirectory("psp-assets-").toFile()
        try {
            val destination = File(parent, "PPSSPP").apply { mkdirs() }
            File(destination, "compat.ini").writeText("old compatibility rules")
            archive("compat.ini" to "new compatibility rules").use {
                PPSSPPAssetArchiveInstaller.install(it, destination)
            }
            assertEquals("new compatibility rules", File(destination, "compat.ini").readText())
            val rollback = parent.listFiles()!!.single { it.name.startsWith("PPSSPP.rollback-") }
            assertEquals("old compatibility rules", File(rollback, "compat.ini").readText())
        } finally {
            parent.deleteRecursively()
        }
    }

    @Test
    fun unsafeArchiveNeverDeletesExistingAssets() {
        val parent = Files.createTempDirectory("psp-unsafe-").toFile()
        try {
            val destination = File(parent, "PPSSPP").apply { mkdirs() }
            File(destination, "compat.ini").writeText("known working")
            val result =
                runCatching {
                    archive("../outside.txt" to "bad").use {
                        PPSSPPAssetArchiveInstaller.install(it, destination)
                    }
                }
            assertTrue(result.isFailure)
            assertEquals("known working", File(destination, "compat.ini").readText())
            assertFalse(File(parent, "outside.txt").exists())
        } finally {
            parent.deleteRecursively()
        }
    }

    @Test
    fun interruptedSwapRecoversRetainedBaseline() {
        val parent = Files.createTempDirectory("psp-recovery-").toFile()
        try {
            val rollback = File(parent, "PPSSPP.rollback-retained").apply { mkdirs() }
            File(rollback, "compat.ini").writeText("previous")
            val destination = File(parent, "PPSSPP")
            PPSSPPAssetArchiveInstaller.recoverInterruptedInstallation(destination)
            assertEquals("previous", File(destination, "compat.ini").readText())
            assertFalse(rollback.exists())
        } finally {
            parent.deleteRecursively()
        }
    }

    @Test
    fun emptyArchiveCannotReplaceWorkingAssets() {
        val parent = Files.createTempDirectory("psp-empty-").toFile()
        try {
            val destination = File(parent, "PPSSPP").apply { mkdirs() }
            File(destination, "compat.ini").writeText("previous")
            val outcome =
                runCatching {
                    archive().use { PPSSPPAssetArchiveInstaller.install(it, destination) }
                }
            assertTrue(outcome.isFailure)
            assertEquals("previous", File(destination, "compat.ini").readText())
        } finally {
            parent.deleteRecursively()
        }
    }

    @Test
    fun stagedValidationFailureKeepsPreviousAssetsAndVersion() {
        val parent = Files.createTempDirectory("psp-validation-").toFile()
        try {
            val destination = File(parent, "PPSSPP").apply { mkdirs() }
            File(destination, "compat.ini").writeText("previous")
            val result =
                runCatching {
                    archive("font.pgf" to "incomplete bundle").use { zip ->
                        PPSSPPAssetArchiveInstaller.install(zip, destination) { staging ->
                            check(File(staging, "compat.ini").isFile)
                        }
                    }
                }
            assertTrue(result.isFailure)
            assertEquals("previous", File(destination, "compat.ini").readText())
            assertEquals(listOf("PPSSPP"), parent.listFiles()!!.map { it.name })
        } finally {
            parent.deleteRecursively()
        }
    }
}
