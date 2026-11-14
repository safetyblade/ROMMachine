package com.swordfish.lemuroid.app.debug

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.swordfish.lemuroid.app.shared.game.ps2next.Armsx2PadMapping
import com.swordfish.lemuroid.app.shared.game.ps2next.Armsx2Storage
import kr.co.iefriends.pcsx2.NativeApp
import java.io.File

/**
 * Unlisted, debug-only ARMSX2 JNI integration probe. Never launched by the public game library.
 *
 * Test fixtures, BIOS and source-licensed emulator resources must be staged in this app's
 * own private files directory. An absent core or any missing input results in a blocked screen.
 * Process isolation is enforced in the debug AndroidManifest.
 */
class Armsx2RuntimeProbeActivity : ComponentActivity(), SurfaceHolder.Callback {
    private lateinit var status: TextView
    private lateinit var surface: SurfaceView

    @Volatile
    private var nativeReady = false

    @Volatile
    private var stopping = false

    private var surfacePublished = false
    private var vmStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        surface = SurfaceView(this)
        status = TextView(this).apply {
            text = "PS2-NEXT JNI probe: checking private test prerequisites"
            setPadding(20, 20, 20, 20)
        }
        setContentView(
            FrameLayout(this).apply {
                addView(surface)
                addView(status)
            },
        )
        surface.holder.addCallback(this)

        // Fixed test UUID ensures isolation from real shelf cartridges.
        val sandbox = Armsx2Storage.paths(filesDir, TEST_CARTRIDGE_ID)
        val disc = File(filesDir, "ps2-next-lab/fixture.iso")
        val missing = Armsx2Storage.missingRequirements(sandbox, disc)
        if (missing.isNotEmpty()) {
            report("PS2_NEXT_BLOCKED: ${missing.joinToString(",")}")
            return
        }

        Thread(
            {
                if (!NativeApp.tryLoad()) {
                    report("PS2_NEXT_BLOCKED: ${NativeApp.loadStatus()}")
                    return@Thread
                }
                runCatching {
                    Armsx2Storage.createPrivateDirectories(sandbox)
                    NativeApp.allowPrivateRoot(sandbox.globalRoot)
                    NativeApp.initialize(sandbox.gameRoot.absolutePath, sandbox.bios.absolutePath, Build.VERSION.SDK_INT)
                }.onFailure {
                    report("PS2_NEXT_INIT_FAILED: ${it.javaClass.simpleName}")
                }.onSuccess {
                    nativeReady = true
                    runOnUiThread {
                        if (!stopping) {
                            if (surface.holder.surface.isValid) {
                                publishSurface(surface.holder, surface.width, surface.height)
                            }
                            startVm(disc)
                        }
                    }
                }
            },
            "cartdeck-ps2-next-init",
        ).start()
    }

    private fun startVm(disc: File) {
        if (vmStarted || stopping || !nativeReady || !surfacePublished) return
        if (!disc.isFile) {
            report("PS2_NEXT_BLOCKED: DISC")
            return
        }
        vmStarted = true
        report("PS2_NEXT_VM_START_REQUESTED (not a gameplay pass)")
        Thread(
            {
                val result = runCatching { NativeApp.runVMThread(disc.absolutePath) }
                report(
                    if (result.isSuccess && result.getOrDefault(false)) {
                        "PS2_NEXT_VM_RETURNED (gameplay not verified)"
                    } else {
                        "PS2_NEXT_VM_FAILED: ${result.exceptionOrNull()?.javaClass?.simpleName ?: "FAILED"}"
                    },
                )
            },
            "cartdeck-ps2-next-vm",
        ).start()
    }

    private fun publishSurface(
        holder: SurfaceHolder,
        width: Int,
        height: Int,
    ) {
        if (!nativeReady || stopping || !holder.surface.isValid || width <= 0 || height <= 0) return
        runCatching { NativeApp.onNativeSurfaceChanged(holder.surface, width, height) }
            .onSuccess { surfacePublished = true }
            .onFailure {
                surfacePublished = false
                report("PS2_NEXT_SURFACE_FAILED: ${it.javaClass.simpleName}")
            }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (nativeReady) {
            runCatching { NativeApp.onNativeSurfaceCreated() }
        }
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int,
    ) {
        publishSurface(holder, width, height)
        gameImageForProbe()?.let(::startVm)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfacePublished = false
        if (nativeReady && !stopping) runCatching { NativeApp.onNativeSurfaceDestroyed() }
    }

    private fun gameImageForProbe(): File? =
        File(filesDir, "ps2-next-lab/fixture.iso").takeIf { it.isFile }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) return super.dispatchKeyEvent(event)
        val input = Armsx2PadMapping.button(event.keyCode, event.action == KeyEvent.ACTION_DOWN)
        if (nativeReady && vmStarted && input != null &&
            event.action in setOf(KeyEvent.ACTION_UP, KeyEvent.ACTION_DOWN)
        ) {
            runCatching { NativeApp.setPadButton(input.nativeCode, input.range, input.pressed) }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (!nativeReady || !vmStarted ||
            (event.source and InputDevice.SOURCE_JOYSTICK) != InputDevice.SOURCE_JOYSTICK
        ) {
            return super.onGenericMotionEvent(event)
        }
        val inputs =
            Armsx2PadMapping.stick(
                event.getAxisValue(MotionEvent.AXIS_X),
                event.getAxisValue(MotionEvent.AXIS_Y),
                right = false,
            ) +
                Armsx2PadMapping.stick(
                    event.getAxisValue(MotionEvent.AXIS_Z),
                    event.getAxisValue(MotionEvent.AXIS_RZ),
                    right = true,
                )
        runCatching {
            inputs.forEach { NativeApp.setPadButton(it.nativeCode, it.range, it.pressed) }
        }
        return true
    }

    override fun onPause() {
        super.onPause()
        if (nativeReady && vmStarted) runCatching { NativeApp.pause() }
    }

    override fun onResume() {
        super.onResume()
        if (nativeReady && vmStarted) runCatching { NativeApp.resume() }
    }

    override fun onDestroy() {
        stopping = true
        if (nativeReady) {
            Thread({ runCatching { NativeApp.shutdown() } }, "cartdeck-ps2-next-shutdown").start()
        }
        super.onDestroy()
    }

    private fun report(message: String) {
        Log.i(TAG, message)
        runOnUiThread {
            if (!isDestroyed && !isFinishing) status.text = message
        }
    }

    companion object {
        private const val TAG = "CARTDECK_PS2_NEXT"
        private const val TEST_CARTRIDGE_ID = "c3882b50-8b41-47cf-8219-f00b70c79e20"
    }
}
