package com.swordfish.lemuroid.app.debug

import android.app.Application
import androidx.work.WorkManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Default fixtures cannot leave production constraint trackers running after sandbox teardown. */
@RunWith(RobolectricTestRunner::class)
class UnitApplicationIsolationTest {
    @Test
    fun defaultApplicationDoesNotStartProductionBackgroundWork() {
        val application = RuntimeEnvironment.getApplication()
        assertEquals(Application::class.java, application.javaClass)
        assertThrows(IllegalStateException::class.java) { WorkManager.getInstance(application) }
    }
}
