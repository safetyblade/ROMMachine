package com.swordfish.lemuroid.app.shared.game

import java.io.ByteArrayOutputStream
import java.io.InputStream

/** Selective decoder for AOSP debuggerd/proto/tombstone.proto. No raw trace is retained. */
object NativeCrashTrace {
    const val MAX_BYTES = 1024 * 1024
    private const val MAX_FRAMES = 24

    private data class Field(val id: Int, val number: Long = 0, val bytes: ByteArray? = null)

    fun read(input: InputStream?): String {
        if (input == null) return "Native trace unavailable from Android. Use the documented ADB capture."
        return runCatching {
            input.use {
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (output.size() <= MAX_BYTES) {
                    val count = it.read(buffer, 0, minOf(buffer.size, MAX_BYTES + 1 - output.size()))
                    if (count <= 0) break
                    output.write(buffer, 0, count)
                }
                if (output.size() > MAX_BYTES) {
                    "Native trace exceeds the bounded limit. Use the documented ADB capture."
                } else {
                    decode(output.toByteArray())
                }
            }
        }.getOrDefault("Native trace could not be read. Use the documented ADB capture.")
    }

    internal fun decode(data: ByteArray): String =
        runCatching {
            require(data.size <= MAX_BYTES)
            val tombstone = fields(data, setOf(1, 6, 10, 16))
            val tid = tombstone.firstOrNull { it.id == 6 }?.number ?: 0
            require(tid > 0)
            val thread =
                tombstone.filter { it.id == 16 }.take(256).firstNotNullOfOrNull { entry ->
                    val map = fields(requireNotNull(entry.bytes), setOf(1, 2))
                    if (map.firstOrNull { it.id == 1 }?.number == tid) map.firstOrNull { it.id == 2 }?.bytes else null
                } ?: error("Faulting thread absent")
            val frames = fields(thread, setOf(4)).filter { it.id == 4 }.take(MAX_FRAMES)
            require(frames.isNotEmpty())
            val signal =
                tombstone.firstOrNull { it.id == 10 }?.bytes?.let { bytes ->
                    fields(bytes, setOf(1)).firstOrNull()?.number
                }
            val header = "Android native trace: signal=${signal ?: "unknown"}; module-relative PCs only"
            header + "\n" +
                frames.mapIndexed { index, field ->
                    val frame = fields(requireNotNull(field.bytes), setOf(1, 4, 5, 6, 8))

                    fun text(id: Int) = frame.firstOrNull { it.id == id }?.bytes?.toString(Charsets.UTF_8).orEmpty()
                    val module = text(6).substringAfterLast('/').substringAfterLast('\\')
                    if (!module.matches(Regex("[A-Za-z0-9_.+-]{1,120}\\.so|linker64|linker"))) {
                        "#${index.toString().padStart(2, '0')} anonymous/JIT frame (no process address exported)"
                    } else {
                        val pc = frame.firstOrNull { it.id == 1 }?.number ?: 0
                        val build = text(8).takeIf { it.matches(Regex("[a-fA-F0-9]{8,128}")) } ?: "unavailable"
                        val symbol =
                            text(4).takeIf { '/' !in it && '\\' !in it }
                                ?.filter { it.code in 32..126 }?.take(180).orEmpty().ifBlank { "unknown" }
                        val offset = frame.firstOrNull { it.id == 5 }?.number ?: 0
                        "#${index.toString().padStart(2, '0')} pc ${java.lang.Long.toUnsignedString(pc, 16)} " +
                            "$module ($symbol+$offset) (BuildId: $build)"
                    }
                }.joinToString("\n")
        }.getOrDefault("Native trace format unsupported or incomplete. Use the documented ADB capture.")

    private fun fields(
        data: ByteArray,
        wanted: Set<Int>,
    ): List<Field> {
        var position = 0
        var count = 0

        fun varint(): Long {
            var value = 0L
            for (shift in 0..63 step 7) {
                require(position < data.size)
                val next = data[position++].toInt() and 255
                require(shift < 63 || next <= 1)
                value = value or ((next and 127).toLong() shl shift)
                if (next < 128) return value
            }
            error("Invalid varint")
        }
        val result = mutableListOf<Field>()
        while (position < data.size) {
            require(++count <= 16384)
            val tag = varint()
            require(tag > 0 && tag <= 0xffffffffL)
            val id = (tag ushr 3).toInt()
            require(id > 0)
            when ((tag and 7).toInt()) {
                0 -> {
                    val value = varint()
                    if (id in wanted) result += Field(id, value)
                }
                1 -> {
                    require(data.size - position >= 8)
                    position += 8
                }
                2 -> {
                    val length = varint()
                    require(length >= 0 && length <= data.size - position)
                    val end = position + length.toInt()
                    if (id in wanted) result += Field(id, bytes = data.copyOfRange(position, end))
                    position = end
                }
                5 -> {
                    require(data.size - position >= 4)
                    position += 4
                }
                else -> error("Unsupported wire type")
            }
        }
        return result
    }
}
