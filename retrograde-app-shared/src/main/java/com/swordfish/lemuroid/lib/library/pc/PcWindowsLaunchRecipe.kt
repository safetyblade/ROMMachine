package com.swordfish.lemuroid.lib.library.pc

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale

/**
 * Guest-only Windows Shell Link (MS-SHLLINK). No DOS batch, registry edit,
 * host command shell or executable discovery through untrusted paths.
 */
class PcWindowsLaunchRecipe(path: String) {
    val executable: String = path.trim().uppercase(Locale.ROOT)

    init {
        require(executable.length in 7..240 && VALID.matches(executable)) {
            "Use a C: or D: Windows path ending in .EXE"
        }
        require(
            executable.substring(3).split('\\').all {
                it != "." && it != ".." && !it.endsWith('.') && !it.endsWith(' ')
            },
        )
    }

    fun startupShortcut(): ByteArray {
        val directory = executable.substringBeforeLast('\\').let { if (it.length == 2) "$it\\" else it }
        val target = executable.toByteArray(Charsets.US_ASCII)
        val working = directory.toByteArray(Charsets.US_ASCII)
        val volumeSize = 17
        val baseOffset = 28 + volumeSize
        val suffixOffset = baseOffset + target.size + 1
        val infoSize = suffixOffset + 1
        val link = ByteBuffer.allocate(76 + infoSize + 2 + working.size + 4).order(ByteOrder.LITTLE_ENDIAN)

        // ShellLinkHeader: HasLinkInfo + HasWorkingDir, normal show command.
        link.putInt(76)
        link.put(CLSID)
        link.putInt(0x12)
        link.putInt(0x20)
        repeat(3) { link.putLong(0) }
        link.putInt(0)
        link.putInt(0)
        link.putInt(1)
        link.putShort(0)
        link.putShort(0)
        link.putInt(0)
        link.putInt(0)

        // ANSI LinkInfo is deliberate for Windows 95 shell compatibility.
        link.putInt(infoSize)
        link.putInt(28)
        link.putInt(1)
        link.putInt(28)
        link.putInt(baseOffset)
        link.putInt(0)
        link.putInt(suffixOffset)
        link.putInt(volumeSize)
        link.putInt(3)
        link.putInt(0)
        link.putInt(16)
        link.put(0)
        link.put(target)
        link.put(0)
        link.put(0)
        link.putShort(working.size.toShort())
        link.put(working)
        link.putInt(0)
        check(!link.hasRemaining())
        return link.array()
    }

    companion object {
        private val VALID = Regex("""[CD]:\\(?:[A-Z0-9 _~().-]+\\)*[A-Z0-9 _~().-]+\.EXE""")
        private val CLSID =
            byteArrayOf(1, 0x14, 2, 0, 0, 0, 0, 0, 0xc0.toByte(), 0, 0, 0, 0, 0, 0, 0x46)
    }
}
