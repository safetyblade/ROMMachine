package com.swordfish.lemuroid.app.shared.input

import android.view.KeyEvent
import android.view.MotionEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test fun missingAndUnipolarAxesCannotProducePhantomMovement() {
        val profile = ControllerProfileStore.Profile()
        val ranges =
            mapOf(
                MotionEvent.AXIS_Y to PhysicalControllerInput.Range(0f, 1f),
                MotionEvent.AXIS_Z to PhysicalControllerInput.Range(0f, 1f),
                MotionEvent.AXIS_RZ to PhysicalControllerInput.Range(0f, 1f),
            )
        listOf(
            MotionEvent.AXIS_X,
            MotionEvent.AXIS_Y,
            MotionEvent.AXIS_Z,
            MotionEvent.AXIS_RZ,
            MotionEvent.AXIS_HAT_Y,
            MotionEvent.AXIS_BRAKE,
        ).forEach { code ->
            assertEquals(0f, PhysicalControllerInput.sampleAxis({ 0f }, ranges, code, profile), 0f)
        }
    }

    @Test fun neutralDeadzoneAndFullTravelRemainCorrect() {
        val ranges = mapOf(MotionEvent.AXIS_Y to PhysicalControllerInput.Range(-1f, 1f))
        val profile = ControllerProfileStore.Profile()

        fun sample(value: Float) = PhysicalControllerInput.sampleAxis({ value }, ranges, MotionEvent.AXIS_Y, profile)

        assertEquals(0f, sample(-0.08f), 0f)
        assertEquals(-1f, sample(-1f), 0f)
        assertEquals(1f, sample(1f), 0f)
        assertEquals(0f, sample(0f), 0f)
        assertEquals(0.05f, PhysicalControllerInput.effectiveDeadzone(profile.copy(deadzone = 0.05f)), 0f)
        assertEquals(0f, PhysicalControllerInput.effectiveDeadzone(profile.copy(calibrated = true)), 0f)
    }

    @Test fun signedRightStickPreferredOverTriggerLikeAxes() {
        val ranges =
            mapOf(
                MotionEvent.AXIS_Z to PhysicalControllerInput.Range(0f, 1f),
                MotionEvent.AXIS_RZ to PhysicalControllerInput.Range(0f, 1f),
                MotionEvent.AXIS_RX to PhysicalControllerInput.Range(-1f, 1f),
                MotionEvent.AXIS_RY to PhysicalControllerInput.Range(-1f, 1f),
            )
        val raw: (Int) -> Float = { if (it == MotionEvent.AXIS_RY) -1f else 0f }
        val profile = ControllerProfileStore.Profile()
        assertEquals(-1f, PhysicalControllerInput.sampleAxis(raw, ranges, MotionEvent.AXIS_RZ, profile), 0f)
        assertEquals(0f, PhysicalControllerInput.sampleAxis(raw, ranges, MotionEvent.AXIS_Z, profile), 0f)
    }

    @Test fun explicitOffsetCalibrationRetainsUnipolarStickSupport() {
        val range = mapOf(MotionEvent.AXIS_Y to PhysicalControllerInput.Range(0f, 1f))
        val profile = ControllerProfileStore.Profile(centres = mapOf(MotionEvent.AXIS_Y to 0.5f), calibrated = true)

        fun sample(value: Float) = PhysicalControllerInput.sampleAxis({ value }, range, MotionEvent.AXIS_Y, profile)

        assertEquals(0f, sample(0.5f), 0f)
        assertEquals(-1f, sample(0f), 0f)
        assertEquals(1f, sample(1f), 0f)
        assertEquals(0f, sample(0.5f), 0f)
    }

    @Test fun staleFramesCannotReplayAfterMappingOrTopologyChanges() {
        val gate = ControllerFrameGate()
        assertTrue(gate.accepts(1L, 0L, 0L))
        assertFalse(gate.accepts(1L, 0L, 0L))
        assertFalse(gate.accepts(2L, 0L, 1L))
        assertTrue(gate.accepts(3L, 1L, 1L))
        assertFalse(gate.accepts(2L, 1L, 1L))
        assertTrue(gate.accepts(4L, 1L, 1L))
    }

    @Test fun perPortAxisReleaseAndRemapDoNotLeaveOldDirectionsHeld() {
        val state = ControllerDigitalAxisState()
        val up = KeyEvent.KEYCODE_DPAD_UP
        val down = KeyEvent.KEYCODE_DPAD_DOWN
        assertEquals(listOf(ControllerDigitalAxisState.Edge(up, true)), state.update(0, setOf(up)))
        assertEquals(listOf(ControllerDigitalAxisState.Edge(up, true)), state.update(1, setOf(up)))
        assertEquals(emptyList<ControllerDigitalAxisState.Edge>(), state.update(0, setOf(up)))
        assertEquals(
            listOf(ControllerDigitalAxisState.Edge(up, false), ControllerDigitalAxisState.Edge(down, true)),
            state.update(0, setOf(down)),
        )
        assertEquals(listOf(ControllerDigitalAxisState.Edge(up, false)), state.update(1, emptySet()))
        assertEquals(listOf(ControllerDigitalAxisState.Edge(down, false)), state.update(0, emptySet()))
        state.update(0, setOf(up))
        state.clear()
        assertEquals(emptyList<ControllerDigitalAxisState.Edge>(), state.update(0, emptySet()))
    }
}
