package com.swordfish.lemuroid.app.shared.game

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Process
import java.util.concurrent.atomic.AtomicBoolean

/** Gives existing save and teardown paths time to finish even if their main looper is blocked. */
object EngineShutdownDeadline {
    private val armed = AtomicBoolean()
    private const val GRACE_MILLIS = 25_000L

    @JvmStatic
    fun arm(context: Context) {
        val pid = Process.myPid()
        val name =
            if (Build.VERSION.SDK_INT >= 28) {
                Application.getProcessName()
            } else {
                context.getSystemService(ActivityManager::class.java).runningAppProcesses
                    ?.firstOrNull { it.pid == pid }?.processName
            }
        if (name?.startsWith("${context.packageName}:") != true || !armed.compareAndSet(false, true)) return
        Thread({
            android.os.SystemClock.sleep(GRACE_MILLIS)
            Process.killProcess(pid)
        }, "cartdeck-engine-shutdown-deadline").apply { isDaemon = true }.start()
    }
}
