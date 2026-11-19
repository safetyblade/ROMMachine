package com.swordfish.lemuroid.app.shared.game

import android.content.Context
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.channels.FileLock

object GameProcessLock {
    private const val LOCK_FILE_NAME = "game_process.lock"

    @Volatile
    private var channel: FileChannel? = null

    @Volatile
    private var lock: FileLock? = null

    @Synchronized
    fun acquire(appContext: Context) {
        if (lock?.isValid == true) return
        val lockFile = File(appContext.filesDir, LOCK_FILE_NAME)
        val candidate = RandomAccessFile(lockFile, "rw").channel
        try {
            val acquired = candidate.tryLock()
            if (acquired != null) {
                channel = candidate
                lock = acquired
            }
        } catch (_: java.nio.channels.OverlappingFileLockException) {
            // Retry at the actual game-loading boundary.
        } finally {
            if (channel !== candidate) candidate.close()
        }
    }

    /** Cleanup owns the same interprocess lock as game launch, not just a snapshot check. */
    suspend fun withIdleAccess(
        appContext: Context,
        action: suspend () -> Unit,
    ): Boolean {
        val lockFile = File(appContext.filesDir, LOCK_FILE_NAME)
        RandomAccessFile(lockFile, "rw").channel.use { candidate ->
            val acquired =
                try {
                    candidate.tryLock()
                } catch (_: java.nio.channels.OverlappingFileLockException) {
                    null
                } ?: return false
            try {
                action()
                return true
            } finally {
                acquired.release()
            }
        }
    }

    @Synchronized
    fun acquireExclusive(appContext: Context) {
        acquire(appContext)
        check(lock?.isValid == true) { "Another CARTDECK game is already running" }
    }

    fun isHeldByAnotherProcess(appContext: Context): Boolean {
        val lockFile = File(appContext.filesDir, LOCK_FILE_NAME)
        RandomAccessFile(lockFile, "rw").channel.use { ch ->
            val testLock =
                try {
                    ch.tryLock()
                } catch (_: java.nio.channels.OverlappingFileLockException) {
                    return true
                }
            return if (testLock != null) {
                testLock.release()
                false
            } else {
                true
            }
        }
    }
}
