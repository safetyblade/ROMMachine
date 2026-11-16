package com.swordfish.lemuroid.app.shared.game

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchPreparationTest {
    @Test fun cancelWaitsForAtomicWriteAndPreventsLateStartup() =
        runBlocking {
            val entered = CompletableDeferred<Unit>()
            val boundary = CompletableDeferred<Unit>()
            val preparation = LaunchPreparation()
            var booted = false
            preparation.start(this) {
                withContext(NonCancellable) {
                    entered.complete(Unit)
                    boundary.await()
                }
                coroutineContext.ensureActive()
                booted = true
            }
            entered.await()
            val cleanup = async { preparation.awaitCleanup() }
            yield()
            assertTrue(preparation.cancelling.value)
            assertFalse(cleanup.isCompleted)
            boundary.complete(Unit)
            cleanup.await()
            assertFalse(booted)
            assertNull(preparation.start(this) { booted = true })
        }

    @Test fun repeatedStartHasOneOwnerAndRepeatedCancelIsSafe() =
        runBlocking {
            val preparation = LaunchPreparation()
            var sessions = 0
            val first = preparation.start(this) { sessions++ }
            assertSame(first, preparation.start(this) { sessions++ })
            first?.join()
            preparation.awaitCleanup()
            preparation.awaitCleanup()
            assertEquals(1, sessions)
        }
}
