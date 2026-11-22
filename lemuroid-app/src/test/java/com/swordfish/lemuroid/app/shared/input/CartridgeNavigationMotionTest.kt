package com.swordfish.lemuroid.app.shared.input

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CartridgeNavigationMotionTest {
    @Test
    fun stickProducesOneFocusStepUntilItReturnsToNeutral() {
        val navigation = CartridgeNavigationMotion()
        val keys = mutableListOf<Pair<Int, Int>>()

        fun send(
            x: Float,
            y: Float,
        ) {
            val coordinates =
                MotionEvent.PointerCoords().apply {
                    setAxisValue(MotionEvent.AXIS_X, x)
                    setAxisValue(MotionEvent.AXIS_Y, y)
                }
            val properties = MotionEvent.PointerProperties().apply { id = 0 }
            val event =
                MotionEvent.obtain(
                    0L, 100L, MotionEvent.ACTION_MOVE, 1, arrayOf(properties), arrayOf(coordinates),
                    0, 0, 1f, 1f, 7, 0, InputDevice.SOURCE_JOYSTICK, 0,
                )
            try {
                assertTrue(navigation.dispatch(event) { keys += it.keyCode to it.action })
            } finally {
                event.recycle()
            }
        }

        send(0.2f, 0f)
        assertTrue(keys.isEmpty())
        send(0.9f, 0.1f)
        send(0.7f, 0.1f)
        send(0.4f, 0.1f)
        assertEquals(
            listOf(
                KeyEvent.KEYCODE_DPAD_RIGHT to KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_DPAD_RIGHT to KeyEvent.ACTION_UP,
            ),
            keys,
        )
        send(0f, 0f)
        send(0f, -0.8f)
        assertEquals(KeyEvent.KEYCODE_DPAD_UP to KeyEvent.ACTION_UP, keys.last())
        assertEquals(4, keys.size)
    }
}
