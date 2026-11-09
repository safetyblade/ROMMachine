package com.swordfish.lemuroid.app.mobile

import com.swordfish.lemuroid.lib.storage.scanner.NextGenDiscIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption

class NativeDiscIdentityTest {
    private val probe = NextGenDiscIdentity

    @Test
    fun gameCubeMagicDoesNotMisidentifyOtherIsos() {
        val header = ByteArray(32)
        header[0x1c] = 0xc2.toByte()
        header[0x1d] = 0x33
        header[0x1e] = 0x9f.toByte()
        header[0x1f] = 0x3d
        assertEquals(probe.Platform.GAMECUBE, probe.probe("game.gcm", header).platform)
        assertEquals(probe.Platform.GAMECUBE, probe.probe("game.iso", header).platform)
        assertNull(probe.probe("game.chd", header).platform)
        assertNull(probe.probe("game.rvz", header).platform)
        assertNull(probe.probe("game.bin", header).platform)
    }

    @Test
    fun wiiMagicNeverClaimsGameCubeSupport() {
        val header = ByteArray(32)
        header[0x18] = 0x5d
        header[0x19] = 0x1c
        header[0x1a] = 0x9e.toByte()
        header[0x1b] = 0xa3.toByte()
        assertEquals(probe.Platform.WII, probe.probe("wii.iso", header).platform)
    }

    @Test
    fun gdiWithValidatedCompanionsIsDreamcast() {
        val descriptor =
            """
            2
            1 0 0 2352 "track 01.bin" 0
            2 4500 4 2048 track02.bin 0
            """.trimIndent()
        val detected =
            probe.probe(
                "homebrew.gdi",
                gdiText = descriptor,
                availableTrackNames = setOf("track 01.bin", "track02.bin"),
            )
        assertEquals(probe.Platform.DREAMCAST, detected.platform)
        assertEquals(listOf("track 01.bin", "track02.bin"), detected.companionTracks)
        assertNull(
            probe.probe(
                "homebrew.gdi",
                gdiText = descriptor,
                availableTrackNames = setOf("track02.bin"),
            ).platform,
        )
    }

    @Test
    fun hostileOrPartialGdiNeverClaimsSupport() {
        val bad = "1\n1 0 4 2352 ../missing.bin 0"
        assertNull(probe.probe("bad.gdi", gdiText = bad).platform)
        assertNull(probe.probe("bad.gdi", gdiText = "2\n1 0 4 2352 only.bin 0").platform)
        assertNull(probe.probe("bad.gdi", gdiText = null).platform)
    }

    @Test
    fun ps2SystemCnfBoot2IsRequiredNotJustIso9660() {
        val file = Files.createTempFile("cartdeck-ps2", ".iso")
        try {
            val ps2 = makeIso("BOOT2 = cdrom0:\\SLUS_123.45;1\r\n")
            Files.write(file, ps2)
            FileChannel.open(file, StandardOpenOption.READ).use {
                assertEquals(probe.Platform.PS2, probe.probePs2Iso("disc.iso", it).platform)
            }
            val ps1 = makeIso("BOOT = cdrom:\\SLUS_123.45;1\r\n")
            Files.write(file, ps1)
            FileChannel.open(file, StandardOpenOption.READ).use {
                assertNull(probe.probePs2Iso("disc.iso", it).platform)
            }
        } finally {
            Files.deleteIfExists(file)
        }
    }

    @Test
    fun unrelatedAndTruncatedIsoRemainsUnknown() {
        val file = Files.createTempFile("cartdeck-unknown", ".iso")
        try {
            Files.write(file, ByteArray(32768))
            FileChannel.open(file, StandardOpenOption.READ).use {
                assertNull(probe.probePs2Iso("unknown.iso", it).platform)
            }
            assertNull(probe.probe("unknown.iso", ByteArray(32)).platform)
        } finally {
            Files.deleteIfExists(file)
        }
    }

    private fun makeIso(config: String): ByteArray {
        val bytes = ByteArray(128 * 1024)
        val pvd = 16 * 2048
        bytes[pvd] = 1
        "CD001".toByteArray().copyInto(bytes, pvd + 1)
        bytes[pvd + 6] = 1
        makeRecord(bytes, pvd + 156, extentSector = 24, length = 2048, filename = byteArrayOf(0))
        val root = 24 * 2048
        makeRecord(
            bytes,
            root,
            extentSector = 26,
            length = config.toByteArray().size,
            filename = "SYSTEM.CNF;1".toByteArray(),
        )
        config.toByteArray().copyInto(bytes, 26 * 2048)
        return bytes
    }

    private fun makeRecord(
        bytes: ByteArray,
        index: Int,
        extentSector: Int,
        length: Int,
        filename: ByteArray,
    ) {
        bytes[index] = (33 + filename.size).toByte()
        writeInt(bytes, index + 2, extentSector)
        writeInt(bytes, index + 10, length)
        bytes[index + 32] = filename.size.toByte()
        filename.copyInto(bytes, index + 33)
    }

    private fun writeInt(
        bytes: ByteArray,
        index: Int,
        value: Int,
    ) {
        for (i in 0..3) {
            bytes[index + i] = (value ushr (i * 8)).toByte()
        }
    }
}
