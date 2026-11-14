package com.swordfish.lemuroid.app.debug

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.io.File

/**
 * Private WIIU-02 Vulkan frame and Libretro probe only. Not a supported Wii U
 * cartridge or claim of gameplay acceptance. Its native host is compiled and
 * packaged separately from CARTDECK's existing GLES/LibretroDroid engines.
 */
class WiiuVulkanRuntimeProbeActivity : ComponentActivity(), SurfaceHolder.Callback {
    private lateinit var display: SurfaceView
    private lateinit var status: TextView
    private var loaded = false
    private var running = false
    private var stopping = false

    private external fun nativeRun(
        surface: Surface,
        sandbox: String,
        game: String,
        maxFrames: Int,
    ): String

    private external fun nativeStop()

    private external fun nativeButton(
        retroId: Int,
        down: Boolean,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loaded =
            runCatching {
                System.loadLibrary("cartdeckwiiuvulkanhost")
            }.isSuccess
        display = SurfaceView(this)
        status =
            TextView(this).apply {
                text = if (loaded) "Wii U Vulkan lab: waiting for lawful private fixture" else "WIIU_BLOCKED_NATIVE_HOST"
                setPadding(16, 16, 16, 16)
            }
        setContentView(
            FrameLayout(this).apply {
                addView(display)
                addView(status)
            },
        )
        display.holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) = Unit

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int,
    ) {
        if (!loaded || running || stopping || width <= 0 || height <= 0 || !holder.surface.isValid) return
        val root = File(filesDir, "wiiu-lab")
        val game = File(root, "fixture.wua")
        if (!game.isFile) {
            report("WIIU_BLOCKED_PRIVATE_FIXTURE: files/wiiu-lab/fixture.wua")
            return
        }
        running = true
        report("WIIU_VULKAN_START_REQUESTED: real game frames not yet established")
        Thread(
            {
                val result =
                    runCatching { nativeRun(holder.surface, root.absolutePath, game.absolutePath, 36000) }
                        .getOrElse { error -> "WIIU_HOST_FAILED: ${error.javaClass.simpleName}" }
                report(result)
                running = false
            },
            "cartdeck-wiiu-vulkan-lab",
        ).start()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        if (loaded) runCatching { nativeStop() }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val id =
            when (event.keyCode) {
                KeyEvent.KEYCODE_BUTTON_A -> 0 // RetroPad South/B
                KeyEvent.KEYCODE_BUTTON_B -> 8 // RetroPad East/A
                KeyEvent.KEYCODE_BUTTON_X -> 1 // RetroPad North/Y
                KeyEvent.KEYCODE_BUTTON_Y -> 9 // RetroPad West/X
                KeyEvent.KEYCODE_DPAD_UP -> 4
                KeyEvent.KEYCODE_DPAD_DOWN -> 5
                KeyEvent.KEYCODE_DPAD_LEFT -> 6
                KeyEvent.KEYCODE_DPAD_RIGHT -> 7
                KeyEvent.KEYCODE_BUTTON_START -> 3
                KeyEvent.KEYCODE_BUTTON_SELECT -> 2
                KeyEvent.KEYCODE_BUTTON_L1 -> 10
                KeyEvent.KEYCODE_BUTTON_R1 -> 11
                KeyEvent.KEYCODE_BUTTON_L2 -> 12
                KeyEvent.KEYCODE_BUTTON_R2 -> 13
                else -> -1
            }
        if (loaded && running && id >= 0 &&
            event.action in setOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)
        ) {
            runCatching { nativeButton(id, event.action == KeyEvent.ACTION_DOWN) }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        stopping = true
        if (loaded) runCatching { nativeStop() }
        super.onDestroy()
    }

    private fun report(message: String) {
        Log.i("CARTDECK_WIIU_LAB", message)
        runOnUiThread {
            if (!isDestroyed && !isFinishing) status.text = message
        }
    }
}
