package com.swordfish.lemuroid.app.shared.game

import android.content.Context
import com.swordfish.lemuroid.lib.saves.safesave.SaveStorageLease
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException

object GameProcessLock {
    private const val LOCK_FILE_NAME = "game_process.lock"

    private var channel: FileChannel? = null
    private var lock: FileLock? = null

    // Avoid opening a second channel to the same lock file in this JVM while it
    // owns a maintenance lock: closing another descriptor can release OS locks.
    private var idleLock: FileLock? = null

    @Synchronized
    fun acquire(appContext: Context) {
        if (lock?.isValid == true || idleLock != null) return
        val lockFile = File(appContext.filesDir, LOCK_FILE_NAME)
        val candidate = RandomAccessFile(lockFile, "rw").channel
        try {
            val acquired = candidate.tryLock()
            if (acquired != null) {
                channel = candidate
                lock = acquired
            }
        } catch (_: OverlappingFileLockException) {
            // The actual game-loading boundary will fail closed if still unavailable.
        } finally {
            if (channel !== candidate) candidate.close()
        }
    }

    @Synchronized
    private fun beginIdleAccess(lockFile: File): FileLock? {
        if (lock?.isValid == true || idleLock != null) return null
        val candidate = RandomAccessFile(lockFile, "rw").channel
        try {
            val acquired = candidate.tryLock()
            idleLock = acquired
            return acquired
        } catch (_: OverlappingFileLockException) {
            return null
        } finally {
            if (idleLock == null) candidate.close()
        }
    }

    @Synchronized
    private fun endIdleAccess(acquired: FileLock) {
        try {
            acquired.release()
        } finally {
            try {
                acquired.channel().close()
            } finally {
                idleLock = null
            }
        }
    }

    /** Own the launch lock for the complete operation, including across coroutine suspension. */
    suspend fun withIdleAccess(
        appContext: Context,
        action: suspend () -> Unit,
    ): Boolean = withIdleAccess(File(appContext.filesDir, LOCK_FILE_NAME), action)

    internal suspend fun withIdleAccess(
        lockFile: File,
        action: suspend () -> Unit,
    ): Boolean {
        val acquired = beginIdleAccess(lockFile) ?: return false
        try {
            action()
            return true
        } finally {
            endIdleAccess(acquired)
        }
    }

    @Synchronized
    fun acquireExclusive(appContext: Context) {
        acquire(appContext)
        check(lock?.isValid == true) { "Another CARTDECK game or storage operation is active" }
        check(SaveStorageLease.enterGameProcess(appContext)) {
            "Save storage is busy or needs recovery. Finish the SafeSave operation before reopening the game."
        }
        runCatching { GameSessionCheckpoint(appContext).bindProcess() }
    }

    @Synchronized
    fun isHeldByAnotherProcess(appContext: Context): Boolean {
        if (lock?.isValid == true || idleLock != null) return true
        val lockFile = File(appContext.filesDir, LOCK_FILE_NAME)
        RandomAccessFile(lockFile, "rw").channel.use { ch ->
            val testLock =
                try {
                    ch.tryLock()
                } catch (_: OverlappingFileLockException) {
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
