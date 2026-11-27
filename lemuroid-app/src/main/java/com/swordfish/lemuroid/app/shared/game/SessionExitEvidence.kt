package com.swordfish.lemuroid.app.shared.game

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

/** Exit data is usable only after both the process and the session time boundary are known. */
internal object SessionExitEvidence {
    @RequiresApi(30)
    fun find(
        context: Context,
        process: String,
        pid: Int,
        started: Long,
    ): ApplicationExitInfo? {
        if (pid <= 0 || started <= 0) return null
        return runCatching {
            context.getSystemService(ActivityManager::class.java)
                .getHistoricalProcessExitReasons(context.packageName, pid, 16)
                .filter { matches(it.processName, it.pid, it.timestamp, process, pid, started) }
                .maxByOrNull { it.timestamp }
        }.getOrNull()
    }

    fun matches(
        actualProcess: String,
        actualPid: Int,
        exitTime: Long,
        expectedProcess: String,
        expectedPid: Int,
        started: Long,
    ): Boolean =
        expectedPid > 0 && started > 0 && actualProcess == expectedProcess &&
            actualPid == expectedPid && exitTime >= started

    @RequiresApi(30)
    fun render(exit: ApplicationExitInfo): String =
        buildString {
            appendLine("androidProcessExit=reason=${exit.reason}; status=${exit.status}; timestamp=${exit.timestamp}")
            appendLine("process=${exit.processName}; pid=${exit.pid}")
            when {
                exit.reason == ApplicationExitInfo.REASON_ANR -> {
                    appendLine("ANR stack frames")
                    append(anrFrames(exit) ?: "NOT_CAPTURED")
                }
                Build.VERSION.SDK_INT >= 31 && exit.reason == ApplicationExitInfo.REASON_CRASH_NATIVE -> {
                    append(runCatching { NativeCrashTrace.read(exit.traceInputStream) }.getOrNull() ?: "NOT_CAPTURED")
                }
            }
        }

    private val javaFrame = Regex("""\s+at [\w.$<>]+\([\w.$: -]+\)""")
    private val nativeFrame = Regex("""\s*#\d+ pc [0-9a-fA-F]+ [^\s]+.*""")
    private val traceHeader = Regex("""----- pid (\d+) at .* -----""")

    @RequiresApi(30)
    private fun anrFrames(exit: ApplicationExitInfo): String? =
        runCatching {
            exit.traceInputStream?.use { input ->
                val buffer = ByteArray(96 * 1024)
                var bytes = 0
                while (bytes < buffer.size) {
                    val count = input.read(buffer, bytes, buffer.size - bytes)
                    if (count <= 0) break
                    bytes += count
                }
                selectAnrFrames(buffer.decodeToString(endIndex = bytes), exit.pid)
            }
        }.getOrNull()

    internal fun selectAnrFrames(
        trace: String,
        pid: Int,
    ): String? {
        val matching =
            trace.lineSequence()
                .dropWhile { traceHeader.matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() != pid }
                .drop(1)
                .takeWhile { !it.startsWith("----- ") }
                .filter { it.length <= 350 && (javaFrame.matches(it) || nativeFrame.matches(it)) }
                .take(72)
                .joinToString("\n")
        return matching.ifBlank { null }
    }
}
