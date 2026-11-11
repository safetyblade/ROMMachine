package com.swordfish.lemuroid.app.debug

import android.os.Bundle
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File

/**
 * PS2-01 ONLY: a debug-only, hidden entrypoint using CARTDECK's actual
 * GLRetroView/LibretroDroid dependency. No Play! core is packaged or downloaded.
 *
 * Stage a locally built, licence-cleared-for-private-testing Play! core and lawful
 * homebrew disc with scripts/ci/ps201_device_probe.sh. This is NOT the retail
 * PlayStation 2 launcher, nor evidence that Play! graphics are compatible.
 */
class PlayStation2RuntimeProbeActivity : ComponentActivity() {
    private var retroView: GLRetroView? = null
    private var frames = 0
    private val triggerStates = mutableMapOf<Int, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val lab = File(filesDir, "ps2-lab")
        val core = File(lab, "play_libretro.so")
        val gameDirectory = File(lab, "game")
        val discName = intent.getStringExtra("disc") ?: "test.iso"
        val disc = File(gameDirectory, discName)

        if (
            discName != File(discName).name ||
            !discName.matches(Regex("[a-zA-Z0-9][a-zA-Z0-9 ._-]{0,180}")) ||
            disc.extension.lowercase() !in setOf("iso", "chd") ||
            !core.isFile ||
            !disc.isFile ||
            disc.canonicalFile.parentFile != gameDirectory.canonicalFile
        ) {
            reportFailure(
                "Invalid Play! core or legal PS2 fixture; run ps201_device_probe.sh",
            )
            return
        }

        val systemDirectory = File(lab, "system").apply { mkdirs() }
        val savesDirectory = File(lab, "saves").apply { mkdirs() }
        val data =
            GLRetroViewData(applicationContext).apply {
                coreFilePath = core.absolutePath
                gameFilePath = disc.absolutePath
                this.systemDirectory = systemDirectory.absolutePath
                this.savesDirectory = savesDirectory.absolutePath
            }

        Log.i(TAG, "CORE_AND_DISC_FOUND coreBytes=${core.length()} disc=${disc.name}")
        Log.i(TAG, "SAVE_DIR ${savesDirectory.absolutePath} (never deleted by this probe)")
        try {
            val view = GLRetroView(this, data)
            retroView = view
            setContentView(
                FrameLayout(this).apply {
                    addView(
                        view,
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT,
                        ),
                    )
                },
            )
            lifecycle.addObserver(view)
            lifecycleScope.launch {
                view.getGLRetroEvents().collect { event ->
                    if (event is GLRetroView.GLRetroEvents.FrameRendered) {
                        frames += 1
                        if (frames == 1 || frames % 300 == 0) {
                            Log.i(TAG, "FRAME_RENDERED count=$frames")
                        }
                    } else {
                        Log.i(TAG, "RETRO_EVENT ${event.javaClass.simpleName}")
                    }
                }
            }
            Log.i(TAG, "VIEW_ATTACHED waiting for native context, frame, audio and gamepad")
        } catch (error: Throwable) {
            Log.e(TAG, "LIBRETRODROID_INIT_FAILED ${error.javaClass.simpleName}", error)
            reportFailure("LibretroDroid could not initialise; inspect PS201 logcat")
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) return super.dispatchKeyEvent(event)
        if (
            KeyEvent.isGamepadButton(event.keyCode) ||
            event.keyCode in KeyEvent.KEYCODE_DPAD_UP..KeyEvent.KEYCODE_DPAD_CENTER
        ) {
            retroView?.sendKeyEvent(event.action, event.keyCode, 0)
            Log.i(TAG, "GAMEPAD_KEY action=${event.action} key=${event.keyCode}")
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if ((event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) {
            retroView?.sendMotionEvent(
                GLRetroView.MOTION_SOURCE_ANALOG_LEFT,
                event.getAxisValue(MotionEvent.AXIS_X),
                event.getAxisValue(MotionEvent.AXIS_Y),
                0,
            )
            retroView?.sendMotionEvent(
                GLRetroView.MOTION_SOURCE_ANALOG_RIGHT,
                event.getAxisValue(MotionEvent.AXIS_Z),
                event.getAxisValue(MotionEvent.AXIS_RZ),
                0,
            )
            listOf(
                MotionEvent.AXIS_LTRIGGER to KeyEvent.KEYCODE_BUTTON_L2,
                MotionEvent.AXIS_RTRIGGER to KeyEvent.KEYCODE_BUTTON_R2,
            ).forEach { (axis, key) ->
                val pressed = event.getAxisValue(axis) > 0.5f
                if (triggerStates[key] != pressed) {
                    retroView?.sendKeyEvent(if (pressed) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP, key, 0)
                    triggerStates[key] = pressed
                }
            }
            retroView?.sendMotionEvent(
                GLRetroView.MOTION_SOURCE_DPAD,
                event.getAxisValue(MotionEvent.AXIS_HAT_X),
                event.getAxisValue(MotionEvent.AXIS_HAT_Y),
                0,
            )
            Log.i(TAG, "GAMEPAD_AXIS leftX=${event.getAxisValue(MotionEvent.AXIS_X)}")
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    private fun reportFailure(message: String) {
        Log.e(TAG, "PS201_BLOCKED $message")
        setContentView(TextView(this).apply { text = message })
    }

    companion object {
        private const val TAG = "CARTDECK_PS201"
    }
}
