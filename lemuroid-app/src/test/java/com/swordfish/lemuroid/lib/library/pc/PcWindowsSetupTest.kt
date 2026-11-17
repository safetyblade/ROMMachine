package com.swordfish.lemuroid.lib.library.pc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.util.UUID

class PcWindowsSetupTest {
    @Test
    fun startupRecipeEncodesWindowsTargetAndWorkingDirectory() {
        val recipe = PcWindowsLaunchRecipe("c:\\Program Files\\Magic\\magic.exe")
        val bytes = recipe.startupShortcut()
        val link = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(76, link.getInt(0))
        assertEquals(0x12, link.getInt(20))
        val infoSize = link.getInt(76)
        val targetOffset = 76 + link.getInt(76 + 16)
        val target = bytes.copyOfRange(targetOffset, bytes.indexOfFrom(0, targetOffset))
        assertEquals("C:\\PROGRAM FILES\\MAGIC\\MAGIC.EXE", target.toString(Charsets.US_ASCII))
        val workingOffset = 76 + infoSize
        val length = link.getShort(workingOffset).toInt()
        val working = bytes.copyOfRange(workingOffset + 2, workingOffset + 2 + length)
        assertEquals("C:\\PROGRAM FILES\\MAGIC", working.toString(Charsets.US_ASCII))
        assertEquals(0, link.getInt(bytes.size - 4))
        for (path in listOf("C:\\..\\X.EXE", "C:\\A&DIR.EXE", "Z:\\X.EXE", "C:\\A.BAT")) {
            assertTrue(runCatching { PcWindowsLaunchRecipe(path) }.isFailure)
        }
    }

    private fun ByteArray.indexOfFrom(
        value: Byte,
        start: Int,
    ): Int = (start until size).first { this[it] == value }

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
