package com.swordfish.lemuroid.lib.storage.scanner

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SeekableByteChannel

/** Read-only bounded XEX/XDVDFS/STFS identification. Shared ISO suffixes are never console evidence. */
object Xbox360ContentIdentity {
    data class Result(val valid: Boolean, val reason: String)

    val formats = setOf("iso", "xex", "xcp", "zar")

    fun probe(
        name: String,
        channel: SeekableByteChannel,
    ): Result {
        val extension = name.substringAfterLast('.', "").lowercase()
        val header = read(channel, 0, 24)
        if (extension == "xex") {
            return Result(
                isXex(header),
                "Xbox 360 requires a valid XEX1/XEX2 executable and its game folder",
            )
        }
        if (extension == "zar") {
            val footer =
                read(channel, channel.size() - 144, 144)
                    ?: return Result(false, "ZAR archive footer is missing")
            val b = ByteBuffer.wrap(footer).order(ByteOrder.BIG_ENDIAN)
            val valid =
                b.getLong(128) == channel.size() && b.getInt(136) == 0x61bf3a01 &&
                    b.getInt(140) == 0x169f52d6 &&
                    (0 until 6).all { i ->
                        val offset = b.getLong(i * 16)
                        val size = b.getLong(i * 16 + 8)
                        offset >= 0 && size >= 0 && offset <= channel.size() - 144 &&
                            size <= channel.size() - 144 - offset
                    }
            return Result(valid, "ZAR archive requires a valid Xenia ZArchive footer")
        }
        if (extension == "xcp") {
            val packageHeader = read(channel, 0, 0x3AD)
            val magic = packageHeader?.let { String(it, 0, 4, Charsets.US_ASCII) }
            val volume = packageHeader?.let { ByteBuffer.wrap(it, 0x3A9, 4).order(ByteOrder.BIG_ENDIAN).int }
            return Result(
                magic in setOf("CON ", "LIVE", "PIRS") && volume in setOf(0, 1),
                "Xbox 360 package requires a valid STFS/SVOD header; SVOD also requires its sibling .data folder",
            )
        }
        if (extension != "iso") {
            return Result(
                false,
                "Supported Xbox 360 imports: ISO, XEX game folders, ZAR and XCP packages",
            )
        }
        val magic = "MICROSOFT*XBOX*MEDIA".toByteArray(Charsets.US_ASCII)
        for (partition in listOf(0L, 0xFB20L, 0x20600L, 0x2080000L, 0xFD90000L)) {
            val volume = read(channel, partition + 32 * 2048L, 2048) ?: continue
            if (!volume.copyOfRange(0, 20).contentEquals(magic) ||
                !volume.copyOfRange(2028, 2048).contentEquals(magic)
            ) {
                continue
            }
            val rootSize = u32(volume, 24)
            if (rootSize !in 14L..4 * 1024 * 1024L) continue
            val directory = read(channel, partition + u32(volume, 20) * 2048, rootSize.toInt()) ?: continue
            val queue = ArrayDeque<Int>()
            queue.add(0)
            val seen = mutableSetOf<Int>()
            while (queue.isNotEmpty() && seen.size < 32768) {
                val offset = queue.removeLast()
                if (!seen.add(offset)) continue
                if (offset < 0 || offset > directory.size - 14) break
                val length = directory[offset + 13].toInt() and 255
                if (length == 0 || offset + 14 + length > directory.size) break
                for (child in listOf(u16(directory, offset), u16(directory, offset + 2))) {
                    if (child != 0) queue.add(child * 4)
                }
                val entry = String(directory, offset + 14, length, Charsets.US_ASCII)
                if (entry.equals("default.xex", true) && directory[offset + 12].toInt() and 0x10 == 0 &&
                    u32(directory, offset + 8) >= 24
                ) {
                    val xex = read(channel, partition + u32(directory, offset + 4) * 2048, 24)
                    if (isXex(xex)) return Result(true, "Xbox 360 XEX executable verified in disc structure")
                }
            }
        }
        return Result(false, "No Xbox 360 default.xex found; an original Xbox XBE disc is a different console")
    }

    private fun isXex(header: ByteArray?): Boolean {
        if (header == null) return false
        if (String(header, 0, 4, Charsets.US_ASCII) !in setOf("XEX1", "XEX2")) return false
        val count = ByteBuffer.wrap(header, 20, 4).order(ByteOrder.BIG_ENDIAN).int.toLong() and 0xffffffffL
        return count <= 65536
    }

    private fun u32(
        b: ByteArray,
        at: Int,
    ) = ByteBuffer.wrap(b, at, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xffffffffL

    private fun u16(
        b: ByteArray,
        at: Int,
    ) = ByteBuffer.wrap(b, at, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 65535

    private fun read(
        channel: SeekableByteChannel,
        at: Long,
        size: Int,
    ): ByteArray? {
        if (at < 0 || at > channel.size() - size) return null
        channel.position(at)
        val buffer = ByteBuffer.allocate(size)
        while (buffer.hasRemaining()) if (channel.read(buffer) <= 0) return null
        return buffer.array()
    }
}
