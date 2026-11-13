package com.swordfish.lemuroid.app.shared.game.xbox360

import java.io.File

/** Bounded, local-only reports. Never include game paths, content URIs or guest profile identifiers. */
object Xbox360Reports {
    fun safeError(error: Throwable): String =
        (error.message ?: "Xbox 360 launch failed")
            .replace(Regex("content://[^\\s]+"), "[content]")
            .replace(Regex("(?:/[A-Za-z0-9_.%:-]+){2,}"), "[path]").take(512)

    fun read(files: File): String =
        runCatching {
            file(files).takeIf { it.length() <= 16384 }?.readText()
        }.getOrNull() ?: "No Xbox 360 launch report. Content and saves are retained."

    fun write(
        files: File,
        report: String,
    ) {
        val target = file(files)
        check(target.parentFile!!.isDirectory || target.parentFile!!.mkdirs())
        val pending = File(target.parentFile, ".report.pending")
        pending.writeText(report.take(16384))
        check(pending.renameTo(target))
    }

    private fun file(files: File) = File(files, "native/xbox360/logs/last-report.txt")
}
