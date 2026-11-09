package com.swordfish.lemuroid.app.psp

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.app.shared.game.EmulatorCompatibilityPreferences
import com.swordfish.lemuroid.lib.library.SystemID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EmulatorCompatibilityPreferencesTest {
    @Test
    fun noPackagedCandidateCannotBecomeDefaultOrGameOverride() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = EmulatorCompatibilityPreferences(context)
        assertFalse(prefs.available(SystemID.PSX))
        assertFalse(prefs.available(SystemID.PSP))
        assertFalse(prefs.setSystemEngine(SystemID.PSX, true))
        assertFalse(prefs.setGameEngine(SystemID.PSP, 32938, true))
        assertFalse(prefs.isUpdatedRequested(SystemID.PSX, 39291))
        assertFalse(prefs.isUpdatedRequested(SystemID.PSP, 32938))
    }

    @Test
    fun previousEngineAndDefaultRecoveryAlwaysRemainSelectable() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = EmulatorCompatibilityPreferences(context)
        assertTrue(prefs.setSystemEngine(SystemID.PSX, false))
        assertTrue(prefs.setGameEngine(SystemID.PSP, 17821, false))
        assertTrue(prefs.restoreGameDefault(SystemID.PSP, 17821))
        assertTrue(prefs.restoreSystemDefault(SystemID.PSX))
        assertFalse(prefs.isUpdatedRequested(SystemID.PSX, 3))
    }
}
