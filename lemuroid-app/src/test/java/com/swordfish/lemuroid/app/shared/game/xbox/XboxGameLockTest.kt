package com.swordfish.lemuroid.app.shared.game.xbox

import com.swordfish.lemuroid.app.shared.game.GameProcessLock
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.io.RandomAccessFile

@RunWith(RobolectricTestRunner::class)
class XboxGameLockTest {
    @Test fun overlappingLaunchPreparationIsReportedBusyAndReleasesCleanly() {
        val context = RuntimeEnvironment.getApplication()
        assertFalse(GameProcessLock.isHeldByAnotherProcess(context))
        RandomAccessFile(File(context.filesDir, "game_process.lock"), "rw").channel.use { channel ->
            val lock = channel.lock()
            try {
                assertTrue(GameProcessLock.isHeldByAnotherProcess(context))
            } finally {
                lock.release()
            }
        }
        assertFalse(GameProcessLock.isHeldByAnotherProcess(context))
    }
}
