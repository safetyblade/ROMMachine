package com.swordfish.lemuroid.app.shared.game

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.util.AtomicFile
import com.swordfish.lemuroid.BuildConfig
import java.io.File
import java.util.Properties

/** Survives an isolated native process dying before it can deliver an Activity result. */
object NativeSessionJournal {
    private fun journal(context: Context) = AtomicFile(File(context.filesDir, "native-session.properties"))

    fun begin(
        context: Context,
        console: String,
        root: File,
    ) {
        val props =
            Properties().apply {
                setProperty("console", console)
                setProperty("pid", Process.myPid().toString())
                setProperty("started", System.currentTimeMillis().toString())
                setProperty("root", root.canonicalPath)
            }
        val file = journal(context)
        val output = file.startWrite()
        try {
            props.store(output, "CARTDECK native launch")
            file.finishWrite(output)
        } catch (error: Throwable) {
            file.failWrite(output)
            throw error
        }
    }

    fun finish(context: Context) = journal(context).delete()

    fun consumeCrash(context: Context): String? {
        val file = journal(context)
        val props = runCatching { Properties().apply { file.openRead().use { load(it) } } }.getOrNull() ?: return null
        val root = File(props.getProperty("root", "")).canonicalFile
        val report =
            buildString {
                appendLine("${props.getProperty("console")} native process stopped")
                appendLine(
                    "APK ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                )
                appendLine("pid=${props.getProperty("pid")} started=${props.getProperty("started")}")
                if (root.path.startsWith(context.filesDir.canonicalPath + File.separator)) {
                    for (name in listOf("native-stage.txt", "last-report.txt", "native-core.log")) {
                        val source = File(root, name)
                        if (source.isFile) {
                            appendLine("\n$name:")
                            source.inputStream().use { appendLine(String(it.readBytesLimited(32768))) }
                        }
                    }
                }
                if (Build.VERSION.SDK_INT >= 30) {
                    runCatching {
                        context.getSystemService(ActivityManager::class.java)
                            .getHistoricalProcessExitReasons(context.packageName, props.getProperty("pid").toInt(), 4)
                            .firstOrNull { it.timestamp >= props.getProperty("started").toLong() }
                            ?.let { exit ->
                                appendLine(
                                    "Android exit reason=${exit.reason} status=${exit.status} " +
                                        "process=${exit.processName}",
                                )
                                appendLine(exit.description)
                                val trace = exit.traceInputStream
                                if (exit.reason == android.app.ApplicationExitInfo.REASON_CRASH_NATIVE) {
                                    appendLine(NativeCrashTrace.read(trace))
                                } else {
                                    trace?.use { appendLine(it.readBytesLimited(65536).toString(Charsets.UTF_8)) }
                                }
                            }
                    }.onFailure { appendLine("Exit trace unavailable: ${it.javaClass.simpleName}") }
                }
            }.take(131072)
        AtomicFile(File(context.filesDir, "last-native-crash.txt")).let { target ->
            val out = target.startWrite()
            try {
                out.write(report.toByteArray())
                target.finishWrite(out)
            } catch (error: Throwable) {
                target.failWrite(out)
            }
        }
        file.delete()
        return report
    }

    private fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        while (output.size() < limit) {
            val count = read(buffer, 0, minOf(buffer.size, limit - output.size()))
            if (count < 0) break
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}
