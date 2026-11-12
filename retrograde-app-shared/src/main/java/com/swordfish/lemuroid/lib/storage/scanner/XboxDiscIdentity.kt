package com.swordfish.lemuroid.lib.storage.scanner

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SeekableByteChannel
import java.nio.charset.StandardCharsets

/** Read-only, bounded XDVDFS/XBE identification. A suffix is never evidence of Xbox content. */
object XboxDiscIdentity {
    data class Identity(val titleId: String, val title: String, val region: Long, val partitionOffset: Long)

    private const val SECTOR = 2048L
    private val magic = "MICROSOFT*XBOX*MEDIA".toByteArray(StandardCharsets.US_ASCII)

    fun probe(
        name: String,
        channel: SeekableByteChannel,
    ): Identity? {
        if (name.substringAfterLast('.', "").lowercase() !in setOf("iso", "xiso")) return null
        // Extracted XISO plus the three data-partition offsets supported by pinned hakuX.
        for (partition in listOf(0L, 0x18300000L, 0xFD90000L, 0x2080000L)) {
            val volume = read(channel, partition + 32 * SECTOR, 2048) ?: continue
            if (!volume.copyOfRange(0, magic.size).contentEquals(magic) ||
                !volume.copyOfRange(2028, 2028 + magic.size).contentEquals(magic)
            ) {
                continue
            }
            val rootSector = u32(volume, 20)
            val rootSize = u32(volume, 24)
            if (rootSector == 0L || rootSize !in 14L..(4 * 1024 * 1024L)) continue
            val directory = read(channel, partition + rootSector * SECTOR, rootSize.toInt()) ?: continue
            val executable = findExecutable(directory) ?: continue
            val header = read(channel, partition + executable.first * SECTOR, 0x120) ?: continue
            if (String(header, 0, 4, StandardCharsets.US_ASCII) != "XBEH") continue
            val base = u32(header, 0x104)
            val certificate = u32(header, 0x118) - base
            if (certificate < 0 || certificate + 0xa4 > executable.second) continue
            val cert = read(channel, partition + executable.first * SECTOR + certificate, 0xa4) ?: continue
            if (u32(cert, 0) !in 0xa4L..4096L) continue
            val title = String(cert, 12, 80, StandardCharsets.UTF_16LE).substringBefore('\u0000').trim()
            val titleId = "%08X".format(u32(cert, 8))
            return Identity(titleId, title, u32(cert, 0xa0), partition)
        }
        return null
    }

    private fun findExecutable(directory: ByteArray): Pair<Long, Long>? {
        val pending = ArrayDeque<Int>()
        val seen = mutableSetOf<Int>()
        pending.add(0)
        while (pending.isNotEmpty() && seen.size < 32768) {
            val offset = pending.removeLast()
            if (!seen.add(offset)) continue
            if (offset < 0 || offset + 14 > directory.size) return null
            val b = ByteBuffer.wrap(directory).order(ByteOrder.LITTLE_ENDIAN)
            val length = directory[offset + 13].toInt() and 255
            if (length == 0 || offset + 14 + length > directory.size) return null
            for (child in listOf(b.getShort(offset).toInt() and 65535, b.getShort(offset + 2).toInt() and 65535)) {
                if (child != 0) pending.add(child * 4)
            }
            val name = String(directory, offset + 14, length, StandardCharsets.US_ASCII)
            val attributes = directory[offset + 12].toInt() and 255
            if (name.equals("default.xbe", true) && attributes and 0x10 == 0) {
                val sector = u32(directory, offset + 4)
                val size = u32(directory, offset + 8)
                if (sector != 0L && size >= 0x120) return sector to size
            }
        }
        return null
    }

    private fun u32(
        bytes: ByteArray,
        offset: Int,
    ): Long = ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xffffffffL

    private fun read(
        channel: SeekableByteChannel,
        offset: Long,
        count: Int,
    ): ByteArray? {
        if (offset < 0 || count < 0 || offset > channel.size() - count) return null
        channel.position(offset)
        val buffer = ByteBuffer.allocate(count)
        while (buffer.hasRemaining()) if (channel.read(buffer) <= 0) return null
        return buffer.array()
    }
}
