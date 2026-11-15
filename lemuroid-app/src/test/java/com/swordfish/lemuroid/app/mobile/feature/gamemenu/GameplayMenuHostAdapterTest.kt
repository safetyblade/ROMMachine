package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameplayMenuHostAdapterTest {
    @Test
    fun nativeHostsNeverAdvertiseUnsupportedSavestates() {
        listOf(
            GameplayMenuHostKind.XBOX_SDL,
            GameplayMenuHostKind.XENIA,
            GameplayMenuHostKind.ARMSX2,
            GameplayMenuHostKind.CEMU,
        ).forEach { kind ->
            val capabilities = GameplayMenuHostProfiles.capabilities(kind)
            assertTrue(capabilities.canPause)
            assertFalse(capabilities.canSaveState)
            assertFalse(capabilities.canLoadState)
        }
    }

    @Test
    fun libretroStateSupportFollowsTheSelectedCoreOnly() {
        val enabled = GameplayMenuHostProfiles.capabilities(GameplayMenuHostKind.LIBRETRO, true)
        val disabled = GameplayMenuHostProfiles.capabilities(GameplayMenuHostKind.LIBRETRO, false)
        assertTrue(enabled.canSaveState)
        assertTrue(enabled.canLoadState)
        assertFalse(disabled.canSaveState)
        assertFalse(disabled.canLoadState)
        // Lifecycle pausing is owned by BaseGameActivity, not this Compose menu.
        assertFalse(enabled.canPause)
    }

    @Test
    fun nativePauseIsSubmittedButNotReportedAsPaused() = runBlocking {
        val calls = mutableListOf<String>()
        val host =
            GameplayMenuHostAdapter(
                GameplayMenuHostProfiles.capabilities(GameplayMenuHostKind.CEMU),
                pause = { calls.add("pause") },
                resume = { calls.add("resume") },
            )
        val session = GameplayMenuSession(host)
        val opened = session.open()
        assertEquals(GameplayMenuSessionStatus.OPEN_RUNNING, opened.status)
        assertTrue(opened.pauseWasDispatched)
        assertEquals(listOf("pause"), calls)
        assertEquals(GameplayMenuSessionStatus.CLOSED, session.resume().status)
        assertEquals(listOf("pause", "resume"), calls)
    }

    @Test
    fun nativePauseExceptionIsNotReportedAsSuccess() = runBlocking {
        val host =
            GameplayMenuHostAdapter(
                GameplayMenuHostProfiles.capabilities(GameplayMenuHostKind.ARMSX2),
                pause = { throw IllegalStateException("PS2 VM stopped") },
                resume = {},
            )
        val session = GameplayMenuSession(host)
        val opened = session.open()
        assertEquals(GameplayMenuSessionStatus.OPEN_RUNNING, opened.status)
        assertFalse(opened.pauseWasDispatched)
        assertTrue(opened.message.orEmpty().contains("PS2 VM stopped"))
        assertEquals(GameplayMenuSessionStatus.CLOSED, session.resume().status)
    }

    @Test
    fun nativeHostNeverCallsSaveStateWhenUnavailable() = runBlocking {
        var nativeCalls = 0
        val host =
            GameplayMenuHostAdapter(
                GameplayMenuHostProfiles.capabilities(GameplayMenuHostKind.XENIA),
                pause = { nativeCalls++ },
                resume = { nativeCalls++ },
            )
        val session = GameplayMenuSession(host)
        session.open()
        val outcome = session.request(GameplayMenuCommand.SAVE_STATE)
        assertTrue(outcome is GameplayMenuCommandResult.Unsupported)
        assertEquals(1, nativeCalls)
    }

    @Test
    fun existingLifecycleCallsStillRunOnTheCallerThread() {
        val callingThread = Thread.currentThread()
        var observed: Thread? = null
        val host =
            GameplayMenuHostAdapter(
                GameplayMenuHostProfiles.capabilities(GameplayMenuHostKind.XBOX_SDL),
                pause = { observed = Thread.currentThread() },
                resume = {},
            )
        assertTrue(host.dispatch(GameplayMenuCommand.PAUSE) is GameplayMenuCommandResult.Submitted)
        assertEquals(callingThread, observed)
    }
}
