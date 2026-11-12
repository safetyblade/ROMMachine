package com.swordfish.lemuroid.app.shared.game

import android.content.Context
import android.os.Process
import android.util.AtomicFile
import java.io.File
import java.util.Properties

/** One bounded, private cross-process record; never stores a title, URI or ROM path. */
class PS2LaunchJournal(context: Context) {
    enum class Stage {
        REQUESTED,
        ACTIVITY_STARTED,
        CORE_PROVISION,
        ROM_STAGE,
        HOST_INIT,
        FRONTEND_FRAME,
        CORE_FRAME,
        CLEAN_EXIT,
    }

    private val file = AtomicFile(File(context.filesDir, "native/ps2/launch.properties"))

    fun begin() {
        write(
            Properties().apply {
                setProperty("started", System.currentTimeMillis().toString())
                setProperty("stage", Stage.REQUESTED.name)
                setProperty("pid", "0")
            },
        )
    }

    fun stage(stage: Stage) =
        synchronized(writerLock) {
            val current = read()
            val previous =
                runCatching {
                    Stage.valueOf(
                        current.getProperty("stage", "REQUESTED"),
                    )
                }.getOrDefault(Stage.REQUESTED)
            if (stage.ordinal < previous.ordinal) return@synchronized
            writeUpdate(
                current.apply {
                    setProperty("stage", stage.name)
                    setProperty("pid", Process.myPid().toString())
                    setProperty("stageTime", System.currentTimeMillis().toString())
                },
            )
        }

    fun graphics(
        vendor: String?,
        renderer: String?,
        version: String?,
    ) = synchronized(writerLock) {
        writeUpdate(
            read().apply {
                setProperty("glVendor", safeGraphicsValue(vendor))
                setProperty("glRenderer", safeGraphicsValue(renderer))
                setProperty("glVersion", safeGraphicsValue(version))
            },
        )
    }

    private fun safeGraphicsValue(value: String?): String =
        value?.filter { !it.isISOControl() }?.take(160) ?: "unavailable"

    fun selectedCore(path: String) =
        synchronized(writerLock) {
            val directory = File(path).parentFile
            val version = directory?.name?.takeIf { it.matches(Regex("[a-fA-F0-9]{12}-[a-fA-F0-9]{12}")) }
            val sha =
                runCatching { File(directory, "sha256.txt").readText().trim() }.getOrNull()
                    ?.takeIf { it.matches(Regex("[a-fA-F0-9]{64}")) }
            writeUpdate(
                read().apply {
                    setProperty("selectedVersion", version ?: "unavailable")
                    setProperty("selectedSHA256", sha ?: "unavailable")
                },
            )
        }

    fun failure(code: String) =
        synchronized(writerLock) {
            val record = read()
            if (!record.containsKey("error")) {
                writeUpdate(
                    record.apply { setProperty("error", code.filter { it.isLetterOrDigit() || it == '_' }.take(80)) },
                )
            }
        }

    fun read(): Properties =
        // AtomicFile.openRead() can remove an in-flight .new file. The supervisor
        // reads only the committed base; only the engine process writes updates.
        runCatching { file.baseFile.inputStream().use { Properties().apply { load(it) } } }.getOrDefault(Properties())

    private fun writeUpdate(properties: Properties) {
        // Diagnostics must not turn a successful launch/exit into a disk-full crash.
        runCatching { write(properties) }
    }

    private fun write(properties: Properties) {
        file.baseFile.parentFile?.mkdirs()
        val output = file.startWrite()
        try {
            properties.store(output, "CARTDECK PS2 launch")
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        }
    }

    companion object {
        private val writerLock = Any()

        /** Older hosts cannot report core frames; never terminate gameplay on their frontend-only signal. */
        fun startupComplete(
            stage: String,
            coreSignalAvailable: Boolean,
        ): Boolean =
            stage in setOf(Stage.CORE_FRAME.name, Stage.CLEAN_EXIT.name) ||
                (!coreSignalAvailable && stage == Stage.FRONTEND_FRAME.name)

        /** Staging a large SAF disc gets a separate budget from native host startup. */
        fun startupBudgetMillis(stage: String): Long =
            if (stage in setOf(Stage.HOST_INIT.name, Stage.FRONTEND_FRAME.name)) 180_000L else 600_000L
    }
}
