package com.swordfish.lemuroid.lib.library.pc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.util.UUID

class PcWindowsSetupTest {
    @Test
    fun startupRecipeUsesGuestWorkingDirectoryAndRejectsCommandInjection() {
        val recipe = PcWindowsLaunchRecipe("c:\\magic\\magic.exe")
        assertEquals(
            "@ECHO OFF\r\nC:\r\nCD \\MAGIC\r\nIF EXIST MAGIC.EXE MAGIC.EXE\r\n",
            recipe.startupBatch().toString(Charsets.US_ASCII),
        )
        for (path in listOf("C:\\..\\X.EXE", "C:\\A&DIR.EXE", "Z:\\X.EXE", "C:\\A.BAT")) {
            assertTrue(runCatching { PcWindowsLaunchRecipe(path) }.isFailure)
        }
    }

    @Test
    fun preparedGuestImportNeverOverwritesOrTouchesAnotherWorkspace() {
        val root = Files.createTempDirectory("pc-guest").toFile()
        try {
            val profile = UUID.randomUUID()
            val workspace = PcWorkspace(root, profile, UUID.randomUUID())
            val other = PcWorkspace(root, profile, UUID.randomUUID())
            val bytes = ByteArray(7 * 1024 * 1024).apply {
                this[510] = 0x55
                this[511] = 0xaa.toByte()
            }
            val store = PcGuestDiskStore(workspace)
            val result = store.importPrepared(bytes.inputStream())
            assertEquals(listOf(result), store.disks())
            assertTrue(runCatching { store.importPrepared(bytes.inputStream()) }.isFailure)
            assertTrue(PcGuestDiskStore(other).disks().isEmpty())
            assertEquals(bytes.size.toLong(), result.length())
        } finally {
            root.deleteRecursively()
        }
    }
}
