package com.swordfish.lemuroid.app.shared.game.xbox

import com.swordfish.lemuroid.lib.storage.scanner.XboxDiscIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files

class XboxDiscIdentityTest {
    @get:Rule val temporary = TemporaryFolder()

    private fun disc(): ByteArray {
        val bytes = ByteArray(100 * 2048)
        val volume = 32 * 2048
        "MICROSOFT*XBOX*MEDIA".toByteArray().copyInto(bytes, volume)
        "MICROSOFT*XBOX*MEDIA".toByteArray().copyInto(bytes, volume + 2028)

        fun u32(
            offset: Int,
            value: Int,
        ) {
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(offset, value)
        }
        u32(volume + 20, 33)
        u32(volume + 24, 2048)
        val directory = 33 * 2048
        u32(directory + 4, 34)
        u32(directory + 8, 4096)
        bytes[directory + 13] = 11
        "default.xbe".toByteArray().copyInto(bytes, directory + 14)
        val xbe = 34 * 2048
        "XBEH".toByteArray().copyInto(bytes, xbe)
        u32(xbe + 0x104, 0x10000)
        u32(xbe + 0x118, 0x10200)
        val cert = xbe + 0x200
        u32(cert, 0x1d0)
        u32(cert + 8, 0x12345678)
        "Homebrew Fixture".toByteArray(Charsets.UTF_16LE).copyInto(bytes, cert + 12)
        u32(cert + 0xa0, 1)
        return bytes
    }

    private fun probe(
        bytes: ByteArray,
        name: String = "game.iso",
    ): XboxDiscIdentity.Identity? {
        val path = temporary.newFile().toPath()
        Files.write(path, bytes)
        return Files.newByteChannel(path).use { XboxDiscIdentity.probe(name, it) }
    }

    @Test fun readsActualXdvdfsAndXbeCertificate() {
        val identified = probe(disc())!!
        assertEquals("12345678", identified.titleId)
        assertEquals("Homebrew Fixture", identified.title)
        assertEquals(1L, identified.region)
        assertEquals(0L, identified.partitionOffset)
    }

    @Test fun genericIsoOrRenamedExtensionIsNeverXbox() {
        assertNull(probe(ByteArray(200000)))
        assertNull(probe(disc(), "game.gcm"))
        assertNull(probe(disc().copyOf(66000)))
    }

    @Test fun requiresBothDiscSignaturesAndDefaultXbe() {
        val broken = disc()
        broken[32 * 2048 + 2028] = 0
        assertNull(probe(broken))
        val wrongExecutable = disc()
        wrongExecutable[34 * 2048] = 0
        assertNull(probe(wrongExecutable))
    }

    @Test fun supportsXisoSuffixUsingSameVerifiedContent() {
        assertEquals("12345678", probe(disc(), "game.xiso")!!.titleId)
    }
}
