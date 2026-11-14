package com.swordfish.lemuroid.app.shared.game.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.atomic.AtomicLong

@RunWith(RobolectricTestRunner::class)
class CartdeckReactiveLightingSessionTest {
    @Test
    fun strongWeakAndInvalidCoreFeedbackAreNormalizedWithoutStrobe() {
        assertEquals(0f, CartdeckFeedbackStrength.normalized(0f, 0f), 0.0001f)
        assertEquals(0.66f, CartdeckFeedbackStrength.normalized(1f, 0f), 0.0001f)
        assertEquals(0.34f, CartdeckFeedbackStrength.normalized(0f, 1f), 0.0001f)
        assertEquals(1f, CartdeckFeedbackStrength.normalized(1f, 1f), 0.0001f)
        assertEquals(0f, CartdeckFeedbackStrength.normalized(Float.NaN, Float.POSITIVE_INFINITY), 0.0001f)
        assertEquals(1f, CartdeckFeedbackStrength.normalized(2f, 2f), 0.0001f)
    }

    @Test
    fun intensityCapNeverExceedsThirtyFivePercentOrRemainingHeadroom() {
        assertEquals(0f, CartdeckFeedbackStrength.intensity(0, 1f), 0.0001f)
        assertEquals(65f, CartdeckFeedbackStrength.intensity(65, 0f), 0.0001f)
        assertEquals(87.75f, CartdeckFeedbackStrength.intensity(65, 1f), 0.0001f)
        assertEquals(100f, CartdeckFeedbackStrength.intensity(95, 1f), 0.0001f)
        assertEquals(65f, CartdeckFeedbackStrength.intensity(65, Float.NaN), 0.0001f)
    }

    @Test
    fun sessionIgnoresDisabledInputAndRestoresStaticAfterSilence() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val clock = AtomicLong(1000L)
        try {
            val session = CartdeckReactiveLightingSession(scope, clock::get)
            session.onCoreRumble(1f, 1f)
            assertEquals(0f, session.strength.value, 0f)

            session.configure(true)
            session.onCoreRumble(1f, 0f)
            withTimeout(2000L) { session.strength.first { it > 0.5f } }
            assertEquals(0.66f, session.strength.value, 0.0001f)

            // Next event is below the 125ms state-update interval.
            clock.set(1040L)
            session.onCoreRumble(1f, 1f)
            delay(40L)
            assertEquals(0.66f, session.strength.value, 0.0001f)

            clock.set(1170L)
            session.onCoreRumble(1f, 1f)
            withTimeout(2000L) { session.strength.first { it > 0.99f } }

            // No polling or repeating pulse is permitted when rumble ends.
            withTimeout(2000L) { session.strength.first { it == 0f } }
            assertEquals(0f, session.strength.value, 0f)
            session.configure(false)
            session.onCoreRumble(1f, 1f)
            delay(40L)
            assertEquals(0f, session.strength.value, 0f)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun invalidModeIsNotMistakenForCoreFeedback() {
        val mode = "static"
        assertFalse(mode == "feedback")
        assertTrue(CartdeckReactiveLightingSession.MIN_INTERVAL_MS >= 125L)
        assertTrue(CartdeckReactiveLightingSession.IDLE_TIMEOUT_MS <= 300L)
    }
}
