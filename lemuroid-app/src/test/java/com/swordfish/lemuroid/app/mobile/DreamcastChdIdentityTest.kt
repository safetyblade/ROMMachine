package com.swordfish.lemuroid.app.mobile

import com.swordfish.lemuroid.lib.storage.scanner.NextGenDiscIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption

class DreamcastChdIdentityTest {
    private val track =
        "TRACK:3 TYPE:MODE1_RAW SUBTYPE:NONE FRAMES:549150 PAD:0 " +
            "PREGAP:0 PGTYPE:MODE1 PGSUB:NONE POSTGAP:0\u0000"

    @Test
    fun gdromTrackMetadataIdentifiesDreamcast() {
        assertEquals(NextGenDiscIdentity.Platform.DREAMCAST, probe(chd("CHGD", track)).platform)
    }

    @Test
    fun sharedCdChdDoesNotBecomeDreamcast() {
        assertNull(probe(chd("CHTR", "TRACK:1 TYPE:MODE1 SUBTYPE:NONE FRAMES:100\u0000")).platform)
        assertNull(probe(chd("CHT2", track)).platform)
    }

    @Test
    fun followsMetadataLinksBeforeGdromRecord() {
        val data = chd("CHGD", track)
        val result = ByteArray(data.size + 16)
        data.copyInto(result, 0, 0, 124)
        data.copyInto(result, 140, 124)
        "TEST".toByteArray().copyInto(result, 124)
        ByteBuffer.wrap(result).order(ByteOrder.BIG_ENDIAN).putLong(132, 140)
        assertEquals(NextGenDiscIdentity.Platform.DREAMCAST, probe(result).platform)
    }

    @Test
    fun malformedOrTruncatedTrackMetadataIsRejected() {
        assertNull(probe(chd("CHGD", track.replace("FRAMES:549150", "FRAMES:0"))).platform)
        val valid = chd("CHGD", track)
        assertNull(probe(valid.copyOf(valid.size - 1)).platform)
        assertNull(probe(valid.copyOf(123)).platform)
    }

    @Test
    fun cyclesAndInvalidOffsetsTerminateWithoutClassification() {
        val cyclic = chd("TEST", "")
        ByteBuffer.wrap(cyclic).order(ByteOrder.BIG_ENDIAN).putLong(132, 124)
        assertNull(probe(cyclic).platform)
        val invalid = chd("CHGD", track)
        ByteBuffer.wrap(invalid).order(ByteOrder.BIG_ENDIAN).putLong(48, Long.MAX_VALUE)
        assertNull(probe(invalid).platform)
        ByteBuffer.wrap(invalid).order(ByteOrder.BIG_ENDIAN).putLong(48, -1)
        assertNull(probe(invalid).platform)
    }

    @Test
    fun unsupportedChdVersionsAndOversizedMetadataAreRejected() {
        val invalid = chd("CHGD", track)
        ByteBuffer.wrap(invalid).order(ByteOrder.BIG_ENDIAN).putInt(12, 4)
        assertNull(probe(invalid).platform)
        ByteBuffer.wrap(invalid).order(ByteOrder.BIG_ENDIAN).putInt(12, 5).putInt(128, 4097)
        assertNull(probe(invalid).platform)
    }

    private fun chd(
        tag: String,
        value: String,
    ): ByteArray {
        val payload = value.toByteArray(Charsets.US_ASCII)
        val data = ByteArray(140 + payload.size)
        "MComprHD".toByteArray().copyInto(data)
        ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
            .putInt(8, 124).putInt(12, 5).putLong(48, 124)
            .putInt(128, payload.size)
        tag.toByteArray().copyInto(data, 124)
        payload.copyInto(data, 140)
        return data
    }

    private fun probe(data: ByteArray): NextGenDiscIdentity.Result {
        val file = Files.createTempFile("cartdeck-chd-probe", ".chd")
        return try {
            Files.write(file, data)
            FileChannel.open(file, StandardOpenOption.READ).use(NextGenDiscIdentity::probeDreamcastChd)
        } finally {
            Files.deleteIfExists(file)
        }
    }
}
