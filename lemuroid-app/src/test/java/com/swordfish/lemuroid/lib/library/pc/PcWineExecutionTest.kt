package com.swordfish.lemuroid.lib.library.pc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.util.UUID

class PcWineExecutionTest {
    private fun pe(machine: Int): ByteArray =
        ByteBuffer.allocate(128).order(ByteOrder.LITTLE_ENDIAN).apply {
            putShort(0, 0x5a4d.toShort())
            putInt(60, 64)
            putInt(64, 0x00004550)
            putShort(68, machine.toShort())
        }.array()

    @Test
    fun peProbeDistinguishesSupportedArchitecturesWithoutExecuting() {
        val root = Files.createTempDirectory("wine-test").toFile()
        try {
            val file = File(root, "GAME.EXE")
            file.writeBytes(pe(0x014c))
            assertEquals(PcWineExecution.Architecture.X86, PcWineExecution.inspectExecutable(file))
            file.writeBytes(pe(0x8664))
            assertEquals(PcWineExecution.Architecture.X64, PcWineExecution.inspectExecutable(file))
            file.writeBytes(pe(0xaa64))
            assertTrue(runCatching { PcWineExecution.inspectExecutable(file) }.isFailure)
            file.writeBytes(ByteArray(64))
            assertTrue(runCatching { PcWineExecution.inspectExecutable(file) }.isFailure)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun absentVerifiedRuntimeNeverStartsAProcess() {
        val root = Files.createTempDirectory("wine-test").toFile()
        try {
            val workspace = PcWorkspace(root, UUID.randomUUID(), UUID.randomUUID())
            val runtime = PcWineExecution.Runtime(File(root, "native"), File(root, "rootfs"), emptyMap())
            val failure =
                runCatching { runtime.command(workspace, File(root, "GAME.EXE"), ":0", File(root, "missing")) }
            assertTrue(failure.isFailure)
        } finally {
            root.deleteRecursively()
        }
    }
}
