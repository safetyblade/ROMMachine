package com.swordfish.lemuroid.app.safesave

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.app.shared.game.GameProcessLock
import com.swordfish.lemuroid.lib.saves.safesave.SaveStorageLease
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class NativeSafeSaveLeaseTest {
    @Test fun nativeHostCannotStartWhileBackupOwnsTheSaveLease() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        requireNotNull(SaveStorageLease.acquire(context)).use {
            val failure =
                assertThrows(IllegalStateException::class.java) {
                    GameProcessLock.acquireExclusive(context)
                }
            assertTrue(failure.message.orEmpty().contains("Save storage is busy"))
        }
    }
}
