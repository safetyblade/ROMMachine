package com.swordfish.lemuroid.app.shared.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class NativeCrashTraceTest {
    private fun number(value: Long): ByteArray {
        var n = value
        val bytes = mutableListOf<Byte>()
        do {
            val b = (n and 127).toInt()
            n = n ushr 7
            bytes += (b or if (n != 0L) 128 else 0).toByte()
        }
        while (n != 0L)
        return bytes.toByteArray()
    }

    private fun v(
        id: Int,
        value: Long,
    ) = number((id * 8).toLong()) + number(value)

    private fun b(
        id: Int,
        value: ByteArray,
    ) = number((id * 8 + 2).toLong()) + number(value.size.toLong()) + value

    private fun s(
        id: Int,
        value: String,
    ) = b(id, value.toByteArray())

    @Test fun exportsOnlyFaultingThreadRelativeFramesAndBuildIds() {
        val frame =
            v(1, 0x1234) + v(2, 0xdeadbeef) + v(3, 0xcafebabe) +
                s(4, "retro_init") + v(5, 16) + s(6, "/private/games/user/libplay.so") + s(8, "1234abcdef")
        val thread = s(2, "private thread name") + b(4, frame) + s(5, "private memory dump")
        val unrelated = v(1, 99) + b(2, s(2, "other thread"))
        val tombstone =
            v(6, 42) + b(10, v(1, 11)) + b(16, unrelated) +
                b(16, v(1, 42) + b(2, thread)) + s(9, "/private/game.iso") + s(18, "private logs")
        val report = NativeCrashTrace.read(ByteArrayInputStream(tombstone))
        assertTrue(report.contains("signal=11"))
        assertTrue(report.contains("pc 1234 libplay.so (retro_init+16) (BuildId: 1234abcdef)"))
        listOf("private", "deadbeef", "cafebabe", "other thread", "game.iso").forEach {
            assertFalse(report, report.contains(it))
        }
    }

    @Test fun malformedOrTruncatedTraceDoesNotExportRawBytes() {
        for (bytes in listOf(
            byteArrayOf(0x32, 0x7f),
            ByteArray(20) { 0xff.toByte() },
            "secret game path".toByteArray(),
        )) {
            val report = NativeCrashTrace.read(ByteArrayInputStream(bytes))
            assertTrue(report.contains("documented ADB capture"))
            assertFalse(report.contains("secret"))
        }
    }

    @Test fun oversizedTraceIsBoundedAndStreamAlwaysCloses() {
        var closed = false
        val input =
            object : ByteArrayInputStream(ByteArray(NativeCrashTrace.MAX_BYTES + 100)) {
                override fun close() {
                    closed = true
                    super.close()
                }
            }
        assertTrue(NativeCrashTrace.read(input).contains("bounded limit"))
        assertTrue(closed)
        assertTrue(input.available() > 0)
    }

    @Test fun absentTraceHasActionableFallback() {
        assertTrue(NativeCrashTrace.read(null).contains("documented ADB capture"))
    }
}
