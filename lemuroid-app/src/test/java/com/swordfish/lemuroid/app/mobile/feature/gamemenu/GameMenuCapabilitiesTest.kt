package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameMenuCapabilitiesTest {
    @Test fun fullBackendUsesCommonOrderWithoutDuplicateActions() {
        val actions = GameMenuCapabilities(true, true, true, true).homeActions()
        assertEquals(GameMenuAction.values().toList(), actions)
        assertEquals(actions.size, actions.distinct().size)
    }

    @Test fun unsupportedFeaturesAreOmittedFromHome() {
        assertEquals(
            listOf(GameMenuAction.RESUME, GameMenuAction.RETURN_TO_SHELF),
            GameMenuCapabilities().homeActions(),
        )
        for (debug in listOf(false, true)) for (probe in listOf(false, true)) for (rollback in listOf(false, true)) {
            val native = XboxMenuCapabilities(debug, probe, rollback)
            assertEquals(
                listOf(
                    GameMenuAction.RESUME,
                    GameMenuAction.CONTROLS,
                    GameMenuAction.ADVANCED,
                    GameMenuAction.RETURN_TO_SHELF,
                ),
                native.home.homeActions(),
            )
            assertEquals(debug && probe, native.showProbe)
            assertFalse(native.home.states)
            assertFalse(native.home.settings)
            assertEquals(rollback, native.rollback)
        }
    }

    @Test fun menuAndBackgroundCannotDoublePauseOrResumeTheGuest() {
        val gate = GameMenuPauseGate()
        assertEquals(false, gate.transition(true, false, false, false))
        assertEquals(true, gate.transition(true, true, false, false))
        assertNull(gate.transition(true, true, false, false)) // repeated menu or submenu
        assertNull(gate.transition(false, true, false, false)) // background while paused
        assertNull(gate.transition(false, false, false, false)) // dismiss while backgrounded
        assertEquals(false, gate.transition(true, false, false, false)) // same session resumes once
        assertNull(gate.transition(true, false, false, false))
        assertEquals(true, gate.transition(false, false, false, false))
    }

    @Test fun returningToForegroundKeepsAnOpenMenuPausedAndRecoveryNeverResumes() {
        val gate = GameMenuPauseGate()
        assertEquals(true, gate.transition(false, false, false, false))
        assertNull(gate.transition(true, true, false, false))
        assertNull(gate.transition(true, false, true, false))
        assertNull(gate.transition(true, false, false, true))
        assertTrue(gate.transition(true, false, false, false) == false)
    }
}
