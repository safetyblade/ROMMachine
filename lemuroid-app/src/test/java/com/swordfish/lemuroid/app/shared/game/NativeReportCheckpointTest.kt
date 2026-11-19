package com.swordfish.lemuroid.app.shared.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeReportCheckpointTest {
    @Test
    fun preservesFirstStageChangesAndPeriodicEvidence() {
        val checkpoint = NativeReportCheckpoint()
        assertTrue(checkpoint.due(0, "LOADING"))
        assertFalse(checkpoint.due(50, "LOADING"))
        assertTrue(checkpoint.due(100, "PRESENTED"))
        assertFalse(checkpoint.due(1000, "PRESENTED"))
        assertTrue(checkpoint.due(5100, "PRESENTED"))
        assertTrue(checkpoint.due(5110, "ERROR"))
    }

    @Test
    fun separateSessionsAndClockResetDoNotSuppressEvidence() {
        val first = NativeReportCheckpoint()
        val second = NativeReportCheckpoint()
        assertTrue(first.due(5000, "PRESENTED"))
        assertFalse(first.due(5001, "PRESENTED"))
        assertTrue(second.due(5001, "PRESENTED"))
        assertTrue(first.due(1, "PRESENTED"))
    }
}
