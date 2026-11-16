package com.swordfish.lemuroid.app.shared.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ControllerHatEdgesTest {
    @Test fun diagonalsReleaseIndependentlyAndHeldSamplesNeverRepeatCapture() {
        val hats = ControllerHatEdges()
        assertEquals(setOf(21 to true, 19 to true), hats.update(-1f, -1f).toSet())
        assertTrue(hats.update(-1f, -1f).isEmpty())
        assertEquals(listOf(19 to false), hats.update(-1f, 0f))
        assertEquals(listOf(21 to false), hats.update(0f, 0f))
        assertTrue(hats.update(0.1f, -0.1f).isEmpty())
        hats.update(1f, 1f)
        hats.clear()
        assertTrue(hats.update(0f, 0f).isEmpty())
    }
}
