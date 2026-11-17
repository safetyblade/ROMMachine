package com.swordfish.lemuroid.app.shared.input

import android.view.KeyEvent
import android.view.MotionEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhysicalControllerInputTest {
    @Test fun rangeDeadzoneAndInvalidSamplesAreNormalized() {
        val range = PhysicalControllerInput.Range(-32768f, 32767f, 1000f)
        assertEquals(0f, PhysicalControllerInput.axis(500f, range, false), 0.0001f)
        assertEquals(1f, PhysicalControllerInput.axis(32767f, range, false), 0.0001f)
        assertEquals(-1f, PhysicalControllerInput.axis(-32768f, range, false), 0.0001f)
        assertEquals(0f, PhysicalControllerInput.axis(Float.NaN, range, false), 0f)
        assertEquals(0.5f, PhysicalControllerInput.axis(0f, PhysicalControllerInput.Range(-1f, 1f), true), 0f)
    }

    @Test fun duplicateTriggerSourcesHaveOneLogicalOwnerAndDisconnectClearsHeldInput() {
        val state = PhysicalControllerInput.State()
        val ranges = mapOf(MotionEvent.AXIS_LTRIGGER to PhysicalControllerInput.Range(0f, 1f))
        state.motion({ if (it == MotionEvent.AXIS_LTRIGGER) 0.6f else 0f }, ranges)
        state.key(KeyEvent.KEYCODE_BUTTON_L2, true)
        assertEquals(1f, state.values[PhysicalControllerInput.Control.TRIGGER_L])
        state.key(KeyEvent.KEYCODE_BUTTON_L2, false)
        assertEquals(0.6f, state.values[PhysicalControllerInput.Control.TRIGGER_L])
        state.clear()
        assertTrue(state.values.isEmpty())
    }

    @Test fun vendorEvidenceAndRightStickVariantsDoNotDependOnPrintedLabels() {
        assertEquals(
            ControllerHardwareProfile.Family.PLAYSTATION,
            ControllerHardwareProfile.family("Wireless Controller", 0x054c, 1),
        )
        assertEquals(
            MotionEvent.AXIS_RX to MotionEvent.AXIS_RY,
            PhysicalControllerInput.rightAxes(setOf(MotionEvent.AXIS_RX, MotionEvent.AXIS_RY)),
        )
        assertEquals(
            MotionEvent.AXIS_Z to MotionEvent.AXIS_RZ,
            PhysicalControllerInput.rightAxes(setOf(MotionEvent.AXIS_Z, MotionEvent.AXIS_RZ)),
        )
        assertEquals(
            PhysicalControllerInput.Control.FACE_SOUTH,
            PhysicalControllerInput.keys[KeyEvent.KEYCODE_BUTTON_A],
        )
    }
}
