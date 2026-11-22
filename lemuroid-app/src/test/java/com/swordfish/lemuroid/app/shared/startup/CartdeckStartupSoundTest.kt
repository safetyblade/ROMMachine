package com.swordfish.lemuroid.app.shared.startup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CartdeckStartupSoundTest {
    @Test
    fun startupSoundUsesMediaGainAndRetainsVolumeWhenMuted() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = CartdeckStartupSound.preferences(context)
        assertFalse(CartdeckStartupSound.muted(prefs))
        assertEquals(0.5f, CartdeckStartupSound.gain(prefs), 0f)
        prefs.edit().putInt(CartdeckStartupSound.VOLUME, 72).putBoolean(CartdeckStartupSound.MUTED, true).commit()
        val reopened = CartdeckStartupSound.preferences(context)
        assertEquals(72, CartdeckStartupSound.volume(reopened))
        assertEquals(0f, CartdeckStartupSound.gain(reopened), 0f)
        reopened.edit().putBoolean(CartdeckStartupSound.MUTED, false).commit()
        assertEquals(0.72f, CartdeckStartupSound.gain(reopened), 0f)
    }

    @Test
    fun outOfRangeVolumeIsClamped() {
        val prefs = CartdeckStartupSound.preferences(RuntimeEnvironment.getApplication())
        prefs.edit().putInt(CartdeckStartupSound.VOLUME, 101).commit()
        assertEquals(1f, CartdeckStartupSound.gain(prefs), 0f)
        prefs.edit().putInt(CartdeckStartupSound.VOLUME, -1).commit()
        assertEquals(0f, CartdeckStartupSound.gain(prefs), 0f)
    }
}
