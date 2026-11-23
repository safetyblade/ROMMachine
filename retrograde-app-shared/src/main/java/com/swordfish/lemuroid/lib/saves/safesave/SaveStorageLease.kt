package com.swordfish.lemuroid.lib.saves.safesave

import android.content.Context
import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile

/** One process-safe fence shared by game hosts, the Drive worker and local backup. */
object SaveStorageLease {
    private var held: Closeable? = null
    private var engineLease: Closeable? = null

    @Synchronized
    fun acquire(context: Context): Closeable? {
        if (held != null) return null
        val handle = RandomAccessFile(File(context.noBackupFilesDir, "safesave-storage.lock"), "rw")
        try {
            if (handle.channel.tryLock() != null) {
                val lease = Closeable { release(handle) }
                held = lease
                return lease
            }
        } catch (failure: Throwable) {
            handle.close()
            throw failure
        }
        handle.close()
        return null
    }

    @Synchronized
    private fun release(handle: RandomAccessFile) {
        try {
            handle.close()
        } finally {
            held = null
        }
    }

    /** The game process retains the lease until it exits and the OS releases the lock. */
    @Synchronized
    fun enterGameProcess(context: Context): Boolean {
        if (engineLease == null) {
            val lease = acquire(context) ?: return false
            if (BoundSaveRestore.recoveryPending(context)) {
                lease.close()
                return false
            }
            engineLease = lease
        }
        return engineLease != null
    }
}
