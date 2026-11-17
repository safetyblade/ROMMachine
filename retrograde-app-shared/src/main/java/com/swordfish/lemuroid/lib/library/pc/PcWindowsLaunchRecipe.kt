package com.swordfish.lemuroid.lib.library.pc

import java.util.Locale

/**
 * Guest-only launcher. The batch is copied to the Windows Startup folder by
 * the player after installation. No host command shell or Windows registry edit.
 */
class PcWindowsLaunchRecipe(path: String) {
    val executable: String = path.trim().uppercase(Locale.ROOT)

    init {
        // 8.3 components avoid ambiguous Win95 command.com quoting and metacharacters.
        require(executable.length in 7..120 && VALID.matches(executable)) {
            "Use a C: or D: DOS short path ending in .EXE"
        }
    }

    fun startupBatch(): ByteArray {
        val directory = executable.substringBeforeLast('\\')
        val drive = executable.substring(0, 2)
        val file = executable.substringAfterLast('\\')
        val location = directory.removePrefix(drive).ifEmpty { "\\" }
        return listOf("@ECHO OFF", drive, "CD $location", "IF EXIST $file $file", "")
            .joinToString("\r\n").toByteArray(Charsets.US_ASCII)
    }

    companion object {
        private val VALID = Regex("""[CD]:\\(?:[A-Z0-9_~!-]{1,8}(?:\.[A-Z0-9_~!-]{1,3})?\\)*[A-Z0-9_~!-]{1,8}\.EXE""")
    }
}
