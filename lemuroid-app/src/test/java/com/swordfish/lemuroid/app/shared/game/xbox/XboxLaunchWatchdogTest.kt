package com.swordfish.lemuroid.app.shared.game.xbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class XboxLaunchWatchdogTest {
    @Test fun legacyAcquiredFrameNeverCertifiesGuestPresentationOrKillsRollbackGameplay() {
        val watchdog = XboxLaunchWatchdog(1000)
        assertNull(watchdog.poll(5000, 0, false, legacyFrameAcquired = true))
        org.junit.Assert.assertFalse(watchdog.presented)
        assertEquals("LEGACY_PRESENTATION_UNVERIFIED", watchdog.stage)
    }

    @Test fun stoppedCoreFailsImmediatelyEvenAfterEarlierPresentation() {
        val watchdog = XboxLaunchWatchdog(1000)
        assertNull(watchdog.poll(10, 1, true))
        assertNotNull(watchdog.poll(0, -1, true))
        assertEquals("CORE_STOPPED", watchdog.stage)
    }

    @Test fun pausedIntervalsDoNotConsumeStartupBudget() {
        val watchdog = XboxLaunchWatchdog(1000)
        repeat(20) { assertNull(watchdog.poll(0, 0, false)) }
        assertNull(watchdog.poll(999, 0, false))
        assertNotNull(watchdog.poll(1, 0, false))
    }

    @Test fun bootWithoutPresentationTimesOutEvenWhenCoreRuns() {
        val watchdog = XboxLaunchWatchdog(1000)
        assertNull(watchdog.poll(600, 0, false))
        assertNotNull(watchdog.poll(400, 1, false))
        assertEquals("CORE_RUNNING_AWAITING_PRESENT", watchdog.stage)
    }

    @Test fun confirmedGuestPresentStopsStartupTimeout() {
        val watchdog = XboxLaunchWatchdog(1000)
        assertNull(watchdog.poll(999, 1, false))
        assertNull(watchdog.poll(1, 1, true))
        assertNull(watchdog.poll(10000, 1, false))
        assertEquals("FIRST_GUEST_FRAME_PRESENTED", watchdog.stage)
    }
}
