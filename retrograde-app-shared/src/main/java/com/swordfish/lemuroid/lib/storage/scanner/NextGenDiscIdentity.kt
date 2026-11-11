package com.swordfish.lemuroid.lib.storage.scanner

import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Safe, read-only next-gen ROM identity building block.
 *
 * Do not register unsupported systems from an extension alone. Callers own
 * channel lifetime and may use Android SAF ParcelFileDescriptor channels.
 * No game record or save directory is written by this class.
 */
object NextGenDiscIdentity {
    enum class Platform {
        DREAMCAST,
        GAMECUBE,
        PS2,
        WII,
    }

    data class Result(
        val platform: Platform?,
        val source: String,
        val companionTracks: List<String> = emptyList(),
    )

    private val gameCubeMagic = byteArrayOf(0xc2.toByte(), 0x33, 0x9f.toByte(), 0x3d)
    private val wiiMagic = byteArrayOf(0x5d, 0x1c, 0x9e.toByte(), 0xa3.toByte())
    private val descriptorLine =
        Regex("""^\s*(\d+)\s+(\d+)\s+([04])\s+(2048|2336|2352)\s+("[^"]+"|[^\s]+)\s+(\d+)\s*$""")
    private val ps2Boot =
        Regex("""(?im)^\s*BOOT2\s*=\s*cdrom0:\\[A-Z0-9_.\\-]+(?:;\d+)?\s*$""")

    fun probe(
        name: String,
        header: ByteArray = byteArrayOf(),
        gdiText: String? = null,
        availableTrackNames: Set<String>? = null,
    ): Result {
        val ext = name.substringAfterLast('.', "").lowercase(Locale.US)
        return when (ext) {
            "iso", "gcm" -> {
                when {
                    matches(header, 0x18, wiiMagic) -> Result(Platform.WII, "wii-magic")
                    matches(header, 0x1c, gameCubeMagic) -> Result(Platform.GAMECUBE, "gc-magic")
                    else -> Result(null, "unknown-iso")
                }
            }
            "gdi" -> {
                val tracks = gdiText?.let(::parseGdi)
                when {
                    tracks == null -> Result(null, "invalid-or-unreadable-gdi")
                    availableTrackNames != null && !availableTrackNames.containsAll(tracks) ->
                        Result(null, "missing-gdi-tracks")
                    else -> Result(Platform.DREAMCAST, "gdi-manifest", tracks)
                }
            }
            else -> Result(null, "shared-or-unsupported-container")
        }
    }

    /**
     * Conservative ISO9660 PS2 recognition via SYSTEM.CNF BOOT2.
     * Reads bounded descriptor, root directory and config ranges rather than
     * scanning gigabytes or inferring an ISO's system from its filename.
     */
    fun probePs2Iso(
        name: String,
        source: SeekableByteChannel,
    ): Result {
        if (!name.endsWith(".iso", ignoreCase = true)) return Result(null, "not-raw-iso")
        val header = readAt(source, 0, 32) ?: return Result(null, "unreadable-iso")
        val nintendo = probe(name, header)
        if (nintendo.platform != null) return nintendo

        val pvd = readAt(source, 16L * 2048, 2048) ?: return Result(null, "no-primary-volume")
        val cd001 = byteArrayOf(0x43, 0x44, 0x30, 0x30, 0x31)
        if (pvd[0] != 1.toByte() || !matches(pvd, 1, cd001) || pvd[6] != 1.toByte()) {
            return Result(null, "not-iso9660")
        }
        val root = extent(pvd, 156) ?: return Result(null, "invalid-root-record")
        val rootBytes =
            readAt(source, root.first * 2048L, root.second.coerceAtMost(65536))
                ?: return Result(null, "unreadable-root")

        var index = 0
        while (index + 34 <= rootBytes.size) {
            val length = rootBytes[index].toInt() and 0xff
            if (length == 0) {
                index = ((index / 2048) + 1) * 2048
                continue
            }
            if (length < 34 || index + length > rootBytes.size) break
            val nameSize = rootBytes[index + 32].toInt() and 0xff
            if (nameSize > 0 && 33 + nameSize <= length) {
                val filename =
                    String(rootBytes, index + 33, nameSize, StandardCharsets.US_ASCII)
                        .uppercase(Locale.US)
                if (filename == "SYSTEM.CNF;1" || filename == "SYSTEM.CNF") {
                    val config = extent(rootBytes, index)
                    if (config != null && config.second in 1..16384) {
                        val configBytes = readAt(source, config.first * 2048L, config.second)
                        val contents = configBytes?.let { String(it, StandardCharsets.US_ASCII) }
                        if (contents != null && ps2Boot.containsMatchIn(contents)) {
                            return Result(Platform.PS2, "iso9660-system-cnf-boot2")
                        }
                    }
                }
            }
            index += length
        }
        return Result(null, "no-confirmed-ps2-boot2")
    }

    /** CHD v5 GD-ROM track metadata, never the shared .chd suffix or filename. */
    fun probeDreamcastChd(source: SeekableByteChannel): Result {
        val header = readAt(source, 0, 124) ?: return Result(null, "short-chd")
        val bytes = ByteBuffer.wrap(header).order(java.nio.ByteOrder.BIG_ENDIAN)
        if (String(header, 0, 8, StandardCharsets.US_ASCII) != "MComprHD" ||
            bytes.getInt(8) != 124 || bytes.getInt(12) != 5
        ) {
            return Result(null, "unsupported-chd-header")
        }
        var offset = bytes.getLong(48)
        val visited = mutableSetOf<Long>()
        repeat(256) {
            if (offset == 0L || !visited.add(offset)) return Result(null, "no-gdrom-metadata")
            val record = readAt(source, offset, 16) ?: return Result(null, "invalid-chd-metadata")
            val meta = ByteBuffer.wrap(record).order(java.nio.ByteOrder.BIG_ENDIAN)
            val tag = String(record, 0, 4, StandardCharsets.US_ASCII)
            val length = meta.getInt(4) and 0x00ffffff
            if (tag == "CHGD" && length in 1..4096) {
                val value = readAt(source, offset + 16, length) ?: return Result(null, "truncated-chd-metadata")
                val track = String(value, StandardCharsets.US_ASCII).trimEnd('\u0000')
                if (Regex(
                        "TRACK:[1-9][0-9]* TYPE:[A-Z0-9_/]+ SUBTYPE:[A-Z0-9_/]+ " +
                            "FRAMES:[1-9][0-9]* PAD:[0-9]+ PREGAP:[0-9]+ PGTYPE:[A-Z0-9_/]+ " +
                            "PGSUB:[A-Z0-9_/]+ POSTGAP:[0-9]+",
                    ).matches(track)
                ) {
                    return Result(Platform.DREAMCAST, "chd-v5-gdrom-track")
                }
                return Result(null, "invalid-gdrom-track")
            }
            offset = meta.getLong(8)
        }
        return Result(null, "chd-metadata-limit")
    }

    private fun matches(
        bytes: ByteArray,
        offset: Int,
        magic: ByteArray,
    ): Boolean =
        bytes.size >= offset + magic.size &&
            magic.indices.all { bytes[offset + it] == magic[it] }

    private fun parseGdi(text: String): List<String>? {
        val lines = text.lines().map(String::trim).filter(String::isNotEmpty)
        val count = lines.firstOrNull()?.toIntOrNull() ?: return null
        if (count !in 1..99 || lines.size != count + 1) return null
        var previousLba = -1L
        val names = mutableListOf<String>()
        for ((index, line) in lines.drop(1).withIndex()) {
            val match = descriptorLine.matchEntire(line) ?: return null
            val trackNumber = match.groupValues[1].toIntOrNull() ?: return null
            val lba = match.groupValues[2].toLongOrNull() ?: return null
            val fileOffset = match.groupValues[6].toLongOrNull() ?: return null
            val filename = match.groupValues[5].removeSurrounding("\"")
            if (trackNumber != index + 1 || lba < previousLba || fileOffset < 0L) return null
            if (filename.isBlank() || filename == "." || filename == ".." ||
                filename.contains('/') || filename.contains('\\') || filename.contains(':')
            ) {
                return null
            }
            names += filename
            previousLba = lba
        }
        return names.distinct()
    }

    private fun extent(
        bytes: ByteArray,
        index: Int,
    ): Pair<Long, Int>? {
        if (index + 14 > bytes.size) return null
        val extent = unsigned32(bytes, index + 2)
        val length = unsigned32(bytes, index + 10)
        if (extent < 0L || length < 1L || length > Int.MAX_VALUE) return null
        return extent to length.toInt()
    }

    private fun unsigned32(
        bytes: ByteArray,
        index: Int,
    ): Long {
        var number = 0L
        for (offset in 0..3) {
            number = number or ((bytes[index + offset].toLong() and 0xffL) shl (offset * 8))
        }
        return number
    }

    private fun readAt(
        source: SeekableByteChannel,
        offset: Long,
        count: Int,
    ): ByteArray? {
        if (offset < 0 || count !in 1..65536 || offset > source.size() - count) return null
        val target = ByteBuffer.allocate(count)
        source.position(offset)
        while (target.hasRemaining()) {
            val read = source.read(target)
            if (read <= 0) return null
        }
        return target.array()
    }
}
