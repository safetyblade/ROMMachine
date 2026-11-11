package com.swordfish.lemuroid.app.wii

import com.swordfish.lemuroid.lib.storage.scanner.NextGenDiscIdentity
import com.swordfish.lemuroid.lib.storage.scanner.WiiDiscIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.security.MessageDigest

class WiiDiscIdentityTest {
    private fun disc(wii: Boolean) =
        ByteArray(256).apply {
            val offset = if (wii) 24 else 28
            ByteBuffer.wrap(
                this,
            ).order(ByteOrder.BIG_ENDIAN).putInt(offset, if (wii) 0x5d1c9ea3 else 0xc2339f3d.toInt())
        }

    private fun probe(
        name: String,
        bytes: ByteArray,
    ): NextGenDiscIdentity.Platform? {
        val file = Files.createTempFile("wii-disc", ".bin").toFile()
        return try {
            file.writeBytes(bytes)
            RandomAccessFile(file, "r").use { WiiDiscIdentity.probe(name, it.channel).platform }
        } finally {
            file.delete()
        }
    }

    @Test fun rawNintendoDiscsRemainDistinct() {
        assertEquals(NextGenDiscIdentity.Platform.WII, probe("game.iso", disc(true)))
        assertEquals(NextGenDiscIdentity.Platform.GAMECUBE, probe("game.iso", disc(false)))
        assertNull(probe("game.iso", ByteArray(256)))
        assertNull(probe("game.wad", disc(true)))
    }

    @Test fun wbfsRequiresMappedRealWiiHeader() {
        val bytes = ByteArray(65536)
        val header = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        header.putInt(0, 0x57424653)
        header.putInt(4, 128)
        bytes[8] = 9
        bytes[9] = 15
        bytes[12] = 1
        disc(true).copyInto(bytes, 512)
        bytes[769] = 1
        disc(true).copyInto(bytes, 32768)
        assertEquals(NextGenDiscIdentity.Platform.WII, probe("game.wbfs", bytes))
        bytes[32768 + 24] = 0
        assertNull(probe("game.wbfs", bytes))
        assertNull(probe("game.wbfs", bytes.copyOf(1024)))
    }

    private fun rvz(wii: Boolean): ByteArray {
        val second = ByteArray(0xd5)
        val fields = ByteBuffer.wrap(second).order(ByteOrder.BIG_ENDIAN)
        fields.putInt(0, if (wii) 2 else 1)
        fields.putInt(4, 5)
        fields.putInt(12, 0x20000)
        disc(wii).copyOf(0x80).copyInto(second, 16)
        val first = ByteArray(0x48)
        val header = ByteBuffer.wrap(first).order(ByteOrder.BIG_ENDIAN)
        header.putInt(0, 0x52565a01)
        header.putInt(4, 0x01000000)
        header.putInt(8, 0x00030000)
        header.putInt(12, second.size)
        MessageDigest.getInstance("SHA-1").digest(second).copyInto(first, 16)
        header.putLong(36, 0x100000)
        header.putLong(44, (first.size + second.size).toLong())
        MessageDigest.getInstance("SHA-1").digest(first.copyOf(0x34)).copyInto(first, 0x34)
        return first + second
    }

    @Test fun rvzChecksTypeAndBothHeaderHashes() {
        assertEquals(NextGenDiscIdentity.Platform.WII, probe("game.rvz", rvz(true)))
        assertEquals(NextGenDiscIdentity.Platform.GAMECUBE, probe("game.rvz", rvz(false)))
        val corrupt = rvz(true).apply { this[lastIndex] = 1 }
        assertNull(probe("game.rvz", corrupt))
        assertNull(probe("game.rvz", ByteArray(512)))
    }
}
