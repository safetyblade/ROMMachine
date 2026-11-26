package com.swordfish.lemuroid.app.shared.game

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.util.AtomicFile
import com.swordfish.lemuroid.app.shared.support.SupportReport
import com.swordfish.lemuroid.lib.library.db.entity.Game
import java.io.File
import java.util.Properties
import java.util.UUID

/** Extends the native journal to every private game process, including the shared Libretro and PC hosts. */
internal class GameSessionCheckpoint(private val context: Context) {
    private val file = AtomicFile(File(context.filesDir, "game-session.properties"))

    fun begin(
        game: Game,
        target: Intent,
        requestedEngine: String?,
    ) {
        val component = target.component ?: return
        val process = context.packageManager.getActivityInfo(component, 0).processName
        if (process == context.packageName || !process.startsWith("${context.packageName}:")) return
        write(
            Properties().apply {
                setProperty("session", UUID.randomUUID().toString())
                setProperty("process", process)
                setProperty("started", System.currentTimeMillis().toString())
                setProperty("title", SupportReport.sanitise(game.displayName().take(160)))
                setProperty("system", game.systemId)
                requestedEngine?.let { setProperty("requestedEngine", it.take(100)) }
            },
        )
    }

    fun bindProcess() {
        val record = read() ?: return
        val pid = Process.myPid()
        val process =
            if (Build.VERSION.SDK_INT >= 28) {
                Application.getProcessName()
            } else {
                context.getSystemService(ActivityManager::class.java).runningAppProcesses
                    ?.firstOrNull { it.pid == pid }?.processName
            }
        if (process == record.getProperty("process") && record.getProperty("pid") == null) {
            write(record.apply { setProperty("pid", pid.toString()) })
        }
    }

    fun read(): Properties? =
        runCatching { file.baseFile.inputStream().use { Properties().apply { load(it) } } }.getOrNull()

    fun clear(session: String? = null) {
        if (session == null || read()?.getProperty("session") == session) file.delete()
    }

    fun failure(
        record: Properties,
        details: String?,
        restart: Boolean,
        confirmedFailure: Boolean,
    ): SupportReport? {
        val process = record.getProperty("process") ?: return null
        val pid = record.getProperty("pid")?.toIntOrNull() ?: 0
        val started = record.getProperty("started")?.toLongOrNull() ?: return null
        val exit = if (Build.VERSION.SDK_INT >= 30) SessionExitEvidence.find(context, process, pid, started) else null
        if (!confirmedFailure && exit == null) return null
        val exitReason = if (Build.VERSION.SDK_INT >= 30) exit?.reason else null
        val stoppedResponding = exitReason == ApplicationExitInfo.REASON_ANR
        val exitDetails = if (Build.VERSION.SDK_INT >= 30) exit?.let(SessionExitEvidence::render) else null
        return SupportReport.create(
            source = "failure recovery",
            category = if (stoppedResponding) "Game stopped responding" else "Game stopped unexpectedly",
            diagnostic =
                buildString {
                    appendLine("requestedEngine=${record.getProperty("requestedEngine", SupportReport.NOT_CAPTURED)}")
                    appendLine("startupRecovery=$restart")
                    appendLine("saveWrite=${SupportReport.UNKNOWN}")
                    appendLine(exitDetails ?: "androidProcessExit=${SupportReport.NOT_CAPTURED}")
                    details?.let { append(it) }
                },
            gameTitle = record.getProperty("title"),
            system = record.getProperty("system"),
            session = record.getProperty("session"),
        )
    }

    private fun write(record: Properties) {
        val stream = file.startWrite()
        try {
            record.store(stream, "CARTDECK game launch checkpoint")
            file.finishWrite(stream)
        } catch (error: Exception) {
            file.failWrite(stream)
            throw error
        }
    }

    companion object {
        fun launch(
            activity: Activity,
            target: Intent,
            game: Game,
            requestedEngine: String?,
        ) {
            val checkpoint = GameSessionCheckpoint(activity)
            runCatching { checkpoint.begin(game, target, requestedEngine) }
            try {
                activity.startActivityForResult(target, BaseGameActivity.REQUEST_PLAY_GAME)
            } catch (error: Exception) {
                runCatching { checkpoint.clear() }
                throw error
            }
        }
    }
}
