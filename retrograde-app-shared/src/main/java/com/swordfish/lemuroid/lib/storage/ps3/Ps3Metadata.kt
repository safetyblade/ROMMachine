package com.swordfish.lemuroid.lib.storage.ps3

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SeekableByteChannel

/** Bounded parsers. Filename, folder name and .iso/.pkg suffix are never console identity. */
object Ps3Metadata {
    data class Title(val id: String, val name: String, val category: String, val version: String?)

    fun sfo(bytes: ByteArray): Title? = runCatching {
        require(bytes.size in 20..1048576)
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(b.int == 0x46535000 && b.int == 0x101)
        val keys = b.int
        val values = b.int
        val count = b.int
        require(count in 1..256 && keys >= 20 + count * 16 && values >= keys && values <= bytes.size)
        val fields = mutableMapOf<String, String>()
        repeat(count) { index ->
            b.position(20 + index * 16)
            val key = keys.toLong() + (b.short.toInt() and 65535)
            val format = b.short.toInt() and 65535
            val size = b.int
            val capacity = b.int
            val value = values.toLong() + (b.int.toLong() and 0xffffffffL)
            require(key in keys.toLong() until values.toLong())
            require(size in 0..1048576 && capacity >= size && value >= values && value + capacity <= bytes.size)
            var end = key.toInt()
            while (end < values && bytes[end] != 0.toByte()) end++
            require(end < values)
            val name = bytes.copyOfRange(key.toInt(), end).toString(Charsets.US_ASCII)
            require(!fields.containsKey(name))
            if (format == 0x204) {
                require(size > 0 && bytes[value.toInt() + size - 1] == 0.toByte())
                fields[name] = bytes.copyOfRange(value.toInt(), value.toInt() + size - 1).toString(Charsets.UTF_8)
            }
        }
        val id = fields["TITLE_ID"] ?: error("Missing title identity")
        val category = fields["CATEGORY"] ?: error("Missing category")
        require(Regex("[A-Z]{4}[0-9]{5}").matches(id))
        // PS Vita and PSP also use SFO. Accept only PS3 executable categories.
        require(category in setOf("DG", "HG", "GD", "HM", "CB"))
        Title(id, fields["TITLE"]?.takeIf { it.isNotBlank() } ?: id, category, fields["APP_VER"])
    }.getOrNull()

    fun pkg(header: ByteArray, length: Long): String? = runCatching {
        require(header.size >= 128 && length >= 128)
        val b = ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN)
        require(b.int == 0x7f504b47)
        val revision = b.short.toInt() and 65535
        val platform = b.short.toInt() and 65535
        require(revision in setOf(0, 0x8000) && platform == 1) // PS3, not PSP/Vita PKG
        val total = b.getLong(24)
        val data = b.getLong(32)
        val size = b.getLong(40)
        require(total == length && data >= 128 && size >= 0 && data <= total && size <= total - data)
        val content = header.copyOfRange(48, 96).toString(Charsets.US_ASCII).trimEnd('\u0000')
        require(Regex("[A-Z]{2}[0-9]{4}-[A-Z]{4}[0-9]{5}_[0-9]{2}-[A-Z0-9_-]{1,16}").matches(content))
        content.substring(7, 16)
    }.getOrNull()

    /** A raw ISO needs actual PS3_DISC.SFB and PS3_GAME/PARAM.SFO records. Encrypted/unreadable discs stay unknown. */
    fun iso(source: SeekableByteChannel): Title? = runCatching {
        fun read(offset: Long, size: Int): ByteArray {
            require(size in 1..1048576 && offset >= 0 && offset <= source.size() - size)
            val b = ByteBuffer.allocate(size)
            source.position(offset)
            while (b.hasRemaining()) require(source.read(b) > 0)
            return b.array()
        }
        data class Record(val offset: Long, val size: Int, val directory: Boolean)
        fun extent(b: ByteArray, p: Int): Record {
            require(p >= 0 && p + 34 <= b.size)
            val v = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN)
            val sector = v.getInt(p + 2).toLong() and 0xffffffffL
            val size = v.getInt(p + 10)
            require(size in 1..1048576)
            return Record(sector * 2048, size, (b[p + 25].toInt() and 2) != 0)
        }
        fun find(dir: Record, name: String): Record {
            val b = read(dir.offset, dir.size)
            var p = 0
            while (p + 34 <= b.size) {
                val size = b[p].toInt() and 255
                if (size == 0) { p = (p / 2048 + 1) * 2048; continue }
                require(size >= 34 && p + size <= b.size)
                val n = b[p + 32].toInt() and 255
                require(n + 33 <= size)
                val entry = b.copyOfRange(p + 33, p + 33 + n).toString(Charsets.US_ASCII).removeSuffix(";1")
                if (entry == name) return extent(b, p)
                p += size
            }
            error("Missing PS3 metadata")
        }
        val pvd = read(32768, 2048)
        require(pvd[0] == 1.toByte() && pvd.copyOfRange(1, 6).toString(Charsets.US_ASCII) == "CD001")
        val root = extent(pvd, 156)
        val disc = find(root, "PS3_DISC.SFB")
        require(read(disc.offset, minOf(disc.size, 16)).take(4).toByteArray().contentEquals(byteArrayOf(0x2e, 0x53, 0x46, 0x42)))
        val game = find(root, "PS3_GAME")
        require(game.directory)
        val sfoRecord = find(game, "PARAM.SFO")
        sfo(read(sfoRecord.offset, sfoRecord.size))?.takeIf { it.category == "DG" }
    }.getOrNull()
}
