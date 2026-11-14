package com.swordfish.lemuroid.app.shared.game.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CartdeckControllerActivitySessionTest {
    @Test
    fun controllerPressUsesSameBoundedReducerAndGoesIdleAfterSilence() {
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            try {
                val session = CartdeckReactiveLightingSession(scope)
                session.onControllerActivity()
                assertEquals(0f, session.strength.value, 0.0001f)

                session.configure(true)
                session.onControllerActivity()
                withTimeout(2500L) { session.strength.first { it > 0f } }
                assertEquals(
                    CartdeckReactiveLightingSession.CONTROLLER_ACTIVITY_STRENGTH,
                    session.strength.value,
                    0.0001f,
                )
                withTimeout(2500L) { session.strength.first { it == 0f } }
                assertEquals(0f, session.strength.value, 0.0001f)

                session.configure(false)
                session.onControllerActivity()
                assertEquals(0f, session.strength.value, 0.0001f)
            } finally {
                scope.cancel()
            }
        }
    }

    @Test
    fun controllerSignalCannotRunFasterThanExistingFeedbackBounds() {
        assertEquals(0.75f, CartdeckReactiveLightingSession.CONTROLLER_ACTIVITY_STRENGTH, 0.0001f)
        assertTrue(CartdeckReactiveLightingSession.MIN_INTERVAL_MS >= 125L)
        assertTrue(CartdeckReactiveLightingSession.IDLE_TIMEOUT_MS <= 300L)
    }
}
