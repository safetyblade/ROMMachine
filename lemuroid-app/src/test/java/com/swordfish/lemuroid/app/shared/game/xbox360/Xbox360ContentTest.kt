package com.swordfish.lemuroid.app.shared.game.xbox360

import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.storage.scanner.NativeDiscFolderIdentity
import com.swordfish.lemuroid.lib.storage.scanner.Xbox360ContentIdentity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Xbox360ContentTest {
    private fun probe(
        name: String,
        bytes: ByteArray,
    ): Boolean {
        val file = File.createTempFile("x360-synthetic", ".bin")
        return try {
            file.writeBytes(bytes)
            file.inputStream().channel.use { Xbox360ContentIdentity.probe(name, it).valid }
        } finally {
            file.delete()
        }
    }

    private fun xex() = ByteArray(24).apply { "XEX2".toByteArray().copyInto(this) }

    @Test fun isoRequiresXbox360ExecutableNotSharedSuffixOrOriginalXbox() {
        val disc = ByteArray(40 * 2048)
        val header = ByteBuffer.wrap(disc).order(ByteOrder.LITTLE_ENDIAN)
        "MICROSOFT*XBOX*MEDIA".toByteArray().copyInto(disc, 32 * 2048)
        "MICROSOFT*XBOX*MEDIA".toByteArray().copyInto(disc, 33 * 2048 - 20)
        header.putInt(32 * 2048 + 20, 34)
        header.putInt(32 * 2048 + 24, 64)
        header.putInt(34 * 2048 + 4, 36)
        header.putInt(34 * 2048 + 8, 24)
        disc[34 * 2048 + 13] = 11
        "default.xbe".toByteArray().copyInto(disc, 34 * 2048 + 14)
        "XBEH".toByteArray().copyInto(disc, 36 * 2048)
        assertFalse(probe("original.iso", disc))
        "default.xex".toByteArray().copyInto(disc, 34 * 2048 + 14)
        assertFalse(probe("renamed.iso", disc))
        xex().copyInto(disc, 36 * 2048)
        assertTrue(probe("homebrew.iso", disc))
        header.putInt(32 * 2048 + 24, Int.MAX_VALUE)
        assertFalse(probe("corrupt.iso", disc))
    }

    @Test fun formatsRequireRecognisedSignaturesAndBoundedReads() {
        assertTrue(probe("default.XEX", xex()))
        assertFalse(probe("bad.xex", ByteArray(23)))
        assertFalse(probe("unsupported.m3u", xex()))
        assertFalse(probe("not-a-disc.iso", xex()))
        assertTrue(probe("synthetic.xcp", ByteArray(0x3AD).apply { "LIVE".toByteArray().copyInto(this) }))
        assertFalse(probe("bad.xcp", xex()))
    }

    @Test fun zarFooterMustMatchPinnedVersionAndFileRanges() {
        val data = ByteArray(160)
        val b = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
        b.putLong(16 + 128, 160)
        b.putInt(16 + 136, 0x61bf3a01)
        b.putInt(16 + 140, 0x169f52d6)
        assertTrue(probe("synthetic.zar", data))
        b.putLong(16, Long.MAX_VALUE)
        assertFalse(probe("corrupt.zar", data))
        assertFalse(probe("truncated.zar", ByteArray(143)))
    }

    @Test fun explicitContextNeverOverridesOriginalXboxEvidence() {
        val identity = NativeDiscFolderIdentity
        val ids = SystemID.entries.toSet()
        assertTrue(
            identity.resolve(
                "original.iso", "/ROMs/Xbox 360/original.iso", ids,
                verified = SystemID.XBOX,
            ) == SystemID.XBOX,
        )
        assertTrue(
            identity.infer("game.iso", "/ROMs/Xbox 360/game.iso", ids) ==
                SystemID.XBOX360,
        )
        assertTrue(identity.infer("game.iso", "/ROMs/game.iso", ids) == null)
    }
}
