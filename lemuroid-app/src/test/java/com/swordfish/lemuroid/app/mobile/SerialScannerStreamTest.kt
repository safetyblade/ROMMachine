package com.swordfish.lemuroid.app.mobile

import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.storage.scanner.SerialScanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.FilterInputStream

class SerialScannerStreamTest {
    @Test fun emptyAndTruncatedDiscFilesDoNotAbortTheShelfRefresh() {
        for (size in listOf(0, 1, 15, 16, 100, 32768)) {
            for (extension in listOf("bin", "iso", "BIN", "ISO", "pbp")) {
                val result = SerialScanner.extractInfo("incomplete.$extension", ByteArrayInputStream(ByteArray(size)))
                assertNull(result.serial)
            }
        }
    }

    @Test fun contentSignaturesAndSerialsSurviveShortSafReadsAndZeroAvailable() {
        val bytes = ByteArray(65536)
        "PLAYSTATION".toByteArray().copyInto(bytes, 0x8008)
        "SLUS_123.45".toByteArray().copyInto(bytes, 0x1000)
        val stream =
            object : FilterInputStream(ByteArrayInputStream(bytes)) {
                override fun available() = 0

                override fun read(
                    buffer: ByteArray,
                    offset: Int,
                    length: Int,
                ): Int = super.read(buffer, offset, minOf(length, 7))
            }
        val result = SerialScanner.extractInfo("game.ISO", stream)
        assertEquals(SystemID.PSX, result.systemID)
        assertEquals("SLUS-12345", result.serial)
    }

    @Test fun truncatedSerialAtEndOfAStreamDoesNotCrashOrInventItsSuffix() {
        val bytes = ByteArray(65540)
        "PLAYSTATION".toByteArray().copyInto(bytes, 0x8008)
        "SLUS".toByteArray().copyInto(bytes, bytes.size - 4)
        assertNull(SerialScanner.extractInfo("game.bin", ByteArrayInputStream(bytes)).serial)
    }
}
