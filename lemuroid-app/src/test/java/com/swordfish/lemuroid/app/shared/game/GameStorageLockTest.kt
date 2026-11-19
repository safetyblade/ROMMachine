package com.swordfish.lemuroid.app.shared.game

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class GameStorageLockTest {
    @Test
    fun nestedMaintenanceIsExcludedAndLockReleasedAfterFailure() {
        val root = Files.createTempDirectory("cartdeck-storage-lock").toFile()
        try {
            val lock = File(root, "game_process.lock")
            runBlocking {
                assertTrue(
                    GameProcessLock.withIdleAccess(lock) {
                        assertFalse(GameProcessLock.withIdleAccess(lock) { error("Must not run") })
                    },
                )
                val result =
                    runCatching {
                        GameProcessLock.withIdleAccess(lock) { error("Interrupted cleanup") }
                    }
                assertTrue(result.isFailure)
                assertTrue(GameProcessLock.withIdleAccess(lock) {})
            }
        } finally {
            root.deleteRecursively()
        }
    }
}
