package com.swordfish.lemuroid.lib.storage.scanner

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SeekableByteChannel
import java.security.MessageDigest
import java.util.Locale

/** Bounded content probes matching the pinned Dolphin WBFS/RVZ readers. Never guesses from a suffix. */
object WiiDiscIdentity {
    fun probe(
        name: String,
        source: SeekableByteChannel,
    ): NextGenDiscIdentity.Result {
        return when (name.substringAfterLast('.', "").lowercase(Locale.US)) {
            "iso", "gcm" ->
                read(source, 0, 32)?.let { NextGenDiscIdentity.probe(name, it) }
                    ?: unknown("truncated-disc")
            "wbfs" -> probeWbfs(source)
            "rvz" -> probeRvz(source)
            else -> unknown("unsupported-dolphin-container")
        }
    }

    private fun probeWbfs(source: SeekableByteChannel): NextGenDiscIdentity.Result {
        val header = read(source, 0, 512) ?: return unknown("short-wbfs")
        val bytes = ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN)
        if (bytes.getInt(0) != 0x57424653) return unknown("not-wbfs")
        val diskShift = header[8].toInt() and 255
        val clusterShift = header[9].toInt() and 255
        if (diskShift !in 9..20 || clusterShift !in 15..31 || clusterShift < diskShift) {
            return unknown("invalid-wbfs-sector-shifts")
        }
        val diskSector = 1L shl diskShift
        val clusterSize = 1L shl clusterShift
        val sectors = bytes.getInt(4).toLong() and 0xffffffffL
        // Dolphin's file reader selects slot zero and rejects an incorrect aggregate size.
        // Split .wbf1 images need sibling staging and are deliberately not claimed here.
        if (sectors * diskSector != source.size() || header[12] == 0.toByte()) {
            return unknown("unsupported-or-truncated-wbfs")
        }
        val discInfo = read(source, diskSector, 258) ?: return unknown("missing-wbfs-disc-info")
        val copied = NextGenDiscIdentity.probe("disc.iso", discInfo)
        if (copied.platform != NextGenDiscIdentity.Platform.WII) return unknown("not-wii-wbfs")
        val firstBlock = ((discInfo[256].toInt() and 255) shl 8) or (discInfo[257].toInt() and 255)
        if (firstBlock == 0) return unknown("missing-wbfs-first-block")
        val actual = read(source, firstBlock * clusterSize, 256) ?: return unknown("invalid-wbfs-block-map")
        if (!actual.contentEquals(discInfo.copyOf(256))) return unknown("wbfs-disc-header-mismatch")
        return NextGenDiscIdentity.Result(NextGenDiscIdentity.Platform.WII, "wbfs-mapped-wii-disc")
    }

    private fun probeRvz(source: SeekableByteChannel): NextGenDiscIdentity.Result {
        val first = read(source, 0, 0x48) ?: return unknown("short-rvz")
        val header = ByteBuffer.wrap(first).order(ByteOrder.BIG_ENDIAN)
        if (header.getInt(0) != 0x52565a01) return unknown("not-rvz")
        val version = header.getInt(4).toLong() and 0xffffffffL
        val compatible = header.getInt(8).toLong() and 0xffffffffL
        if (version < 0x00030000L || compatible > 0x01000000L || compatible > version) {
            return unknown("unsupported-rvz-version")
        }
        if (!sha1(first.copyOfRange(0, 0x34)).contentEquals(first.copyOfRange(0x34, 0x48))) {
            return unknown("damaged-rvz-header")
        }
        val size = header.getInt(12)
        if (size !in 0xd5..65536 || header.getLong(44) != source.size() || header.getLong(36) < 0x80) {
            return unknown("invalid-rvz-size")
        }
        val second = read(source, 0x48, size) ?: return unknown("truncated-rvz-disc-header")
        if (!sha1(second).contentEquals(first.copyOfRange(16, 36))) return unknown("damaged-rvz-disc-header")
        val disc = ByteBuffer.wrap(second).order(ByteOrder.BIG_ENDIAN)
        val compression = disc.getInt(4)
        val chunk = disc.getInt(12)
        val powerOfTwo = chunk > 0 && (chunk and (chunk - 1)) == 0
        if (compression !in setOf(0, 2, 3, 4, 5) || chunk < 0x8000 ||
            (!powerOfTwo && chunk % 0x200000 != 0)
        ) {
            return unknown("unsupported-rvz-compression")
        }
        val compressorBytes = second[0xd4].toInt() and 255
        if (compressorBytes > 7 || size < 0xd5 + compressorBytes) return unknown("invalid-rvz-compressor")
        val identified = NextGenDiscIdentity.probe("disc.iso", second.copyOfRange(16, 16 + 0x80))
        val expected =
            when (disc.getInt(0)) {
                1 -> NextGenDiscIdentity.Platform.GAMECUBE
                2 -> NextGenDiscIdentity.Platform.WII
                else -> null
            }
        if (expected == null || identified.platform != expected) return unknown("rvz-disc-type-mismatch")
        return NextGenDiscIdentity.Result(expected, "rvz-verified-disc-header")
    }

    private fun sha1(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-1").digest(bytes)

    private fun unknown(reason: String) = NextGenDiscIdentity.Result(null, reason)

    private fun read(
        source: SeekableByteChannel,
        offset: Long,
        size: Int,
    ): ByteArray? {
        if (offset < 0 || size !in 1..65536 || offset > source.size() - size) return null
        val bytes = ByteBuffer.allocate(size)
        source.position(offset)
        while (bytes.hasRemaining()) if (source.read(bytes) <= 0) return null
        return bytes.array()
    }
}
