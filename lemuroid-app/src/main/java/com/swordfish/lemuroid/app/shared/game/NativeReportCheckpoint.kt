package com.swordfish.lemuroid.app.shared.game

/** Rate-limits routine file checkpoints, not native health checks or explicitly requested reports. */
internal class NativeReportCheckpoint(private val intervalMs: Long = 5000L) {
    private var lastWrite: Long? = null
    private var lastStage: String? = null

    fun due(
        nowMs: Long,
        stage: String,
    ): Boolean {
        val previous = lastWrite
        if (previous != null && lastStage == stage && nowMs >= previous && nowMs - previous < intervalMs) {
            return false
        }
        lastWrite = nowMs
        lastStage = stage
        return true
    }
}
