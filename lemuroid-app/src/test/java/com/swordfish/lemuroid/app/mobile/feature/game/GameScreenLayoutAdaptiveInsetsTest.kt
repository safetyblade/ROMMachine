package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameScreenLayoutAdaptiveInsetsTest {
    @Test
    fun portraitHandheldRemainsIdenticalToAcceptedGlass() {
        val standard = portraitHardwareInsets(393.dp, 852.dp, expanded = false)
        assertEquals(84.dp, standard.top)
        assertEquals(22.dp, standard.side)
        assertEquals(16.dp, standard.controlGap)
        val expanded = portraitHardwareInsets(393.dp, 852.dp, expanded = true)
        assertEquals(76.dp, expanded.top)
        assertEquals(14.dp, expanded.side)
    }

    @Test
    fun compactPortraitGivesControlsClearanceAndTabletPreservesProportions() {
        val compact = portraitHardwareInsets(320.dp, 640.dp, expanded = false)
        val baseline = portraitHardwareInsets(393.dp, 852.dp, expanded = false)
        val tablet = portraitHardwareInsets(800.dp, 1100.dp, expanded = false)
        assertTrue(compact.top < baseline.top)
        assertTrue(compact.side < baseline.side)
        assertTrue(tablet.top > baseline.top)
        assertTrue(tablet.controlGap > baseline.controlGap)
    }

    @Test
    fun acceptedLandscapeHandheldRetainsExistingInsets() {
        val insets = landscapeHardwareInsets(852.dp, 393.dp)
        assertEquals(42.dp, insets.padTopBottom)
        assertEquals(16.dp, insets.sideInset)
        assertEquals(16.dp, insets.screenHorizontal)
        assertEquals(8.dp, insets.screenVertical)
    }

    @Test
    fun compactDeviceUsesLessChassisClearanceWithoutRearrangingButtons() {
        val compact = landscapeHardwareInsets(640.dp, 320.dp)
        val normal = landscapeHardwareInsets(852.dp, 393.dp)
        assertTrue(compact.padTopBottom < normal.padTopBottom)
        assertTrue(compact.sideInset < normal.sideInset)
        assertTrue(compact.screenHorizontal < normal.screenHorizontal)
    }

    @Test
    fun tabletIncreasesClearanceButKeepsAFiniteGameViewport() {
        val tablet = landscapeHardwareInsets(1280.dp, 800.dp)
        assertTrue(tablet.padTopBottom > 42.dp)
        assertTrue(tablet.screenHorizontal > 16.dp)
        assertTrue(tablet.screenHorizontal * 2 < 1280.dp)
    }
}
