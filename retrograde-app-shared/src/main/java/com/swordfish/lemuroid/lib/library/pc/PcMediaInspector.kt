package com.swordfish.lemuroid.lib.library.pc

import java.io.RandomAccessFile
import java.io.File

/** Only for explicit PC import; optical signatures do not establish platform identity. */
object PcMediaInspector {
    enum class Kind { ISO9660, RAW_CD, DISK_IMAGE, UNSUPPORTED }
    fun inspect(file: File): Kind {
        if (!file.isFile) return Kind.UNSUPPORTED
        return RandomAccessFile(file, "r").use { input ->
            fun matches(offset: Long, expected: ByteArray): Boolean {
                if (input.length() < offset + expected.size) return false
                input.seek(offset)
                val actual = ByteArray(expected.size)
                input.readFully(actual)
                return actual.contentEquals(expected)
            }
            val cd = "CD001".toByteArray(Charsets.US_ASCII)
            when (file.extension.lowercase(java.util.Locale.ROOT)) {
                "iso" -> if (matches(32769, cd)) Kind.ISO9660 else Kind.UNSUPPORTED
                "bin" -> if (matches(16L * 2352 + 17, cd) || matches(16L * 2352 + 25, cd)) Kind.RAW_CD else Kind.UNSUPPORTED
                "img" -> if (input.length() >= 512 && input.length() % 512 == 0L &&
                    matches(510, byteArrayOf(0x55, 0xaa.toByte()))) Kind.DISK_IMAGE else Kind.UNSUPPORTED
                else -> Kind.UNSUPPORTED
            }
        }
    }
}
